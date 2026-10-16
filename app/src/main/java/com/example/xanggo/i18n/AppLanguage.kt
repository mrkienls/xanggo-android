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

fun translateFuelName(name: String, lang: AppLang, compact: Boolean = false): String {
    val cleanVi = name
        .replace("Xăng E10 RON 95-III", "Xăng RON 95-III")
        .replace("Xăng E10 RON 95-V", "Xăng RON 95-V")
        .replace("Dầu DO 0,05S-II", "Dầu DO 0.05S-II")
        .replace("Dầu DO 0,001S-V", "Dầu DO 0.001S-V")

    if (compact) {
        return if (lang == AppLang.VI) {
            cleanVi
                .replace("Xăng E5 RON 92-II", "E5 RON 92-II")
                .replace("Xăng E5 RON 92", "E5 RON 92")
                .replace("Xăng RON 95-III", "RON 95-III")
                .replace("Xăng RON 95-V", "RON 95-V")
                .replace("Dầu Diesel 0.05S", "Dầu DO 0.05S")
        } else {
            cleanVi
                .replace("Xăng E5 RON 92-II", "E5 RON 92-II")
                .replace("Xăng E5 RON 92", "E5 RON 92")
                .replace("Xăng RON 95-III", "RON 95-III")
                .replace("Xăng RON 95-V", "RON 95-V")
                .replace("Dầu DO 0.05S-II", "Diesel 0.05S-II")
                .replace("Dầu DO 0.001S-V", "Diesel 0.001S-V")
                .replace("Dầu Diesel 0.05S", "Diesel 0.05S")
                .replace("Dầu hỏa 2-K", "Kerosene 2-K")
                .replace("Dầu hỏa", "Kerosene")
        }
    }

    if (lang == AppLang.VI) return cleanVi
    return cleanVi
        .replace("Xăng RON 95-III", "Gasoline RON 95-III")
        .replace("Xăng RON 95-V", "Gasoline RON 95-V")
        .replace("Xăng E5 RON 92-II", "Bio-Gas E5 RON 92-II")
        .replace("Xăng E5 RON 92", "Bio-Gas E5 RON 92")
        .replace("Dầu DO 0.05S-II", "Diesel DO 0.05S-II")
        .replace("Dầu DO 0.001S-V", "Diesel DO 0.001S-V")
        .replace("Dầu Diesel 0.05S", "Diesel DO 0.05S")
        .replace("Dầu hỏa 2-K", "Kerosene 2-K")
        .replace("Dầu hỏa", "Kerosene")
}

fun translateCountryName(name: String, code: String, lang: AppLang): String {
    if (lang == AppLang.VI) return name
    return when (code.uppercase()) {
        "VN", "VIE" -> "Vietnam"
        "SG", "SIN" -> "Singapore"
        "TH", "THA" -> "Thailand"
        "MY", "MAL" -> "Malaysia"
        "ID", "IND" -> if (name.contains("Ấn Độ")) "India" else "Indonesia"
        "PH", "PHI" -> "Philippines"
        "LA", "LAO" -> "Laos"
        "KH", "CAM" -> "Cambodia"
        "MM", "BUR" -> "Myanmar"
        "BN", "BRU" -> "Brunei"
        "US", "USA" -> "United States"
        "CN", "CHI" -> "China"
        "JP", "JAP" -> "Japan"
        "KR", "SOU" -> if (name.contains("Nam Phi")) "South Africa" else "South Korea"
        "HK", "HON" -> if (name.contains("Honduras")) "Honduras" else "Hong Kong"
        "DE", "GER" -> "Germany"
        "FR", "FRA" -> "France"
        "GB", "UK" -> "United Kingdom"
        "AU", "AUS" -> "Australia"
        "RU", "RUS" -> "Russia"
        "IR", "IRA" -> if (name.contains("Iraq")) "Iraq" else "Iran"
        "VE", "VEN" -> "Venezuela"
        "KW", "KUW" -> "Kuwait"
        "SA", "SAU" -> "Saudi Arabia"
        "AE", "UAE" -> "UAE"
        "IN" -> "India"
        "TW", "TAI" -> "Taiwan"
        "CA", "CAN" -> "Canada"
        "NO", "NOR" -> "Norway"
        "CH", "SWI" -> "Switzerland"
        else -> name
    }
}

fun translateCountryNote(note: String, lang: AppLang): String {
    if (lang == AppLang.VI || note.isBlank()) return note
    return when {
        note.contains("Kỳ điều hành hôm nay") -> "Current official period"
        note.contains("Kỳ điều hành") -> note.replace("Kỳ điều hành ngày", "Official period:")
        note.contains("Trợ cấp") || note.contains("Trợ giá") || note.contains("trợ giá") -> "Gov. fuel subsidy"
        note.contains("Thuế môi trường cao") || note.contains("Thuế cao") -> "High fuel & eco tax"
        note.contains("Nhập khẩu 100%") -> "100% imported fuel"
        note.contains("Đắt nhất thế giới") -> "Highest in the world"
        else -> note
    }
}
