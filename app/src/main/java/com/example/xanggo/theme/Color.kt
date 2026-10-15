package com.example.xanggo.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Quản lý trạng thái giao diện Tối AMOLED (Mặc định) & Sáng Ngoài Trời (Outdoor Sun Mode)
 */
object ThemeState {
    private const val PREFS_NAME = "xanggo_prefs"
    private const val KEY_LIGHT_MODE = "outdoor_light_mode"

    var isLightMode by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isLightMode = prefs.getBoolean(KEY_LIGHT_MODE, false)
    }

    fun toggle(context: Context) {
        val next = !isLightMode
        isLightMode = next
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIGHT_MODE, next)
            .apply()
    }
}

// Bộ màu nhận diện XăngGo (Petrol Teal & Energy Amber)
val PetrolTeal: Color
    get() = if (ThemeState.isLightMode) Color(0xFF0D9488) else Color(0xFF14B8A6)
val PetrolTealDark = Color(0xFF0D9488)
val EnergyAmber: Color
    get() = if (ThemeState.isLightMode) Color(0xFFD97706) else Color(0xFFF59E0B)
val EnergyAmberDark = Color(0xFFD97706)

// Light Theme Colors (Hỗ trợ khi đi xe ngoài trời nắng gắt)
val LightBackground = Color(0xFFF1F5F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFCBD5E1)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// Dynamic Theme Colors (Tự động chuyển đổi mượt mà giữa Dark AMOLED & Outdoor Light Mode)
val DarkBackground: Color
    get() = if (ThemeState.isLightMode) LightBackground else Color(0xFF0B0F17)
val DarkSurface: Color
    get() = if (ThemeState.isLightMode) LightSurface else Color(0xFF1E293B)
val DarkSurfaceVariant: Color
    get() = if (ThemeState.isLightMode) LightSurfaceVariant else Color(0xFF334155)
val TextPrimaryDark: Color
    get() = if (ThemeState.isLightMode) TextPrimaryLight else Color(0xFFF8FAFC)
val TextSecondaryDark: Color
    get() = if (ThemeState.isLightMode) TextSecondaryLight else Color(0xFF94A3B8)

// Quy chuẩn màu biến động giá (Tâm lý người tiêu dùng)
val PriceGreen: Color
    get() = if (ThemeState.isLightMode) Color(0xFF059669) else Color(0xFF10B981)
val PriceRed: Color
    get() = if (ThemeState.isLightMode) Color(0xFFDC2626) else Color(0xFFEF4444)

// Mã màu nhận diện từng loại xăng dầu
val FuelRon95V = Color(0xFFD97706)   // Vàng hổ phách Euro 5
val FuelRon95III = Color(0xFFF97316) // Cam tiêu chuẩn
val FuelE5 = Color(0xFF16A34A)       // Xanh lá sinh học
val FuelDiesel = Color(0xFF38BDF8)   // Xanh thép Diesel
val FuelKerosene = Color(0xFF0284C7) // Xanh dầu hỏa
