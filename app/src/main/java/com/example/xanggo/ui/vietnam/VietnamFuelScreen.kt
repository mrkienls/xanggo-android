package com.example.xanggo.ui.vietnam

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xanggo.data.model.FuelItem
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.data.model.WorldFuelSummary
import com.example.xanggo.i18n.AppLang
import com.example.xanggo.i18n.LocalAppLanguage
import com.example.xanggo.i18n.tr
import com.example.xanggo.i18n.translateCountryName
import com.example.xanggo.i18n.translateFuelName
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.LocationPinVector
import com.example.xanggo.ui.components.PriceHistoryChartCard
import com.example.xanggo.ui.components.ShareCardPreviewDialog
import com.example.xanggo.util.ShareCardGenerator
import com.example.xanggo.util.ShareCardPreviewData
import com.example.xanggo.widget.XangGoWidgetProvider
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.roundToInt

/** Bảng màu nhận diện riêng biệt cho từng loại nhiên liệu (Cam RON 95 | Xanh lá E5 | Xanh dương DO 0.05S | Tím DO 0.001S-V) */
private fun getFuelNozzleAccentColor(item: FuelItem): Color {
    val key = (item.id + " " + item.name).lowercase()
    return when {
        key.contains("95-v") || key.contains("95v") -> Color(0xFFEF4444)
        key.contains("95") -> Color(0xFFF97316)
        key.contains("e5") || key.contains("92") -> Color(0xFF10B981)
        key.contains("001s") -> Color(0xFF8B5CF6)
        key.contains("do") || key.contains("diesel") -> Color(0xFF0EA5E9)
        else -> Color(0xFFF59E0B)
    }
}

/** Tên siêu gọn cho dải 3 cột ngang (Không bao giờ bị cắt chữ) */
private fun shortFuelStripTitle(rawName: String, lang: AppLang): String {
    val low = rawName.lowercase()
    return when {
        low.contains("e5") || low.contains("92") -> "E5 RON 92"
        low.contains("0.001s") || low.contains("0,001s") -> "DO 0.001S-V"
        low.contains("0.05s") || low.contains("0,05s") -> "DO 0.05S-II"
        low.contains("95-v") -> "RON 95-V"
        low.contains("95") -> "RON 95-III"
        low.contains("hỏa") -> if (lang == AppLang.EN) "Kerosene" else "Dầu hỏa"
        else -> translateFuelName(rawName, lang, compact = true)
    }
}

data class VehiclePreset(
    val id: String,
    val nameVi: String,
    val nameEn: String,
    val emoji: String,
    val tankLiters: Double,
    val defaultFuelKeyword: String = "95"
)

private val VEHICLE_PRESETS = listOf(
    VehiclePreset("vision", "Vision", "Vision", "🛵", 4.9, "95"),
    VehiclePreset("airblade", "AirBlade", "AirBlade", "🛵", 4.4, "95"),
    VehiclePreset("wave", "Wave", "Wave", "🛵", 3.7, "95"),
    VehiclePreset("lead", "Lead", "Lead", "🛵", 6.0, "95"),
    VehiclePreset("vario", "Vario", "Vario", "🛵", 5.5, "95"),
    VehiclePreset("sh", "SH", "SH", "🛵", 7.0, "95"),
    VehiclePreset("exciter", "Exciter", "Exciter", "🏍️", 5.0, "95"),
    VehiclePreset("sedan", "Ô tô 5 chỗ", "Sedan", "🚗", 45.0, "95"),
    VehiclePreset("suv", "SUV 7 chỗ", "SUV 7-seat", "🚙", 58.0, "95"),
    VehiclePreset("pickup", "Bán tải", "Pickup", "🛻", 76.0, "do")
)

private val MONEY_PRESETS = listOf(30_000, 50_000, 70_000, 100_000, 200_000, 500_000, 1_000_000)

@Composable
fun VietnamFuelScreen(
    data: VietnamFuelData,
    worldData: WorldFuelSummary? = null,
    onNavigateToStations: () -> Unit = {},
    onNavigateToWorld: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lang = LocalAppLanguage.current
    val prefs = remember { context.getSharedPreferences("xanggo_prefs", Context.MODE_PRIVATE) }
    var selectedZone by remember { mutableIntStateOf(prefs.getInt("selected_zone", 1)) }
    var selectedProvince by remember { mutableStateOf(prefs.getString("selected_province", "") ?: "") }
    var selectedVehicleIdx by remember { mutableIntStateOf(prefs.getInt("selected_vehicle_idx", 0).coerceIn(0, VEHICLE_PRESETS.lastIndex)) }
    val currentVehicle = VEHICLE_PRESETS.getOrElse(selectedVehicleIdx) { VEHICLE_PRESETS.first() }

    var customLiters by remember(selectedVehicleIdx) { mutableDoubleStateOf(currentVehicle.tankLiters) }
    var selectedFuelKeyword by remember(selectedVehicleIdx) { mutableStateOf(currentVehicle.defaultFuelKeyword) }

    var showZoneDialog by remember { mutableStateOf(false) }
    var previewCard by remember { mutableStateOf<ShareCardPreviewData?>(null) }
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    LaunchedEffect(data, selectedZone) {
        XangGoWidgetProvider.saveAndRefreshWidget(context, data, selectedZone)
    }

    val vehicleFuelItem = remember(data.items, selectedFuelKeyword) {
        data.items.firstOrNull {
            it.id.lowercase().contains(selectedFuelKeyword) ||
                it.name.lowercase().contains(selectedFuelKeyword)
        } ?: data.items.firstOrNull { it.isFeatured } ?: data.items.first()
    }
    val fullTankCost = (vehicleFuelItem.getPrice(selectedZone) * customLiters).roundToInt()
    val fullTankDelta = (vehicleFuelItem.delta * customLiters).roundToInt()

    val shareZoneLabel = when {
        selectedProvince.isNotEmpty() && !selectedProvince.startsWith("Vùng ") -> selectedProvince
        selectedZone == 1 -> tr("Vùng 1 (Chuẩn)", "Zone 1 (Std)")
        else -> tr("Vùng 2 (+2%)", "Zone 2 (+2%)")
    }

    val headerZoneLabel = when {
        selectedProvince.isNotEmpty() && !selectedProvince.startsWith("Vùng ") -> {
            selectedProvince
                .replace("TP. Hồ Chí Minh", "TP.HCM")
                .replace("TP. ", "")
                .replace(" (Z1)", " · V1")
                .replace(" (Z2)", " · V2")
        }
        selectedZone == 1 -> tr("Vùng 1", "Zone 1")
        else -> tr("Vùng 2 (+2%)", "Zone 2 (+2%)")
    }

    val openSharePreview = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        previewCard = ShareCardGenerator.createVietnamFuelCard(
            data = data,
            zone = selectedZone,
            provinceLabel = shareZoneLabel,
            vehicleName = if (lang == AppLang.EN) currentVehicle.nameEn else currentVehicle.nameVi,
            tankLiters = customLiters,
            selectedFuelName = vehicleFuelItem.name,
            fullTankCost = fullTankCost,
            fullTankDelta = fullTankDelta,
            lang = lang
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. HEADER THƯƠNG HIỆU 2 TÔNG MÀU (Giá Xăng 24h + Tagline gọn + Cụm nút Glassmorphic)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))),
                        contentAlignment = Alignment.Center
                    ) {
                        FuelPumpVectorIcon(
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Giá Xăng ",
                                color = TextPrimaryDark,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "24h",
                                color = Color(0xFF10B981),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = tr("Cập nhật giá xăng dầu • Nhanh, chính xác", "Live Vietnam fuel prices • Fast & accurate"),
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        color = DarkSurface,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                ThemeState.toggle(context)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (ThemeState.isLightMode) "☀️" else "🌙",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.clickable { onToggleLanguage() }
                    ) {
                        Text(
                            text = if (lang == AppLang.EN) "🇬🇧 EN ›" else "🇻🇳 VI ›",
                            color = TextPrimaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        color = PetrolTeal.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.55f)),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showZoneDialog = true
                        }
                    ) {
                        Text(
                            text = "📍 $headerZoneLabel ›",
                            color = PetrolTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 2. TÂM ĐIỂM THỊ GIÁC SỐ 1: HERO SPOTLIGHT CARD (Atmospheric Bokeh + Vòi Bơm Mờ + Neon Sparkline + Nút Chia Sẻ)
        item {
            val heroItem = data.items.firstOrNull { it.isFeatured } ?: data.items.first()
            val targetPrice = heroItem.getPrice(selectedZone)
            val animatedHeroPrice by animateIntAsState(
                targetValue = targetPrice,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                label = "hero_price_counter"
            )
            val previousPrice = targetPrice - heroItem.delta
            val deltaPercent = if (previousPrice > 0) (heroItem.delta.toDouble() / previousPrice) * 100 else 0.0
            val sparklinePrices = remember(data.history, targetPrice) {
                val list = data.history.map { it.ron95Price }.filter { it > 0 }.toMutableList()
                if (list.size < 2) {
                    listOf(previousPrice.coerceAtLeast(20000), targetPrice)
                } else {
                    list[list.lastIndex] = targetPrice
                    list
                }
            }

            val isLight = ThemeState.isLightMode
            val heroGradient = if (isLight) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFCCFBF1),
                        Color(0xFFE0F2FE),
                        Color(0xFFF0FDFA)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(950f, 520f)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D3B3E),
                        Color(0xFF082231),
                        Color(0xFF0A1424)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(950f, 520f)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            PetrolTeal.copy(alpha = 0.85f),
                            PetrolTeal.copy(alpha = 0.35f),
                            EnergyAmber.copy(alpha = 0.45f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(heroGradient)
                ) {
                    // Lớp nền nghệ thuật: Silhouette Vòi Bơm Xăng + Quầng sáng Bokeh Vàng/Ngọc tạo chiều sâu 3D
                    HeroAtmosphericBackdrop(
                        isLight = isLight,
                        modifier = Modifier.matchParentSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        // Hàng 1: Badge 🔥 XĂNG RON 95-III (Trái) + Nút Chia sẻ Glassmorphic (Phải)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isLight) Color(0xFFFEF3C7) else Color(0xFF2A2419).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    EnergyAmber.copy(alpha = 0.55f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🔥", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = translateFuelName(heroItem.name, lang, compact = false).uppercase(),
                                        color = if (isLight) Color(0xFFB45309) else Color(0xFFFCD34D),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.4.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Surface(
                                color = PetrolTeal.copy(alpha = if (isLight) 0.18f else 0.22f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    PetrolTeal.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.clickable { openSharePreview() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ShareNodesVectorIcon(
                                        tint = if (isLight) Color(0xFF0F766E) else Color(0xFF5EEAD4),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = tr("Chia sẻ", "Share"),
                                        color = if (isLight) Color(0xFF0F766E) else Color(0xFF99F6E4),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hàng 2: Con số Giá Khổng Lồ (Trái) + Đồ họa Sóng Neon Sparkline (Phải)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = currencyFormat.format(animatedHeroPrice),
                                        color = TextPrimaryDark,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.8).sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = tr(" đ/lít", " VND/L"),
                                        color = TextSecondaryDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                DeltaPriceBadge(delta = heroItem.delta, percent = deltaPercent)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Đồ họa Sóng Neon Sparkline 6 kỳ điều hành
                            HeroMiniSparkline(
                                prices = sparklinePrices,
                                isRaise = heroItem.delta > 0,
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(68.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(color = TextSecondaryDark.copy(alpha = 0.18f))

                        Spacer(modifier = Modifier.height(9.dp))

                        // Hàng 3: Chân thẻ có icon lịch + Chip đồng hồ cập nhật ở góc phải
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "📅 ${tr("Trước", "Prev")}: ${currencyFormat.format(previousPrice)}đ",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "  |  ",
                                    color = TextSecondaryDark.copy(alpha = 0.45f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "📅 ${getNextAdjustmentCountdownText(lang)}",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Surface(
                                color = PetrolTeal.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onRefresh() }
                            ) {
                                Text(
                                    text = "🕒 ${formatShortDayMonth(data.updatedAt)}",
                                    color = PetrolTeal,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. DẢI 3 CỘT NGANG PHÂN MÀU (🟢 E5 RON 92 | 🔵 DO 0.05S-II | 🟣 DO 0.001S-V)
        item {
            val otherItems = data.items.filter { !it.isFeatured }.take(3)
            if (otherItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    otherItems.forEach { item ->
                        FuelCompactStripCard(
                            item = item,
                            zone = selectedZone,
                            currencyFormat = currencyFormat,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val low = (item.id + " " + item.name).lowercase()
                                selectedFuelKeyword = when {
                                    low.contains("e5") || low.contains("92") -> "e5"
                                    low.contains("do") || low.contains("diesel") -> "do"
                                    else -> "95"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. THẺ TÍNH TIỀN ĐỔ XĂNG (Chuyển đổi Theo dung tích / Theo số tiền + Khung đồng hồ điện tử)
        item {
            FullTankCalculatorCard(
                presets = VEHICLE_PRESETS,
                selectedIdx = selectedVehicleIdx,
                onSelectVehicle = { idx ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedVehicleIdx = idx
                    prefs.edit().putInt("selected_vehicle_idx", idx).apply()
                },
                customLiters = customLiters,
                onChangeLiters = { newLiters ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    customLiters = (Math.round(newLiters * 10.0) / 10.0).coerceIn(1.0, 150.0)
                },
                selectedFuelKeyword = selectedFuelKeyword,
                onSelectFuelKeyword = { kw ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedFuelKeyword = kw
                },
                fuelItem = vehicleFuelItem,
                zone = selectedZone,
                fullTankCost = fullTankCost,
                fullTankDelta = fullTankDelta,
                currencyFormat = currencyFormat,
                onShareClick = openSharePreview
            )
        }

        // 5. Biểu đồ biến động giá xăng dầu lịch sử (luôn có >= 6 kỳ điều hành)
        if (data.history.size >= 2) {
            item {
                PriceHistoryChartCard(history = data.history)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showZoneDialog) {
        ZoneSelectionDialog(
            currentZone = selectedZone,
            currentProvince = selectedProvince,
            onZoneSelected = { zone, province ->
                selectedZone = zone
                selectedProvince = province
                prefs.edit().putInt("selected_zone", zone).putString("selected_province", province).apply()
                showZoneDialog = false
            },
            onDismiss = { showZoneDialog = false }
        )
    }

    previewCard?.let { cardData ->
        ShareCardPreviewDialog(
            previewData = cardData,
            onDismiss = { previewCard = null }
        )
    }
}

/** Lớp nền nghệ thuật trong thẻ Hero: Quầng sáng Bokeh Vàng/Ngọc + Silhouette Vòi Bơm Xăng mờ tạo chiều sâu */
@Composable
private fun HeroAtmosphericBackdrop(
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Quầng sáng Ngọc Lục Bảo góc phải trên
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF14B8A6).copy(alpha = if (isLight) 0.16f else 0.24f),
                    Color.Transparent
                ),
                center = Offset(w * 0.78f, h * 0.32f),
                radius = h * 0.85f
            ),
            radius = h * 0.85f,
            center = Offset(w * 0.78f, h * 0.32f)
        )

        // Bokeh Vàng Hổ Phách ấm ở rìa phải (giống ánh đèn trạm xăng ban đêm)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF59E0B).copy(alpha = if (isLight) 0.14f else 0.22f),
                    Color.Transparent
                ),
                center = Offset(w * 0.96f, h * 0.48f),
                radius = h * 0.45f
            ),
            radius = h * 0.45f,
            center = Offset(w * 0.96f, h * 0.48f)
        )

        // Silhouette Cò Bơm Xăng cách điệu mờ phía sau đồ thị Sparkline
        val nozzleAlpha = if (isLight) 0.06f else 0.09f
        val nozzleColor = if (isLight) Color(0xFF0F766E) else Color(0xFF5EEAD4)
        val bodyPath = Path().apply {
            moveTo(w * 0.64f, h * 0.72f)
            lineTo(w * 0.74f, h * 0.22f)
            lineTo(w * 0.81f, h * 0.25f)
            lineTo(w * 0.72f, h * 0.75f)
            close()
        }
        drawPath(path = bodyPath, color = nozzleColor.copy(alpha = nozzleAlpha))

        val spoutPath = Path().apply {
            moveTo(w * 0.75f, h * 0.23f)
            lineTo(w * 0.83f, h * 0.06f)
            lineTo(w * 0.86f, h * 0.08f)
            lineTo(w * 0.79f, h * 0.25f)
            close()
        }
        drawPath(path = spoutPath, color = nozzleColor.copy(alpha = nozzleAlpha * 1.2f))
    }
}

/** Icon Vector Cây Xăng sắc nét dùng cho Header & 3 thẻ nhiên liệu phụ */
@Composable
private fun FuelPumpVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)

        // Thân trụ bơm
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.16f),
            size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f),
            style = stroke
        )
        // Màn hình trụ bơm
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.27f, h * 0.26f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.20f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, w * 0.04f)
        )
        // Chân đế
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.85f),
            end = Offset(w * 0.70f, h * 0.85f),
            strokeWidth = w * 0.11f,
            cap = StrokeCap.Round
        )
        // Dây vòi bơm bên phải
        val hose = Path().apply {
            moveTo(w * 0.64f, h * 0.36f)
            cubicTo(w * 0.86f, h * 0.36f, w * 0.88f, h * 0.72f, w * 0.76f, h * 0.72f)
            lineTo(w * 0.76f, h * 0.24f)
            lineTo(w * 0.86f, h * 0.16f)
        }
        drawPath(path = hose, color = tint, style = stroke)
    }
}

/** Icon Vector Chia sẻ (3 nút nối) dùng trên góc phải Hero Card */
@Composable
private fun ShareNodesVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val left = Offset(w * 0.22f, h * 0.50f)
        val topRight = Offset(w * 0.78f, h * 0.20f)
        val bottomRight = Offset(w * 0.78f, h * 0.80f)
        val sw = w * 0.12f

        drawLine(color = tint, start = left, end = topRight, strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = tint, start = left, end = bottomRight, strokeWidth = sw, cap = StrokeCap.Round)

        val r = w * 0.15f
        drawCircle(color = tint, radius = r, center = left)
        drawCircle(color = tint, radius = r, center = topRight)
        drawCircle(color = tint, radius = r, center = bottomRight)
    }
}

/** Đồ họa sóng Neon Sparkline kèm hào quang điểm cuối ngay trong thẻ Hero */
@Composable
fun HeroMiniSparkline(
    prices: List<Int>,
    isRaise: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isRaise) Color(0xFFFB923C) else Color(0xFF10B981)
    val glowColor = if (isRaise) Color(0xFFF97316) else Color(0xFF2DD4BF)
    val lang = LocalAppLanguage.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (prices.size < 2) return@Canvas
            val minP = prices.minOrNull()?.toFloat() ?: 0f
            val maxP = prices.maxOrNull()?.toFloat() ?: 1f
            val range = (maxP - minP).coerceAtLeast(100f)

            val padX = 10.dp.toPx()
            val padY = 8.dp.toPx()
            val w = size.width - padX * 2
            val h = size.height - padY * 2

            val pts = prices.mapIndexed { idx, p ->
                val x = padX + (idx.toFloat() / (prices.size - 1)) * w
                val y = padY + h - ((p - minP) / range) * h
                Offset(x, y)
            }

            val strokePath = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (i in 0 until pts.size - 1) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(pts.last().x, size.height)
                lineTo(pts.first().x, size.height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(glowColor.copy(alpha = 0.36f), Color.Transparent)
                )
            )

            // Lớp hào quang Neon bên ngoài đường sóng
            drawPath(
                path = strokePath,
                color = glowColor.copy(alpha = 0.28f),
                style = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Đường sóng chính sắc nét
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            val last = pts.last()
            drawCircle(
                color = glowColor.copy(alpha = 0.32f),
                radius = 8.5.dp.toPx(),
                center = last
            )
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = last
            )
            drawCircle(
                color = Color.White,
                radius = 2.3.dp.toPx(),
                center = last
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (lang == AppLang.EN) "6-period trend" else "Xu hướng 6 kỳ",
            color = TextSecondaryDark.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Thẻ dọc phân màu riêng biệt cho dải 3 cột ngang (🟢 E5 RON 92 | 🔵 DO 0.05S-II | 🟣 DO 0.001S-V) */
@Composable
fun FuelCompactStripCard(
    item: FuelItem,
    zone: Int,
    currencyFormat: NumberFormat,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val accent = getFuelNozzleAccentColor(item)
    val animatedPrice by animateIntAsState(
        targetValue = item.getPrice(zone),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "strip_price_counter"
    )

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.02f))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    FuelPumpVectorIcon(
                        tint = accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = shortFuelStripTitle(item.name, lang),
                    color = TextPrimaryDark.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currencyFormat.format(animatedPrice)}đ",
                    color = TextPrimaryDark,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "›",
                    color = TextSecondaryDark.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            DeltaPriceBadge(delta = item.delta, isCompact = true)
        }
    }
}

@Composable
fun FullTankCalculatorCard(
    presets: List<VehiclePreset>,
    selectedIdx: Int,
    onSelectVehicle: (Int) -> Unit,
    customLiters: Double,
    onChangeLiters: (Double) -> Unit,
    selectedFuelKeyword: String,
    onSelectFuelKeyword: (String) -> Unit,
    fuelItem: FuelItem,
    zone: Int,
    fullTankCost: Int,
    fullTankDelta: Int,
    currencyFormat: NumberFormat,
    onShareClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val haptic = LocalHapticFeedback.current
    val selectedVehicle = presets.getOrElse(selectedIdx) { presets.first() }
    var calcMode by remember { mutableIntStateOf(0) }
    var selectedMoney by remember { mutableIntStateOf(50_000) }

    val unitPrice = fuelItem.getPrice(zone).coerceAtLeast(1)
    val prevUnitPrice = (unitPrice - fuelItem.delta).coerceAtLeast(1)

    val animatedFullTankCost by animateIntAsState(
        targetValue = fullTankCost,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "full_tank_cost_counter"
    )
    val animatedFullTankDelta by animateIntAsState(
        targetValue = fullTankDelta,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "full_tank_delta_counter"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, EnergyAmber.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header Thẻ Tính Tiền: Icon Máy Tính Xanh + Tiêu đề + Nút gạt Vàng [ 🚗 Theo dung tích | 💵 Theo số tiền ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0EA5E9).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🧮", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr("Tính tiền đổ xăng", "Fuel Cost Calculator"),
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = tr("Ước tính chi phí nhanh chóng", "Quick full-tank & cash estimate"),
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        listOf(
                            0 to tr("🚗 Theo dung tích", "🚗 By Tank"),
                            1 to tr("💵 Theo số tiền", "💵 By Cash")
                        ).forEach { (mode, label) ->
                            val active = calcMode == mode
                            Surface(
                                color = if (active) EnergyAmber else Color.Transparent,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.clickable {
                                    if (calcMode != mode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        calcMode = mode
                                    }
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) Color(0xFF1E1B18) else TextSecondaryDark,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Crossfade(
                targetState = calcMode,
                animationSpec = tween(durationMillis = 220),
                label = "calc_mode_crossfade"
            ) { currentMode ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currentMode == 0) {
                        // Hàng chọn nhanh dòng xe (1 chạm: 🛵 Vision, 🛵 AirBlade, 🛵 Wave, 🛵 Lead, 🛵 SH, 🚗 Ô tô 5 chỗ...)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEachIndexed { idx, preset ->
                                val isSelected = idx == selectedIdx
                                val vName = if (lang == AppLang.EN) preset.nameEn else preset.nameVi
                                Surface(
                                    color = if (isSelected) EnergyAmber.copy(alpha = 0.16f) else DarkBackground,
                                    shape = RoundedCornerShape(11.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) EnergyAmber else DarkSurfaceVariant
                                    ),
                                    modifier = Modifier.clickable { onSelectVehicle(idx) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = preset.emoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = vName,
                                            color = if (isSelected) EnergyAmber else TextSecondaryDark,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Hàng 2 Ô Điều Khiển Đóng Khung: [ Loại xe đang chọn ] + [ Dung tích (Lít) [- 4.9 +] ]
                        val step = if (customLiters >= 20.0) 5.0 else 0.5
                        val litersStr = String.format(Locale.US, "%.1f", customLiters).replace(".0", "")
                        val vName = if (lang == AppLang.EN) selectedVehicle.nameEn else selectedVehicle.nameVi

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Ô 1: Loại xe (Bấm để chuyển nhanh sang mẫu xe kế tiếp)
                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .clickable {
                                        val nextIdx = (selectedIdx + 1) % presets.size
                                        onSelectVehicle(nextIdx)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = selectedVehicle.emoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = tr("Loại xe", "Vehicle"),
                                                color = TextSecondaryDark,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = vName,
                                                color = TextPrimaryDark,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Text(
                                        text = "›",
                                        color = TextSecondaryDark,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Ô 2: Dung tích (Lít) kèm hộp số [4.9] và 2 nút tròn (-) (+)
                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                modifier = Modifier.weight(1.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = tr("Dung tích (Lít)", "Tank (L)"),
                                            color = TextSecondaryDark,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            color = DarkSurface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                                        ) {
                                            Text(
                                                text = "${litersStr}L",
                                                color = TextPrimaryDark,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Surface(
                                            color = DarkSurface,
                                            shape = CircleShape,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable { onChangeLiters(customLiters - step) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "−",
                                                    color = TextPrimaryDark,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Surface(
                                            color = DarkSurface,
                                            shape = CircleShape,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable { onChangeLiters(customLiters + step) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "+",
                                                    color = TextPrimaryDark,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Hộp Kết Quả Dạng Đồng Hồ Điện Tử Cây Xăng (Inset Result Box)
                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EnergyAmber.copy(alpha = 0.32f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(EnergyAmber.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FuelPumpVectorIcon(
                                            tint = EnergyAmber,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tr("Số tiền (ước tính)", "Estimated cost"),
                                            color = TextSecondaryDark,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${currencyFormat.format(animatedFullTankCost)}đ",
                                            color = EnergyAmber,
                                            fontSize = 27.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val isDrop = fullTankDelta < 0
                                    val isRaise = fullTankDelta > 0
                                    val badgeColor = when {
                                        isDrop -> PriceGreen
                                        isRaise -> PriceRed
                                        else -> TextSecondaryDark
                                    }
                                    val badgeText = when {
                                        isDrop -> "▼ ${currencyFormat.format(abs(animatedFullTankDelta))}đ/${tr("bình", "tank")}"
                                        isRaise -> "▲ +${currencyFormat.format(animatedFullTankDelta)}đ/${tr("bình", "tank")}"
                                        else -> tr("Không đổi", "Unchanged")
                                    }
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = badgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tr("So với kỳ trước", "vs previous period"),
                                        color = TextSecondaryDark,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        // CHẾ ĐỘ 2: ĐỔ THEO SỐ TIỀN (30k, 50k, 70k, 100k, 200k, 500k, 1M)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            MONEY_PRESETS.forEach { amount ->
                                val isSelected = selectedMoney == amount
                                val label = if (amount >= 1_000_000) "1 Triệu" else "${amount / 1000}k"
                                Surface(
                                    color = if (isSelected) EnergyAmber.copy(alpha = 0.2f) else DarkBackground,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) EnergyAmber else DarkSurfaceVariant
                                    ),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedMoney = amount
                                    }
                                ) {
                                    Text(
                                        text = "💵 $label",
                                        color = if (isSelected) EnergyAmber else TextSecondaryDark,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        val targetLitersNow = (selectedMoney.toDouble() / unitPrice).toFloat()
                        val animatedLitersNow by animateFloatAsState(
                            targetValue = targetLitersNow,
                            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                            label = "liters_by_money_counter"
                        )
                        val litersPrev = selectedMoney.toDouble() / prevUnitPrice
                        val litersDiff = targetLitersNow.toDouble() - litersPrev
                        val litersFormatted = String.format(if (lang == AppLang.EN) Locale.US else Locale.GERMANY, "%.2f", animatedLitersNow)
                        val diffFormatted = String.format(if (lang == AppLang.EN) Locale.US else Locale.GERMANY, "%.2f", abs(litersDiff))

                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(PetrolTeal.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FuelPumpVectorIcon(
                                            tint = PetrolTeal,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tr("Số lít nhận được", "Estimated liters"),
                                            color = TextSecondaryDark,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "$litersFormatted ${tr("Lít", "Liters")}",
                                            color = PetrolTeal,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val badgeColor = when {
                                        litersDiff < -0.005 -> PriceRed
                                        litersDiff > 0.005 -> PriceGreen
                                        else -> TextSecondaryDark
                                    }
                                    val diffText = when {
                                        litersDiff < -0.005 -> "▼ -${diffFormatted}L"
                                        litersDiff > 0.005 -> "▲ +${diffFormatted}L"
                                        else -> tr("Không đổi", "Unchanged")
                                    }
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = diffText,
                                            color = badgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${currencyFormat.format(selectedMoney)}đ",
                                        color = TextSecondaryDark,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Hàng đáy: Chọn nhanh loại nhiên liệu bằng chấm màu + Viền Vàng & Dấu ✓ khi đang chọn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("95", "RON 95", Color(0xFFF97316)),
                    Triple("e5", "E5 RON 92", Color(0xFF10B981)),
                    Triple("do", tr("Dầu DO", "Diesel"), Color(0xFF0EA5E9))
                ).forEach { (kw, label, dotColor) ->
                    val isSelected = selectedFuelKeyword == kw
                    Surface(
                        color = if (isSelected) EnergyAmber.copy(alpha = 0.16f) else DarkBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.3.dp else 1.dp,
                            if (isSelected) EnergyAmber else DarkSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectFuelKeyword(kw) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "✓",
                                    color = EnergyAmber,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tính thời gian còn lại tới kỳ điều hành giá xăng tiếp theo (15:00 chiều Thứ Năm hàng tuần). */
private fun getNextAdjustmentCountdownText(lang: AppLang): String {
    return try {
        val tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val now = Calendar.getInstance(tz)
        val target = Calendar.getInstance(tz).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (now.after(target)) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }
        val diffMs = target.timeInMillis - now.timeInMillis
        val totalHours = (diffMs / (1000 * 60 * 60)).toInt()
        val days = totalHours / 24
        val hours = totalHours % 24
        val d = target.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val m = (target.get(Calendar.MONTH) + 1).toString().padStart(2, '0')

        if (days == 0 && hours <= 6) {
            if (lang == AppLang.EN) "Next: Today 15:00" else "Kỳ tới: 15h hôm nay"
        } else {
            if (lang == AppLang.EN) "Next: Thu ($d/$m)" else "Kỳ tới: Thứ 5 ($d/$m)"
        }
    } catch (_: Exception) {
        if (lang == AppLang.EN) "Every Thu 15:00" else "Kỳ tới: 15h Thứ 5"
    }
}

private fun formatShortDayMonth(raw: String): String {
    val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(raw)
    if (iso != null) {
        val (_, mo, d) = iso.destructured
        return "$d/$mo"
    }
    val vn = Regex("""^(\d{2})/(\d{2})""").find(raw)
    if (vn != null) {
        val (d, mo) = vn.destructured
        return "$d/$mo"
    }
    return raw.take(5)
}

@Composable
fun DeltaPriceBadge(delta: Int, percent: Double? = null, isCompact: Boolean = false) {
    val isDrop = delta < 0
    val isRaise = delta > 0

    val bgColor = when {
        isDrop -> PriceGreen.copy(alpha = 0.15f)
        isRaise -> PriceRed.copy(alpha = 0.15f)
        else -> TextSecondaryDark.copy(alpha = 0.15f)
    }

    val textColor = when {
        isDrop -> PriceGreen
        isRaise -> PriceRed
        else -> TextSecondaryDark
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (percent != null && !isCompact) {
            val pctText = String.format(Locale.GERMANY, "%.1f", kotlin.math.abs(percent))
            Text(
                text = when {
                    isRaise -> "▲ +${delta}đ · +$pctText%"
                    isDrop -> "▼ ${-delta}đ · -$pctText%"
                    else -> tr("Không đổi", "Unchanged")
                },
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        } else {
            val text = when {
                isDrop -> if (isCompact) "▼ ${-delta}đ" else tr("▼ Giảm ${-delta} đ/lít", "▼ Down ${-delta} VND/L")
                isRaise -> if (isCompact) "▲ +${delta}đ" else tr("▲ Tăng ${delta} đ/lít", "▲ Up +${delta} VND/L")
                else -> tr("Không đổi", "Unchanged")
            }
            Text(
                text = text,
                color = textColor,
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

data class ProvinceZoneItem(
    val name: String,
    val zone: Int,
    val description: String,
    val subNote: String = "",
    val aliases: String = ""
)

@Composable
fun ZoneSelectionDialog(
    currentZone: Int,
    currentProvince: String,
    onZoneSelected: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val allProvinces = remember {
        listOf(
            ProvinceZoneItem("TP. Hà Nội", 1, "Thủ đô, tổng kho Đức Giang (Giá chuẩn)", "Thủ đô", "ha noi"),
            ProvinceZoneItem("TP. Hồ Chí Minh", 1, "Đô thị lớn, tổng kho Nhà Bè (Giá chuẩn)", "Gồm TP.HCM, Bình Dương, Bà Rịa - Vũng Tàu", "tphcm sai gon binh duong vung tau"),
            ProvinceZoneItem("TP. Hải Phòng", 1, "Thành phố cảng, kho xăng dầu B12 (Giá chuẩn)", "Gồm Hải Phòng, Hải Dương", "hai phong hai duong"),
            ProvinceZoneItem("TP. Đà Nẵng", 1, "Tổng kho Nước Mặn, Liên Chiểu (Giá chuẩn)", "Gồm Đà Nẵng, Quảng Nam", "da nang quang nam hoi an"),
            ProvinceZoneItem("TP. Cần Thơ", 1, "Trung tâm ĐBSCL, tổng kho Miền Tây (Giá chuẩn)", "Gồm Cần Thơ, Hậu Giang, Sóc Trăng", "can tho hau giang soc trang"),
            ProvinceZoneItem("TP. Huế", 1, "Cố đô Huế, kho Thuận An (Giá chuẩn)", "Thành phố Huế", "hue thua thien hue"),
            ProvinceZoneItem("Lạng Sơn", 2, "Miền núi phía Bắc (+2%)", "Cửa khẩu Hữu Nghị, Đồng Đăng", "lang son dong dang huu nghi"),
            ProvinceZoneItem("Cao Bằng", 2, "Miền núi phía Bắc (+2%)", "Thác Bản Giốc, Pác Bó", "cao bang pac bo"),
            ProvinceZoneItem("Tuyên Quang", 2, "Miền núi phía Bắc (+2%)", "Gồm Tuyên Quang, Hà Giang", "tuyen quang ha giang dong van meo vac"),
            ProvinceZoneItem("Thái Nguyên", 2, "Miền núi phía Bắc (+2%)", "Gồm Thái Nguyên, Bắc Kạn", "thai nguyen bac kan ba be"),
            ProvinceZoneItem("Lào Cai", 2, "Miền núi Tây Bắc (+2%)", "Gồm Lào Cai, Yên Bái", "lao cai yen bai sa pa bat xat"),
            ProvinceZoneItem("Điện Biên", 2, "Miền núi Tây Bắc (+2%)", "Điện Biên Phủ, Mường Nhé", "dien bien phu muong nhe"),
            ProvinceZoneItem("Lai Châu", 2, "Miền núi Tây Bắc (+2%)", "Tây Bắc hiểm trở", "lai chau tam duong"),
            ProvinceZoneItem("Sơn La", 2, "Miền núi Tây Bắc (+2%)", "Mộc Châu, Sông Mã", "son la moc chau"),
            ProvinceZoneItem("Gia Lai", 2, "Tây Nguyên (+2%)", "Gồm Gia Lai, Bình Định", "gia lai pleiku binh dinh quy nhon"),
            ProvinceZoneItem("Đắk Lắk", 2, "Tây Nguyên (+2%)", "Thủ phủ cà phê Buôn Ma Thuột", "dak lak dac lac buon ma thuot bmt"),
            ProvinceZoneItem("Lâm Đồng", 2, "Tây Nguyên (+2%)", "Gồm Lâm Đồng, Đắk Nông, Bình Thuận", "lam dong da lat dak nong binh thuan phan thiet gia nghia"),
            ProvinceZoneItem("Quảng Ngãi", 2, "Khu vực miền núi & duyên hải (+2%)", "Gồm Quảng Ngãi, Kon Tum", "quang ngai kon tum mang den"),
            ProvinceZoneItem("Cà Mau", 2, "Cực Nam, chi phí vận tải xa (+2%)", "Gồm Cà Mau, Bạc Liêu", "ca mau bac lieu"),
            ProvinceZoneItem("An Giang", 1, "Tây Nam Bộ (Giá chuẩn)", "Gồm An Giang, Kiên Giang", "an giang kien giang rach gia chau doc ha tien"),
            ProvinceZoneItem("Bắc Ninh", 1, "Đồng bằng sông Hồng (Giá chuẩn)", "Gồm Bắc Ninh, Bắc Giang", "bac ninh bac giang"),
            ProvinceZoneItem("Đồng Nai", 1, "Đông Nam Bộ, kho Gò Dầu (Giá chuẩn)", "Gồm Đồng Nai, Bình Phước", "dong nai bien hoa binh phuoc dong xoai"),
            ProvinceZoneItem("Đồng Tháp", 1, "Tây Nam Bộ (Giá chuẩn)", "Gồm Đồng Tháp, Tiền Giang, Bến Tre", "dong thap tien giang ben tre my tho cao lanh"),
            ProvinceZoneItem("Hà Tĩnh", 1, "Cảng nước sâu Vũng Áng (Giá chuẩn)", "Bắc Trung Bộ", "ha tinh vung ang"),
            ProvinceZoneItem("Hưng Yên", 1, "Đồng bằng sông Hồng (Giá chuẩn)", "Gồm Hưng Yên, Thái Bình", "hung yen thai binh"),
            ProvinceZoneItem("Khánh Hòa", 1, "Tổng kho Cam Ranh, Vân Phong (Giá chuẩn)", "Gồm Khánh Hòa, Ninh Thuận, Phú Yên", "khanh hoa nha trang ninh thuan phan rang phu yen tuy hoa"),
            ProvinceZoneItem("Nghệ An", 1, "Kho Cửa Lò, Bến Thủy (Giá chuẩn)", "Bắc Trung Bộ, TP Vinh", "nghe an vinh cua lo"),
            ProvinceZoneItem("Ninh Bình", 1, "Đồng bằng sông Hồng (Giá chuẩn)", "Gồm Ninh Bình, Nam Định, Hà Nam", "ninh binh nam dinh ha nam phu ly"),
            ProvinceZoneItem("Phú Thọ", 1, "Trung du Bắc Bộ (Giá chuẩn)", "Gồm Phú Thọ, Vĩnh Phúc, Hòa Bình", "phu tho viet tri vinh phuc vinh yen hoa binh"),
            ProvinceZoneItem("Quảng Ninh", 1, "Cảng đầu mối xăng dầu B12 (Giá chuẩn)", "Hạ Long, Cẩm Phả, B12", "quang ninh ha long cam pha bai chay"),
            ProvinceZoneItem("Quảng Trị", 1, "Bắc Trung Bộ (Giá chuẩn)", "Gồm Quảng Trị, Quảng Bình", "quang tri dong ha quang binh dong hoi"),
            ProvinceZoneItem("Tây Ninh", 1, "Đông Nam Bộ (Giá chuẩn)", "Gồm Tây Ninh, Long An", "tay ninh long an tan an"),
            ProvinceZoneItem("Thanh Hóa", 1, "Nhà máy Lọc hóa dầu Nghi Sơn (Giá chuẩn)", "Nghi Sơn, Bỉm Sơn", "thanh hoa nghi son"),
            ProvinceZoneItem("Vĩnh Long", 1, "Tây Nam Bộ (Giá chuẩn)", "Gồm Vĩnh Long, Trà Vinh", "vinh long tra vinh"),
            ProvinceZoneItem("Huyện đảo & Quần đảo", 2, "Hải đảo cước đường biển (+2%)", "Phú Quốc, Côn Đảo, Cát Bà, Lý Sơn, Trường Sa...", "phu quoc con dao cat ba ly son co to bach long vy truong sa hoang sa")
        )
    }

    val filteredProvinces = remember(searchQuery, allProvinces) {
        val q = removeAccents(searchQuery)
        if (q.isEmpty()) emptyList()
        else allProvinces.filter {
            removeAccents(it.name).contains(q) ||
                removeAccents(it.description).contains(q) ||
                removeAccents(it.subNote).contains(q) ||
                removeAccents(it.aliases).contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📍", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = tr("Vùng 1 & Vùng 2 là gì?", "What are Zone 1 & Zone 2?"),
                        color = TextPrimaryDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr("Cơ cấu 34 tỉnh thành & quy định giá Petrolimex", "Vietnam regional fuel pricing by Petrolimex"),
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr(
                        "Bộ Công Thương & Petrolimex phân chia 34 tỉnh thành thành 2 vùng giá theo chi phí vận chuyển:",
                        "Vietnam divides provinces into 2 pricing zones based on transport distance from main fuel depots:"
                    ),
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onZoneSelected(1, "") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentZone == 1) PetrolTeal.copy(alpha = 0.15f) else DarkBackground
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (currentZone == 1) PetrolTeal else DarkSurfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("🟢 Vùng 1: Giá chuẩn (Thấp nhất)", "🟢 Zone 1: Standard Price"),
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentZone == 1) {
                                Text(text = tr("✓ Đang chọn", "✓ Selected"), color = PetrolTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(
                                "21 tỉnh & TP đồng bằng, ven biển gần cảng/tổng kho (Hà Nội, TP.HCM, Hải Phòng, Đà Nẵng, Cần Thơ, Bắc Ninh...).",
                                "Major cities & coastal provinces near ports (Hanoi, HCMC, Da Nang, Hai Phong, Can Tho, Nha Trang...)."
                            ),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onZoneSelected(2, "") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentZone == 2) EnergyAmber.copy(alpha = 0.15f) else DarkBackground
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (currentZone == 2) EnergyAmber else DarkSurfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("🟡 Vùng 2: Cao hơn tối đa 2%", "🟡 Zone 2: Remote (+2% max)"),
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentZone == 2) {
                                Text(text = tr("✓ Đang chọn", "✓ Selected"), color = EnergyAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(
                                "13 tỉnh miền núi, Tây Nguyên (Lạng Sơn, Cao Bằng, Tuyên Quang, Lào Cai, Sơn La, Lâm Đồng, Đắk Lắk...) & tất cả hải đảo.",
                                "Highland, mountainous provinces (Ha Giang, Sa Pa, Da Lat, Buon Ma Thuot, Lang Son...) & all islands (Phu Quoc, Con Dao)."
                            ),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            tr("🔍 Nhập tỉnh của bạn (vd: Lạng Sơn, Hà Giang, TP.HCM...)", "🔍 Search province/city (e.g. Hanoi, Da Lat, Phu Quoc...)"),
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = PetrolTeal,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        cursorColor = PetrolTeal
                    )
                )

                if (filteredProvinces.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredProvinces.take(5).forEach { item ->
                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onZoneSelected(item.zone, "${item.name} (Z${item.zone})")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.name, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        if (item.subNote.isNotEmpty()) {
                                            Text(text = item.subNote, color = TextSecondaryDark, fontSize = 10.sp)
                                        }
                                    }
                                    Text(
                                        text = tr("➔ Vùng ${item.zone}", "➔ Zone ${item.zone}"),
                                        color = if (item.zone == 1) PetrolTeal else EnergyAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else if (searchQuery.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Hà Nội" to 1, "TP.HCM" to 1, "Lạng Sơn" to 2, "Đà Nẵng" to 1, "Lâm Đồng" to 2).forEach { (prov, z) ->
                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                modifier = Modifier.clickable {
                                    onZoneSelected(z, "$prov (Z$z)")
                                }
                            ) {
                                Text(
                                    text = prov,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
            ) {
                Text(tr("Xong", "Done"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun removeAccents(str: String): String {
    return try {
        val nfd = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        pattern.matcher(nfd).replaceAll("").replace("đ", "d").replace("Đ", "D").lowercase().trim()
    } catch (e: Exception) {
        str.lowercase().trim()
    }
}

@Composable
fun NearbyFuelRadarCard(
    context: Context,
    onViewMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF14B8A6),
                                        Color(0xFF0D9488),
                                        Color(0xFF042F2E)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LocationPinVector,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tr("Trạm Xăng Quanh Bạn", "Nearby Gas Stations"),
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tr("Mở Google Maps chỉ đường 1 chạm", "1-tap navigation via Google Maps"),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuelRadarActionChip(
                    title = tr("Gần nhất", "Nearest"),
                    subtitle = tr("Mọi hãng", "All brands"),
                    icon = "🧭",
                    accentColor = PetrolTeal,
                    onClick = { openMapsSearch(context, "cây xăng gần đây") },
                    modifier = Modifier.weight(1f)
                )

                FuelRadarActionChip(
                    title = "Petrolimex",
                    subtitle = tr("Chính hãng", "Official"),
                    icon = "🟠",
                    accentColor = Color(0xFFF97316),
                    onClick = { openMapsSearch(context, "cây xăng Petrolimex gần đây") },
                    modifier = Modifier.weight(1f)
                )

                FuelRadarActionChip(
                    title = "PVOIL",
                    subtitle = "PVOIL Easy",
                    icon = "🔵",
                    accentColor = Color(0xFF38BDF8),
                    onClick = { openMapsSearch(context, "cây xăng PVOIL gần đây") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FuelRadarActionChip(
    title: String,
    subtitle: String,
    icon: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = TextPrimaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

fun openMapsSearch(context: Context, query: String) {
    try {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        try {
            val webUri = Uri.parse("https://www.google.com/maps/search/${Uri.encode(query)}")
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {}
    }
}

fun formatIsoDateTime(raw: String): String {
    val m = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})""").find(raw) ?: return raw
    val (y, mo, d, h, mi) = m.destructured
    return "$d/$mo/$y $h:$mi"
}

fun formatCompactDateTime(raw: String): String {
    val iso = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})""").find(raw)
    if (iso != null) {
        val (_, mo, d, h, mi) = iso.destructured
        return "$d/$mo • $h:$mi"
    }
    val vn = Regex("""^(\d{2})/(\d{2})/\d{4}\s+(\d{2}:\d{2})""").find(raw)
    if (vn != null) {
        val (d, mo, hm) = vn.destructured
        return "$d/$mo • $hm"
    }
    return raw
}
