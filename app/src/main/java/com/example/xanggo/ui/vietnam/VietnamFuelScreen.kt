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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.xanggo.data.model.FuelItem
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.PriceHistoryChartCard
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

@Composable
fun VietnamFuelScreen(
    data: VietnamFuelData,
    onNavigateToWorld: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("xanggo_prefs", Context.MODE_PRIVATE) }
    var selectedZone by remember { mutableIntStateOf(prefs.getInt("selected_zone", 1)) }
    var selectedProvince by remember { mutableStateOf(prefs.getString("selected_province", "Vùng 1 (Giá chuẩn)") ?: "Vùng 1 (Giá chuẩn)") }
    var showZoneDialog by remember { mutableStateOf(false) }
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Logo & Region Switcher
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⛽", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "XăngGo",
                        color = TextPrimaryDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Nút chọn Vùng 1 / Vùng 2 (Mở hộp thoại giải thích & chọn tỉnh)
                Surface(
                    color = PetrolTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { showZoneDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📍 ", fontSize = 13.sp)
                        Text(
                            text = if (selectedZone == 1) "Vùng 1 (Giá chuẩn)" else "Vùng 2 (+2% cước)",
                            color = PetrolTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = " ▾",
                            color = PetrolTeal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Dòng thời gian cập nhật
            Text(
                text = "Cập nhật: ${data.updatedAt} • ${data.source}",
                color = TextSecondaryDark,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 2. Hero Card: RON 95-III (Nhiều người dùng nhất)
        item {
            val heroItem = data.items.firstOrNull { it.isFeatured } ?: data.items.first()
            val currentPrice = heroItem.getPrice(selectedZone)
            val previousPrice = currentPrice - heroItem.delta

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = heroItem.name,
                            color = TextSecondaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Badge Tăng / Giảm giá
                        DeltaPriceBadge(delta = heroItem.delta)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currencyFormat.format(currentPrice),
                            color = TextPrimaryDark,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = " đ/lít",
                            color = TextSecondaryDark,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Kỳ trước: ${currencyFormat.format(previousPrice)} đ • Vùng ${if (selectedZone == 1) "2: " + currencyFormat.format(heroItem.zone2Price) + " đ" else "1: " + currencyFormat.format(heroItem.zone1Price) + " đ"}",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 3. Biểu đồ biến động giá xăng dầu lịch sử
        if (data.history.isNotEmpty()) {
            item {
                PriceHistoryChartCard(history = data.history)
            }
        }

        // 4. Bento Grid các mặt hàng khác (E5, Diesel...)
        item {
            val otherItems = data.items.filter { !it.isFeatured }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (chunk in otherItems.chunked(2)) {
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
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 4. Vị trí AdMob Native Ad (Theo kế hoạch đã phê duyệt)
        item {
            AdMobNativePlaceholder()
        }

        // 5. Thẻ Xem nhanh So sánh ASEAN (CTA sang màn hình 2)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToWorld() },
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌍 So sánh giá xăng ASEAN",
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Xem tất cả →",
                            color = PetrolTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🇻🇳 VN: 21.310 đ  vs  🇲🇾 Malaysia: 11.382 đ  vs  🇹🇭 Thái: 29.748 đ",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Tránh che banner đáy
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
fun FuelBentoCard(
    item: FuelItem,
    zone: Int,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = item.name,
                color = TextSecondaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${currencyFormat.format(item.getPrice(zone))} đ",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            DeltaPriceBadge(delta = item.delta, isCompact = true)
        }
    }
}

@Composable
fun DeltaPriceBadge(delta: Int, isCompact: Boolean = false) {
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

    val text = when {
        isDrop -> if (isCompact) "▼ ${-delta}đ" else "▼ Giảm ${-delta} đ/L"
        isRaise -> if (isCompact) "▲ +${delta}đ" else "▲ Tăng ${delta} đ/L"
        else -> "Giữ nguyên"
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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

data class ProvinceZoneItem(val name: String, val zone: Int, val description: String)

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
            // Vùng 2: 16 tỉnh miền núi, Tây Nguyên & hải đảo (+2%)
            ProvinceZoneItem("Hà Giang", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Cao Bằng", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Bắc Kạn", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Lạng Sơn", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Tuyên Quang", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Lào Cai", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Yên Bái", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Điện Biên", 2, "Miền núi Tây Bắc (+2%)"),
            ProvinceZoneItem("Lai Châu", 2, "Miền núi Tây Bắc (+2%)"),
            ProvinceZoneItem("Sơn La", 2, "Miền núi Tây Bắc (+2%)"),
            ProvinceZoneItem("Hòa Bình", 2, "Miền núi phía Bắc (+2%)"),
            ProvinceZoneItem("Kon Tum", 2, "Tây Nguyên (+2%)"),
            ProvinceZoneItem("Gia Lai", 2, "Tây Nguyên (+2%)"),
            ProvinceZoneItem("Đắk Lắk", 2, "Tây Nguyên (+2%)"),
            ProvinceZoneItem("Đắk Nông", 2, "Tây Nguyên (+2%)"),
            ProvinceZoneItem("Lâm Đồng", 2, "Tây Nguyên (+2%)"),
            ProvinceZoneItem("Phú Quốc / Côn Đảo", 2, "Hải đảo xa xôi (+2%)"),

            // Vùng 1: Đô thị lớn & các tỉnh đồng bằng (Giá chuẩn)
            ProvinceZoneItem("Hà Nội", 1, "Thủ đô (Giá chuẩn)"),
            ProvinceZoneItem("TP. Hồ Chí Minh", 1, "Đô thị lớn (Giá chuẩn)"),
            ProvinceZoneItem("Đà Nẵng", 1, "Đô thị lớn (Giá chuẩn)"),
            ProvinceZoneItem("Hải Phòng", 1, "Thành phố cảng (Giá chuẩn)"),
            ProvinceZoneItem("Cần Thơ", 1, "Đồng bằng sông Cửu Long"),
            ProvinceZoneItem("Bình Dương", 1, "Gần kho đầu mối"),
            ProvinceZoneItem("Đồng Nai", 1, "Gần kho đầu mối"),
            ProvinceZoneItem("Bà Rịa - Vũng Tàu", 1, "Cảng biển xăng dầu"),
            ProvinceZoneItem("Quảng Ninh", 1, "Cảng biển tiếp nhận"),
            ProvinceZoneItem("Bắc Ninh", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Hải Dương", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Hưng Yên", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Thái Bình", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Nam Định", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Ninh Bình", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Hà Nam", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Vĩnh Phúc", 1, "Đồng bằng sông Hồng"),
            ProvinceZoneItem("Phú Thọ", 1, "Trung du Bắc Bộ"),
            ProvinceZoneItem("Thái Nguyên", 1, "Trung du Bắc Bộ"),
            ProvinceZoneItem("Bắc Giang", 1, "Trung du Bắc Bộ"),
            ProvinceZoneItem("Thanh Hóa", 1, "Bắc Trung Bộ"),
            ProvinceZoneItem("Nghệ An", 1, "Bắc Trung Bộ"),
            ProvinceZoneItem("Hà Tĩnh", 1, "Bắc Trung Bộ"),
            ProvinceZoneItem("Quảng Bình", 1, "Bắc Trung Bộ"),
            ProvinceZoneItem("Quảng Trị", 1, "Bắc Trung Bộ"),
            ProvinceZoneItem("Thừa Thiên Huế", 1, "Bắc Trung Bộ"),
            ProvinceZoneItem("Quảng Nam", 1, "Duyên hải miền Trung"),
            ProvinceZoneItem("Quảng Ngãi", 1, "Gần lọc dầu Dung Quất"),
            ProvinceZoneItem("Bình Định", 1, "Duyên hải miền Trung"),
            ProvinceZoneItem("Phú Yên", 1, "Duyên hải miền Trung"),
            ProvinceZoneItem("Khánh Hòa", 1, "Duyên hải miền Trung"),
            ProvinceZoneItem("Ninh Thuận", 1, "Duyên hải Nam Trung Bộ"),
            ProvinceZoneItem("Bình Thuận", 1, "Duyên hải Nam Trung Bộ"),
            ProvinceZoneItem("Tây Ninh", 1, "Đông Nam Bộ"),
            ProvinceZoneItem("Bình Phước", 1, "Đông Nam Bộ"),
            ProvinceZoneItem("Long An", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Tiền Giang", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Bến Tre", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Trà Vinh", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Vĩnh Long", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Đồng Tháp", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("An Giang", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Kiên Giang", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Hậu Giang", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Sóc Trăng", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Bạc Liêu", 1, "Tây Nam Bộ"),
            ProvinceZoneItem("Cà Mau", 1, "Tây Nam Bộ")
        )
    }

    val filteredProvinces = remember(searchQuery, allProvinces) {
        val q = removeAccents(searchQuery)
        if (q.isEmpty()) emptyList()
        else allProvinces.filter { removeAccents(it.name).contains(q) || removeAccents(it.description).contains(q) }
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
                        text = "Vùng 1 & Vùng 2 là gì?",
                        color = TextPrimaryDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quy định giá bán lẻ xăng dầu Petrolimex",
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
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Giải thích ngắn gọn
                Text(
                    text = "Do chi phí vận chuyển đường xa hiểm trở, Bộ Công Thương & Petrolimex chia giá bán lẻ làm 2 vùng:",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // THẺ VÙNG 1
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onZoneSelected(1, "Vùng 1 (Giá chuẩn)") },
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
                                text = "🟢 Vùng 1: Giá chuẩn (Thấp nhất)",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentZone == 1) {
                                Text(text = "✓ Đang chọn", color = PetrolTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hà Nội, TP.HCM, Đà Nẵng, Hải Phòng, Cần Thơ và hơn 40 tỉnh đồng bằng gần cảng biển & kho đầu mối.",
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
                        .clickable { onZoneSelected(2, "Vùng 2 (+2% cước)") },
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
                                text = "🟡 Vùng 2: Cao hơn tối đa 2%",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentZone == 2) {
                                Text(text = "✓ Đang chọn", color = EnergyAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "16 tỉnh miền núi phía Bắc (Hà Giang, Lạng Sơn, Sơn La...), Tây Nguyên (Đắk Lắk, Lâm Đồng...) và hải đảo xa xôi.",
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
                        Text("🔍 Nhập tỉnh của bạn (vd: Lạng Sơn, Hà Nội...)", color = TextSecondaryDark, fontSize = 12.sp)
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredProvinces.take(4).forEach { item ->
                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onZoneSelected(item.zone, "${item.name} (Vùng ${item.zone})")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.name, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "➔ Vùng ${item.zone}",
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
                        listOf("Hà Nội" to 1, "TP.HCM" to 1, "Lạng Sơn" to 2, "Đắk Lắk" to 2).forEach { (prov, z) ->
                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                modifier = Modifier.clickable {
                                    onZoneSelected(z, "$prov (Vùng $z)")
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
                Text("Xong", color = Color.Black, fontWeight = FontWeight.Bold)
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
