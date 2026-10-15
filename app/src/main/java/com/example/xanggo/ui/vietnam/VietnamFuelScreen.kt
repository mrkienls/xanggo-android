package com.example.xanggo.ui.vietnam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
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
import com.example.xanggo.util.ShareCardGenerator
import com.example.xanggo.widget.XangGoWidgetProvider
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.roundToInt

data class VehiclePreset(
    val id: String,
    val nameVi: String,
    val nameEn: String,
    val emoji: String,
    val tankLiters: Double,
    val defaultFuelKeyword: String = "ron95"
)

private val VEHICLE_PRESETS = listOf(
    VehiclePreset("vision", "Honda Vision", "Honda Vision", "🛵", 4.9, "ron95"),
    VehiclePreset("airblade", "AirBlade", "AirBlade", "🛵", 4.4, "ron95"),
    VehiclePreset("wave", "Wave Alpha", "Wave Alpha", "🛵", 3.7, "e5"),
    VehiclePreset("lead", "Honda Lead", "Honda Lead", "🛵", 6.0, "ron95"),
    VehiclePreset("sh", "Honda SH", "Honda SH", "🛵", 7.0, "ron95"),
    VehiclePreset("exciter", "Exciter / Winner", "Exciter / Winner", "🏍️", 5.0, "ron95"),
    VehiclePreset("sedan", "Ô tô 5 chỗ", "Sedan (45L)", "🚗", 45.0, "ron95"),
    VehiclePreset("suv", "SUV 7 chỗ", "SUV / CUV (58L)", "🚙", 58.0, "ron95"),
    VehiclePreset("pickup", "Bán tải Diesel", "Pickup Diesel", "🛻", 76.0, "do")
)

@Composable
fun VietnamFuelScreen(
    data: VietnamFuelData,
    worldData: WorldFuelSummary? = null,
    onNavigateToStations: () -> Unit = {},
    onNavigateToWorld: () -> Unit = {},
    onToggleLanguage: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val prefs = remember { context.getSharedPreferences("xanggo_prefs", Context.MODE_PRIVATE) }
    var selectedZone by remember { mutableIntStateOf(prefs.getInt("selected_zone", 1)) }
    var selectedProvince by remember { mutableStateOf(prefs.getString("selected_province", "") ?: "") }
    var selectedVehicleIdx by remember { mutableIntStateOf(prefs.getInt("selected_vehicle_idx", 0).coerceIn(0, VEHICLE_PRESETS.lastIndex)) }
    var showZoneDialog by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    // Tự động đồng bộ dữ liệu ra Widget màn hình chính
    LaunchedEffect(data, selectedZone) {
        XangGoWidgetProvider.saveAndRefreshWidget(context, data, selectedZone)
    }

    val currentVehicle = VEHICLE_PRESETS.getOrElse(selectedVehicleIdx) { VEHICLE_PRESETS.first() }
    val vehicleFuelItem = remember(data.items, currentVehicle) {
        data.items.firstOrNull {
            it.id.lowercase().contains(currentVehicle.defaultFuelKeyword) ||
            it.name.lowercase().contains(currentVehicle.defaultFuelKeyword)
        } ?: data.items.firstOrNull { it.isFeatured } ?: data.items.first()
    }
    val fullTankCost = (vehicleFuelItem.getPrice(selectedZone) * currentVehicle.tankLiters).roundToInt()
    val fullTankDelta = (vehicleFuelItem.delta * currentVehicle.tankLiters).roundToInt()

    val displayZoneLabel = when {
        selectedProvince.isNotEmpty() && !selectedProvince.startsWith("Vùng ") -> selectedProvince
        selectedZone == 1 -> tr("Vùng 1 (Chuẩn)", "Zone 1 (Std)")
        else -> tr("Vùng 2 (+2%)", "Zone 2 (+2%)")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header: Logo & Region Switcher + Nút Ngôn ngữ VI/EN
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Logo Box đỏ nổi bật
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⛽", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row {
                        Text(
                            text = "Xăng",
                            color = TextPrimaryDark,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Go",
                            color = PetrolTeal,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Nút chuyển ngôn ngữ nhanh VI / EN
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.clickable { onToggleLanguage() }
                    ) {
                        Text(
                            text = if (lang == AppLang.EN) "🇬🇧 EN" else "🇻🇳 VI",
                            color = TextPrimaryDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }

                    // Nút chọn Vùng 1 / Vùng 2
                    Surface(
                        color = PetrolTeal.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { showZoneDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 $displayZoneLabel ›",
                                color = PetrolTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Dòng thời gian cập nhật tinh gọn & chuyên nghiệp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🕒 ${tr("Cập nhật", "Updated")}: ${formatIsoDateTime(data.updatedAt)}",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Nguồn: Petrolimex", "Source: Petrolimex"),
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 2. Hero Card: RON 95-III (Hiệu ứng viền phát sáng, % biến động & Nút Chia sẻ nhanh)
        item {
            val heroItem = data.items.firstOrNull { it.isFeatured } ?: data.items.first()
            val currentPrice = heroItem.getPrice(selectedZone)
            val previousPrice = currentPrice - heroItem.delta
            val deltaPercent = if (previousPrice > 0) (heroItem.delta.toDouble() / previousPrice) * 100 else 0.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            PetrolTeal.copy(alpha = 0.45f),
                            Color.Transparent,
                            PetrolTeal.copy(alpha = 0.25f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                color = PetrolTeal.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⛽",
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = translateFuelName(heroItem.name, lang),
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Nút Chia sẻ bảng giá 1 chạm (Viral Hook)
                        Surface(
                            color = PetrolTeal.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                ShareCardGenerator.shareVietnamFuelCard(
                                    context = context,
                                    data = data,
                                    zone = selectedZone,
                                    provinceLabel = displayZoneLabel,
                                    vehicleName = if (lang == AppLang.EN) currentVehicle.nameEn else currentVehicle.nameVi,
                                    tankLiters = currentVehicle.tankLiters,
                                    selectedFuelName = vehicleFuelItem.name,
                                    fullTankCost = fullTankCost,
                                    fullTankDelta = fullTankDelta,
                                    lang = lang
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("📤 Chia sẻ", "📤 Share"),
                                    color = PetrolTeal,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currencyFormat.format(currentPrice),
                            color = TextPrimaryDark,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = tr(" đ/lít", " VND/L"),
                            color = TextSecondaryDark,
                            fontSize = 16.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 5.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Badge Tăng / Giảm giá kèm %
                    DeltaPriceBadge(delta = heroItem.delta, percent = deltaPercent)

                    Spacer(modifier = Modifier.height(8.dp))

                    val unitShort = tr("đ/lít", "VND/L")
                    val prevLabel = tr("Kỳ trước", "Prev")
                    val zLabel = tr("Vùng", "Zone")
                    Text(
                        text = "$prevLabel: ${currencyFormat.format(previousPrice)}\u00A0$unitShort  •  $zLabel ${if (selectedZone == 1) "2: " + currencyFormat.format(heroItem.zone2Price) else "1: " + currencyFormat.format(heroItem.zone1Price)}\u00A0$unitShort",
                        color = TextSecondaryDark,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. Bento Grid các mặt hàng khác (E5, Diesel...)
        item {
            val otherItems = data.items.filter { !it.isFeatured }
            val chunks = otherItems.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (chunk in chunks) {
                    if (chunk.size == 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (item in chunk) {
                                FuelBentoCard(
                                    item = item,
                                    zone = selectedZone,
                                    currencyFormat = currencyFormat,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        FuelWideCard(
                            item = chunk[0],
                            zone = selectedZone,
                            currencyFormat = currencyFormat,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. TÍNH NĂNG VIRAL: ĐỔ ĐẦY BÌNH XE CỦA BẠN HẾT BAO NHIÊU?
        item {
            FullTankCalculatorCard(
                presets = VEHICLE_PRESETS,
                selectedIdx = selectedVehicleIdx,
                onSelectVehicle = { idx ->
                    selectedVehicleIdx = idx
                    prefs.edit().putInt("selected_vehicle_idx", idx).apply()
                },
                fuelName = translateFuelName(vehicleFuelItem.name, lang),
                fullTankCost = fullTankCost,
                fullTankDelta = fullTankDelta,
                currencyFormat = currencyFormat,
                onShareClick = {
                    ShareCardGenerator.shareVietnamFuelCard(
                        context = context,
                        data = data,
                        zone = selectedZone,
                        provinceLabel = displayZoneLabel,
                        vehicleName = if (lang == AppLang.EN) currentVehicle.nameEn else currentVehicle.nameVi,
                        tankLiters = currentVehicle.tankLiters,
                        selectedFuelName = vehicleFuelItem.name,
                        fullTankCost = fullTankCost,
                        fullTankDelta = fullTankDelta,
                        lang = lang
                    )
                }
            )
        }

        // 5. Biểu đồ biến động giá xăng dầu lịch sử (cần tối thiểu 2 kỳ thật)
        if (data.history.size >= 2) {
            item {
                PriceHistoryChartCard(history = data.history)
            }
        } else {
            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📊", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr("Lịch sử giá", "Price History"),
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("Biểu đồ sẽ hiện từ kỳ điều hành tiếp theo", "Chart will appear after next price adjustment"),
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. Thẻ Trạm xăng quanh bạn (Smart Fuel Radar - 1 Chạm dẫn đường)
        item {
            NearbyFuelRadarCard(
                context = context,
                onViewMore = onNavigateToStations
            )
        }

        // 7. Thẻ Xem nhanh So sánh ASEAN (CTA sang màn hình Thế Giới)
        val asean = worldData?.countries?.filter { it.region == "ASEAN" }.orEmpty()
        val vn = asean.firstOrNull { it.isCurrent }
        val cheapest = asean.filter { !it.isCurrent }.minByOrNull { it.priceVnd }
        val priciest = asean.filter { !it.isCurrent }.maxByOrNull { it.priceVnd }
        if (vn != null && cheapest != null && priciest != null) item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToWorld() },
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🌍", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr("So sánh Đông Nam Á", "ASEAN Comparison"),
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = PetrolTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = tr("Xem tất cả →", "See all →"),
                                color = PetrolTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple(vn, tr("Việt Nam", "Vietnam"), TextPrimaryDark),
                            Triple(cheapest, tr("Rẻ nhất", "Cheapest"), PriceGreen),
                            Triple(priciest, tr("Đắt nhất", "Priciest"), PriceRed)
                        ).forEachIndexed { i, (country, label, color) ->
                            val cName = translateCountryName(country.name, country.code, lang)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = when (i) {
                                    0 -> Alignment.Start
                                    1 -> Alignment.CenterHorizontally
                                    else -> Alignment.End
                                }
                            ) {
                                Text(
                                    text = "${country.flag} ${if (i == 0) label else cName}",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${currencyFormat.format(country.priceVnd)} đ",
                                    color = color,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (i > 0) {
                                    Text(
                                        text = label,
                                        color = TextSecondaryDark.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
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
}

@Composable
fun FullTankCalculatorCard(
    presets: List<VehiclePreset>,
    selectedIdx: Int,
    onSelectVehicle: (Int) -> Unit,
    fuelName: String,
    fullTankCost: Int,
    fullTankDelta: Int,
    currencyFormat: NumberFormat,
    onShareClick: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val selectedVehicle = presets.getOrElse(selectedIdx) { presets.first() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, EnergyAmber.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tiêu đề + Nút Chia sẻ ảnh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        color = EnergyAmber.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🛵",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr("Đổ Đầy Bình Hết Bao Nhiêu?", "Full Tank Cost Calculator"),
                            color = TextPrimaryDark,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tr("Chọn xe của bạn để tính nhanh 1 chạm", "Tap your vehicle to calculate full-tank price"),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Hàng chọn nhanh dòng xe (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { idx, preset ->
                    val isSelected = idx == selectedIdx
                    val vName = if (lang == AppLang.EN) preset.nameEn else preset.nameVi
                    Surface(
                        color = if (isSelected) EnergyAmber.copy(alpha = 0.18f) else DarkBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EnergyAmber else DarkSurfaceVariant
                        ),
                        modifier = Modifier.clickable { onSelectVehicle(idx) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = preset.emoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$vName (${preset.tankLiters}L)",
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

            // Kết quả tính tiền đổ đầy bình + Chênh lệch
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val vName = if (lang == AppLang.EN) selectedVehicle.nameEn else selectedVehicle.nameVi
                        Text(
                            text = "${selectedVehicle.emoji} $vName • ${selectedVehicle.tankLiters}L ($fuelName)",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${currencyFormat.format(fullTankCost)} đ",
                            color = TextPrimaryDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val deltaColor = when {
                            fullTankDelta < 0 -> PriceGreen
                            fullTankDelta > 0 -> PriceRed
                            else -> TextSecondaryDark
                        }
                        val deltaLabel = when {
                            fullTankDelta < 0 -> tr(
                                "▼ Tiết kiệm ${currencyFormat.format(abs(fullTankDelta))} đ/bình so với kỳ trước",
                                "▼ Save ${currencyFormat.format(abs(fullTankDelta))} VND/tank vs last period"
                            )
                            fullTankDelta > 0 -> tr(
                                "▲ Đắt thêm +${currencyFormat.format(fullTankDelta)} đ/bình so với kỳ trước",
                                "▲ +${currencyFormat.format(fullTankDelta)} VND/tank vs last period"
                            )
                            else -> tr("Không đổi so với kỳ trước", "Unchanged vs last period")
                        }
                        Text(
                            text = deltaLabel,
                            color = deltaColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Nút Gửi cho bạn bè / Zalo
                    Surface(
                        color = PetrolTeal,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onShareClick() }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📤", fontSize = 15.sp)
                            Text(
                                text = tr("Gửi ảnh", "Share"),
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FuelBentoCard(
    item: FuelItem,
    zone: Int,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = translateFuelName(item.name, lang),
                color = TextSecondaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${currencyFormat.format(item.getPrice(zone))} đ",
                color = TextPrimaryDark,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(6.dp))

            DeltaPriceBadge(delta = item.delta, isCompact = true)
        }
    }
}

@Composable
fun FuelWideCard(
    item: FuelItem,
    zone: Int,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translateFuelName(item.name, lang),
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                DeltaPriceBadge(delta = item.delta, isCompact = true)
            }

            Text(
                text = "${currencyFormat.format(item.getPrice(zone))} ${tr("đ/lít", "VND/L")}",
                color = TextPrimaryDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
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

@Composable
fun AdMobNativePlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, TextSecondaryDark.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = EnergyAmber.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Tài trợ",
                    color = EnergyAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Mở thẻ hoàn tiền xăng dầu 10%",
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Nhận ngay voucher đổ xăng 500k khi đăng ký online",
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }
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
            // 6 THÀNH PHỐ TRỰC THUỘC TRUNG ƯƠNG
            ProvinceZoneItem("TP. Hà Nội", 1, "Thủ đô, tổng kho Đức Giang (Giá chuẩn)", "Thủ đô", "ha noi"),
            ProvinceZoneItem("TP. Hồ Chí Minh", 1, "Đô thị lớn, tổng kho Nhà Bè (Giá chuẩn)", "Gồm TP.HCM, Bình Dương, Bà Rịa - Vũng Tàu", "tphcm sai gon binh duong vung tau"),
            ProvinceZoneItem("TP. Hải Phòng", 1, "Thành phố cảng, kho xăng dầu B12 (Giá chuẩn)", "Gồm Hải Phòng, Hải Dương", "hai phong hai duong"),
            ProvinceZoneItem("TP. Đà Nẵng", 1, "Tổng kho Nước Mặn, Liên Chiểu (Giá chuẩn)", "Gồm Đà Nẵng, Quảng Nam", "da nang quang nam hoi an"),
            ProvinceZoneItem("TP. Cần Thơ", 1, "Trung tâm ĐBSCL, tổng kho Miền Tây (Giá chuẩn)", "Gồm Cần Thơ, Hậu Giang, Sóc Trăng", "can tho hau giang soc trang"),
            ProvinceZoneItem("TP. Huế", 1, "Cố đô Huế, kho Thuận An (Giá chuẩn)", "Thành phố Huế", "hue thua thien hue"),

            // 13 TỈNH THUỘC VÙNG 2 (Miền núi, Tây Nguyên, cước vận chuyển + tối đa 2%)
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

            // 15 TỈNH THUỘC VÙNG 1 (Đồng bằng & duyên hải gần cảng, nguồn cấp giá chuẩn)
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

            // ĐẶC THÙ HẢI ĐẢO (Áp dụng giá Vùng 2 theo quy định Petrolimex)
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
                // Giải thích ngắn gọn
                Text(
                    text = tr(
                        "Bộ Công Thương & Petrolimex phân chia 34 tỉnh thành thành 2 vùng giá theo chi phí vận chuyển:",
                        "Vietnam divides provinces into 2 pricing zones based on transport distance from main fuel depots:"
                    ),
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // THẺ VÙNG 1
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

                // THẺ VÙNG 2
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

                // Ô TÌM NHANH TỈNH CỦA BẠN
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

                // Danh sách kết quả tìm kiếm tỉnh
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
                    // Gợi ý bấm nhanh tỉnh phổ biến
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
            // Header: Icon + Title + GPS status badge
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

            // 3 Quick action buttons
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
            // Mở link Google Maps web nếu app bản đồ chưa cài đặt
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

/** "2026-10-10T15:05:12.3+07:00" -> "10/10/2026 15:05". Chuỗi không phải ISO thì giữ nguyên. */
fun formatIsoDateTime(raw: String): String {
    val m = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})""").find(raw) ?: return raw
    val (y, mo, d, h, mi) = m.destructured
    return "$d/$mo/$y $h:$mi"
}
