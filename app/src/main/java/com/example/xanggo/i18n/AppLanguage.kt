package com.example.xanggo.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

enum class AppLang(val code: String, val label: String) {
    VI("vi", "🇻🇳 VI"),
    EN("en", "🇬🇧 EN");

    companion object {
        fun fromCode(code: String?): AppLang =
            if (code?.lowercase() == "en") EN else VI
    }
}

val LocalAppLanguage = compositionLocalOf { AppLang.VI }

object LanguagePrefs {
    private const val PREFS_NAME = "xanggo_prefs"
    private const val KEY_LANG = "app_lang"

    fun getSavedLang(context: Context): AppLang {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, null)
        if (saved != null) return AppLang.fromCode(saved)
        val deviceLang = Locale.getDefault().language
        return if (deviceLang.equals("en", ignoreCase = true)) AppLang.EN else AppLang.VI
    }

    fun saveLang(context: Context, lang: AppLang) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, lang.code)
            .apply()
    }
}

@Composable
fun tr(vi: String, en: String): String {
    return if (LocalAppLanguage.current == AppLang.EN) en else vi
}

fun translateFuelName(name: String, lang: AppLang): String {
    if (lang == AppLang.VI) return name
    return name
        .replace("Xăng RON 95-III", "Gasoline RON 95-III")
        .replace("Xăng RON 95-V", "Gasoline RON 95-V")
        .replace("Xăng E5 RON 92-II", "Bio-Gasoline E5 RON 92")
        .replace("Xăng E5 RON 92", "Bio-Gasoline E5 RON 92")
        .replace("Dầu DO 0,05S-II", "Diesel DO 0.05S-II")
        .replace("Dầu DO 0,001S-V", "Diesel DO 0.001S-V")
        .replace("Dầu Diesel 0.05S", "Diesel DO 0.05S")
        .replace("Dầu hỏa 2-K", "Kerosene 2-K")
        .replace("Dầu hỏa", "Kerosene")
}

fun translateCountryName(name: String, code: String, lang: AppLang): String {
    if (lang == AppLang.VI) return name
    return when (code.uppercase()) {
        "VN" -> "Vietnam"
        "SG" -> "Singapore"
        "TH" -> "Thailand"
        "MY" -> "Malaysia"
        "ID" -> "Indonesia"
        "PH" -> "Philippines"
        "LA" -> "Laos"
        "KH" -> "Cambodia"
        "MM" -> "Myanmar"
        "BN" -> "Brunei"
        "US" -> "United States"
        "CN" -> "China"
        "JP" -> "Japan"
        "KR" -> "South Korea"
        "HK" -> "Hong Kong"
        "DE" -> "Germany"
        "FR" -> "France"
        "GB", "UK" -> "United Kingdom"
        "AU" -> "Australia"
        "RU" -> "Russia"
        "IR" -> "Iran"
        "VE" -> "Venezuela"
        "KW" -> "Kuwait"
        "SA" -> "Saudi Arabia"
        "AE" -> "UAE"
        "IN" -> "India"
        "TW" -> "Taiwan"
        "CA" -> "Canada"
        "NO" -> "Norway"
        "CH" -> "Switzerland"
        else -> name
    }
}
