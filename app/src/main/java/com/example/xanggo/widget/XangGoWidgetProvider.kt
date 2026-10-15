package com.example.xanggo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.xanggo.MainActivity
import com.example.xanggo.R
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.i18n.AppLang
import com.example.xanggo.i18n.LanguagePrefs
import java.text.NumberFormat
import java.util.Locale

class XangGoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_NAME = "xanggo_prefs"

        fun saveAndRefreshWidget(context: Context, data: VietnamFuelData, zone: Int) {
            val ron95 = data.items.firstOrNull { it.id.contains("ron95") || it.isFeatured } ?: data.items.firstOrNull()
            val e5 = data.items.firstOrNull { it.id.contains("e5") } ?: data.items.getOrNull(1)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putInt("selected_zone", zone)
                if (ron95 != null) {
                    putInt("widget_ron95_price", ron95.getPrice(zone))
                    putInt("widget_ron95_delta", ron95.delta)
                }
                if (e5 != null) {
                    putInt("widget_e5_price", e5.getPrice(zone))
                    putInt("widget_e5_delta", e5.delta)
                }
                apply()
            }

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, XangGoWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateAppWidget(context, manager, id)
                }
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lang = LanguagePrefs.getSavedLang(context)
            val zone = prefs.getInt("selected_zone", 1)
            val ron95Price = prefs.getInt("widget_ron95_price", if (zone == 2) 29480 else 28900)
            val ron95Delta = prefs.getInt("widget_ron95_delta", 1210)
            val e5Price = prefs.getInt("widget_e5_price", if (zone == 2) 27770 else 27230)
            val e5Delta = prefs.getInt("widget_e5_delta", 860)

            val fmt = NumberFormat.getNumberInstance(Locale.GERMANY)
            val views = RemoteViews(context.packageName, R.layout.xanggo_widget)

            views.setTextViewText(
                R.id.widget_title,
                if (lang == AppLang.EN) "⛽ XăngGo • Fuel Price" else "⛽ XăngGo • Giá Xăng"
            )
            views.setTextViewText(
                R.id.widget_zone,
                if (lang == AppLang.EN) "Zone $zone" else "Vùng $zone"
            )

            views.setTextViewText(R.id.widget_ron95_price, "${fmt.format(ron95Price)} đ")
            views.setTextViewText(R.id.widget_ron95_delta, formatDelta(ron95Delta, lang))
            views.setTextColor(R.id.widget_ron95_delta, deltaColor(ron95Delta))

            views.setTextViewText(R.id.widget_e5_price, "${fmt.format(e5Price)} đ")
            views.setTextViewText(R.id.widget_e5_delta, formatDelta(e5Delta, lang))
            views.setTextColor(R.id.widget_e5_delta, deltaColor(e5Delta))

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatDelta(delta: Int, lang: AppLang): String = when {
            delta > 0 -> "▲ +${delta}đ"
            delta < 0 -> "▼ ${-delta}đ"
            else -> if (lang == AppLang.EN) "Unchanged" else "Giữ nguyên"
        }

        private fun deltaColor(delta: Int): Int = when {
            delta < 0 -> Color.parseColor("#10B981")
            delta > 0 -> Color.parseColor("#EF4444")
            else -> Color.parseColor("#94A3B8")
        }
    }
}
