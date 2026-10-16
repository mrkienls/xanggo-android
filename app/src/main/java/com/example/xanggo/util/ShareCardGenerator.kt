package com.example.xanggo.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.data.model.WorldFuelSummary
import com.example.xanggo.i18n.AppLang
import com.example.xanggo.i18n.translateCountryName
import com.example.xanggo.i18n.translateFuelName
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class ShareCardPreviewData(
    val bitmap: Bitmap,
    val fileName: String,
    val caption: String
)

object ShareCardGenerator {

    private val currencyFormat = NumberFormat.getNumberInstance(Locale.GERMANY)

    /**
     * Vẽ tấm ảnh Bảng Giá Xăng + Chi phí đổ xăng xe (1080x1320 PNG) để Xem trước hoặc Chia sẻ
     */
    fun createVietnamFuelCard(
        data: VietnamFuelData,
        zone: Int,
        provinceLabel: String,
        vehicleName: String,
        tankLiters: Double,
        selectedFuelName: String,
        fullTankCost: Int,
        fullTankDelta: Int,
        lang: AppLang
    ): ShareCardPreviewData {
        val width = 1080
        val height = 1320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Nền Gradient tối hiện đại
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#0B1120"),
                    Color.parseColor("#0F172A"),
                    Color.parseColor("#091526")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Vòng tròn phát sáng trang trí
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                920f, 180f, 380f,
                Color.argb(45, 20, 184, 166),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(920f, 180f, 380f, glowPaint)

        // 2. Header: Logo Giá Xăng 24h + Ngày cập nhật
        val logoBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
        }
        canvas.drawRoundRect(RectF(60f, 60f, 144f, 144f), 22f, 22f, logoBoxPaint)

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 46f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⛽", 102f, 118f, emojiPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Giá Xăng ", 166f, 118f, titlePaint)
        val xangWidth = titlePaint.measureText("Giá Xăng ")
        val goPaint = Paint(titlePaint).apply {
            color = Color.parseColor("#14B8A6")
        }
        canvas.drawText("24h", 166f + xangWidth, 118f, goPaint)

        // Badge Vùng ở góc phải
        val zonePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val zoneBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14B8A6")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val zoneRect = RectF(600f, 68f, 1020f, 136f)
        canvas.drawRoundRect(zoneRect, 34f, 34f, zonePillPaint)
        canvas.drawRoundRect(zoneRect, 34f, 34f, zoneBorderPaint)

        val zoneTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14B8A6")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val shortProvince = if (provinceLabel.length > 22) provinceLabel.take(20) + "…" else provinceLabel
        canvas.drawText("📍 $shortProvince", zoneRect.centerX(), 111f, zoneTextPaint)

        // Subtitle ngày cập nhật
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
        }
        val headerSub = if (lang == AppLang.EN) {
            "🕒 Updated: ${formatDateShort(data.updatedAt)}  •  Source: Petrolimex"
        } else {
            "🕒 Kỳ điều hành: ${formatDateShort(data.updatedAt)}  •  Nguồn: Petrolimex"
        }
        canvas.drawText(headerSub, 60f, 195f, subPaint)

        // 3. HERO CARD: Xăng RON 95-III
        val heroItem = data.items.firstOrNull { it.isFeatured } ?: data.items.first()
        val heroPrice = heroItem.getPrice(zone)
        val heroRect = RectF(60f, 230f, 1020f, 520f)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
        }
        val heroBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14B8A6")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 140
        }
        canvas.drawRoundRect(heroRect, 36f, 36f, cardBgPaint)
        canvas.drawRoundRect(heroRect, 36f, 36f, heroBorderPaint)

        val heroNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("⛽ ${translateFuelName(heroItem.name, lang)}", 105f, 300f, heroNamePaint)

        val bigPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 86f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val priceStr = currencyFormat.format(heroPrice)
        canvas.drawText(priceStr, 105f, 405f, bigPricePaint)

        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 36f
        }
        val unitText = if (lang == AppLang.EN) " VND/liter" else " đ/lít"
        canvas.drawText(unitText, 105f + bigPricePaint.measureText(priceStr), 400f, unitPaint)

        // Badge tăng/giảm của Hero
        drawDeltaPill(
            canvas = canvas,
            x = 105f,
            y = 435f,
            delta = heroItem.delta,
            lang = lang
        )

        // 4. DANH SÁCH MẶT HÀNG KHÁC (E5 RON 92, Dầu DO...)
        val otherItems = data.items.filter { it.id != heroItem.id }.take(2)
        val boxTop = 555f
        val boxBottom = 775f
        otherItems.forEachIndexed { idx, item ->
            val left = if (idx == 0) 60f else 555f
            val right = if (idx == 0) 525f else 1020f
            val r = RectF(left, boxTop, right, boxBottom)
            canvas.drawRoundRect(r, 30f, 30f, cardBgPaint)

            val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val displayName = translateFuelName(item.name, lang)
            val shortName = if (displayName.length > 22) displayName.take(20) + "…" else displayName
            canvas.drawText(shortName, left + 34f, boxTop + 60f, itemTitlePaint)

            val itemPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 48f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val pText = "${currencyFormat.format(item.getPrice(zone))} đ"
            canvas.drawText(pText, left + 34f, boxTop + 125f, itemPricePaint)

            drawDeltaPill(
                canvas = canvas,
                x = left + 34f,
                y = boxTop + 148f,
                delta = item.delta,
                lang = lang,
                compact = true
            )
        }

        // 5. BOX VIRAL TÂM ĐIỂM: ĐỔ ĐẦY BÌNH XE CỦA BẠN HẾT BAO NHIÊU?
        val tankRect = RectF(60f, 815f, 1020f, 1135f)
        val tankBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                tankRect.left, tankRect.top, tankRect.right, tankRect.bottom,
                intArrayOf(Color.parseColor("#0F292E"), Color.parseColor("#132A3E")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val tankStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 180
        }
        canvas.drawRoundRect(tankRect, 36f, 36f, tankBgPaint)
        canvas.drawRoundRect(tankRect, 36f, 36f, tankStrokePaint)

        val tankHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBBF24")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val litersText = String.format(Locale.US, "%.1f", tankLiters).replace(".0", "")
        val tankTitle = if (lang == AppLang.EN) {
            "🔥 FULL TANK COST TODAY ($vehicleName • ${litersText}L)"
        } else {
            "🔥 ĐỔ ĐẦY BÌNH HÔM NAY ($vehicleName • ${litersText}L)"
        }
        canvas.drawText(tankTitle, 100f, 880f, tankHeaderPaint)

        val tankSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 25f
        }
        val fuelLabel = translateFuelName(selectedFuelName, lang)
        canvas.drawText(
            if (lang == AppLang.EN) "Fuel: $fuelLabel" else "Loại nhiên liệu: $fuelLabel",
            100f, 922f, tankSubPaint
        )

        val tankCostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val costText = "${currencyFormat.format(fullTankCost)} đ"
        canvas.drawText(costText, 100f, 1012f, tankCostPaint)

        // Chênh lệch cả bình
        val deltaTankColor = when {
            fullTankDelta < 0 -> Color.parseColor("#10B981")
            fullTankDelta > 0 -> Color.parseColor("#EF4444")
            else -> Color.parseColor("#94A3B8")
        }
        val deltaTankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deltaTankColor
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val deltaTankStr = when {
            fullTankDelta < 0 -> if (lang == AppLang.EN) {
                "▼ Save ${currencyFormat.format(abs(fullTankDelta))} VND / tank vs last period"
            } else {
                "▼ Tiết kiệm ${currencyFormat.format(abs(fullTankDelta))} đ / bình so với kỳ trước"
            }
            fullTankDelta > 0 -> if (lang == AppLang.EN) {
                "▲ +${currencyFormat.format(fullTankDelta)} VND / tank vs last period"
            } else {
                "▲ Đắt thêm +${currencyFormat.format(fullTankDelta)} đ / bình so với kỳ trước"
            }
            else -> if (lang == AppLang.EN) "Unchanged vs last period" else "Giữ nguyên so với kỳ trước"
        }
        canvas.drawText(deltaTankStr, 100f, 1082f, deltaTankPaint)

        // 6. FOOTER WATERMARK GIÁ XĂNG 24H
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14B8A6")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val footerText = if (lang == AppLang.EN) {
            "📲 Download Giá Xăng 24h on Google Play • Live Fuel Prices & Gas Stations"
        } else {
            "📲 Tải app Giá Xăng 24h trên Google Play • Giá Xăng & Cây Xăng Gần Nhất"
        }
        canvas.drawText(footerText, width / 2f, 1235f, footerPaint)

        val shareCaption = if (lang == AppLang.EN) {
            "⛽ Vietnam Fuel Price Today (${formatDateShort(data.updatedAt)}): ${translateFuelName(heroItem.name, lang)} is ${currencyFormat.format(heroPrice)} VND/L. Full tank for $vehicleName (${litersText}L) costs ${currencyFormat.format(fullTankCost)} VND! Check live prices & nearby gas stations with Giá Xăng 24h: https://play.google.com/store/apps/details?id=com.mrkienls.xanggo"
        } else {
            "⛽ Giá xăng dầu hôm nay (${formatDateShort(data.updatedAt)}): ${heroItem.name} ở mức ${currencyFormat.format(heroPrice)} đ/lít. Đổ đầy bình $vehicleName (${litersText}L) hết ${currencyFormat.format(fullTankCost)} đ! Tra cứu giá xăng & tìm cây xăng gần nhất trên Giá Xăng 24h: https://play.google.com/store/apps/details?id=com.mrkienls.xanggo"
        }

        return ShareCardPreviewData(bitmap, "gia_xang_24h.png", shareCaption)
    }

    fun shareVietnamFuelCard(
        context: Context,
        data: VietnamFuelData,
        zone: Int,
        provinceLabel: String,
        vehicleName: String,
        tankLiters: Double,
        selectedFuelName: String,
        fullTankCost: Int,
        fullTankDelta: Int,
        lang: AppLang
    ) {
        val card = createVietnamFuelCard(
            data, zone, provinceLabel, vehicleName, tankLiters,
            selectedFuelName, fullTankCost, fullTankDelta, lang
        )
        shareRenderedCard(context, card)
    }

    /**
     * Vẽ ảnh Infographic So sánh Giá Xăng Đông Nam Á & Thế giới (1080x1320 PNG)
     */
    fun createWorldCompareCard(
        data: WorldFuelSummary,
        lang: AppLang
    ): ShareCardPreviewData {
        val width = 1080
        val height = 1320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Nền tối
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#0B1120"),
                    Color.parseColor("#0F172A"),
                    Color.parseColor("#091526")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitle = if (lang == AppLang.EN) {
            "🌍 ASEAN Fuel Price & Affordability"
        } else {
            "🌍 Giá Xăng & Sức Mua Đông Nam Á"
        }
        canvas.drawText(headerTitle, 60f, 110f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
        }
        val subTitle = if (lang == AppLang.EN) {
            "RON 95 Gasoline • % of daily income needed for 1 Liter • Giá Xăng 24h"
        } else {
            "Xăng RON 95 • Mua 1 lít xăng tốn bao nhiêu % thu nhập 1 ngày • Giá Xăng 24h"
        }
        canvas.drawText(subTitle, 60f, 160f, subPaint)

        val asean = data.countries
            .filter { it.region == "ASEAN" }
            .sortedBy { it.priceVnd }
            .take(8)

        val maxPrice = (asean.maxOfOrNull { it.priceVnd } ?: 65000).coerceAtLeast(1)

        var y = 215f
        val rowHeight = 112f

        asean.forEachIndexed { idx, country ->
            val isVn = country.isCurrent || country.code == "VN"
            val rowRect = RectF(60f, y, 1020f, y + 96f)

            val rowBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isVn) Color.parseColor("#13383A") else Color.parseColor("#1E293B")
            }
            canvas.drawRoundRect(rowRect, 22f, 22f, rowBg)

            if (isVn) {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#14B8A6")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRoundRect(rowRect, 22f, 22f, borderPaint)
            }

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isVn) Color.parseColor("#2DD4BF") else Color.WHITE
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cName = translateCountryName(country.name, country.code, lang)
            canvas.drawText("#${idx + 1}  ${country.flag} $cName", 90f, y + 44f, namePaint)

            val workMins = if (country.dailyIncomeUsd > 0) {
                ((country.priceUsd / country.dailyIncomeUsd) * 480).roundToInt()
            } else 0
            val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 22f
            }
            val detailStr = if (lang == AppLang.EN) {
                "1L = ${country.affordabilityPercent}% daily income (~${workMins}m work)"
            } else {
                "1L = ${country.affordabilityPercent}% thu nhập ngày (~${workMins} phút làm việc)"
            }
            canvas.drawText(detailStr, 90f, y + 78f, detailPaint)

            val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isVn) Color.parseColor("#2DD4BF") else Color.WHITE
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val unitLabel = if (lang == AppLang.EN) "VND/L" else "đ/lít"
            canvas.drawText("${currencyFormat.format(country.priceVnd)} $unitLabel", 990f, y + 58f, pricePaint)

            val barWidth = ((country.priceVnd.toFloat() / maxPrice) * 320f).coerceIn(30f, 320f)
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isVn) Color.parseColor("#14B8A6") else Color.parseColor("#475569")
            }
            canvas.drawRoundRect(
                RectF(990f - barWidth, y + 70f, 990f, y + 80f),
                5f, 5f, barPaint
            )

            y += rowHeight
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14B8A6")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val footerText = if (lang == AppLang.EN) {
            "📲 Giá Xăng 24h on Google Play • Vietnam & World Fuel Price Tracker"
        } else {
            "📲 Tải Giá Xăng 24h trên Google Play • Tra cứu giá xăng Việt Nam & Thế giới"
        }
        canvas.drawText(footerText, width / 2f, 1245f, footerPaint)

        val shareCaption = if (lang == AppLang.EN) {
            "🌍 ASEAN Fuel Price & Affordability Ranking! How much of daily income does 1 liter of gas cost in Vietnam vs Singapore, Thailand, Malaysia? Check live on Giá Xăng 24h: https://play.google.com/store/apps/details?id=com.mrkienls.xanggo"
        } else {
            "🌍 Bảng xếp hạng giá xăng & sức mua Đông Nam Á! Đổ 1 lít xăng ở Việt Nam tốn bao nhiêu phút làm việc so với Singapore, Thái Lan, Malaysia? Xem chi tiết trên Giá Xăng 24h: https://play.google.com/store/apps/details?id=com.mrkienls.xanggo"
        }

        return ShareCardPreviewData(bitmap, "gia_xang_24h_asean.png", shareCaption)
    }

    fun shareWorldCompareCard(
        context: Context,
        data: WorldFuelSummary,
        lang: AppLang
    ) {
        val card = createWorldCompareCard(data, lang)
        shareRenderedCard(context, card)
    }

    fun shareRenderedCard(context: Context, card: ShareCardPreviewData) {
        shareBitmap(context, card.bitmap, card.fileName, card.caption)
    }

    private fun drawDeltaPill(
        canvas: Canvas,
        x: Float,
        y: Float,
        delta: Int,
        lang: AppLang,
        compact: Boolean = false
    ) {
        val bgColor = when {
            delta < 0 -> Color.argb(45, 16, 185, 129)
            delta > 0 -> Color.argb(45, 239, 68, 68)
            else -> Color.argb(40, 148, 163, 184)
        }
        val textColor = when {
            delta < 0 -> Color.parseColor("#10B981")
            delta > 0 -> Color.parseColor("#EF4444")
            else -> Color.parseColor("#94A3B8")
        }
        val text = when {
            delta < 0 -> if (compact) "▼ ${-delta}đ" else if (lang == AppLang.EN) "▼ Down ${-delta} VND/L" else "▼ Giảm ${-delta} đ/lít"
            delta > 0 -> if (compact) "▲ +${delta}đ" else if (lang == AppLang.EN) "▲ Up +${delta} VND/L" else "▲ Tăng +${delta} đ/lít"
            else -> if (lang == AppLang.EN) "Unchanged" else "Không đổi"
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = if (compact) 24f else 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(text)
        val padH = if (compact) 20f else 26f
        val pillHeight = if (compact) 44f else 54f
        val rect = RectF(x, y, x + textWidth + padH * 2, y + pillHeight)

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
        }
        canvas.drawRoundRect(rect, 22f, 22f, pillPaint)
        canvas.drawText(text, x + padH, y + pillHeight * 0.7f, textPaint)
    }

    private fun formatDateShort(raw: String): String {
        val m = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})""").find(raw) ?: return raw
        val (y, mo, d, h, mi) = m.destructured
        return "$d/$mo/$y $h:$mi"
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, caption: String) {
        try {
            val cacheDir = File(context.cacheDir, "shared_cards")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Giá Xăng 24h").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            context.startActivity(Intent.createChooser(textIntent, "Giá Xăng 24h"))
        }
    }
}
