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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header: Logo & Region Switcher
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⛽", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "XăngGo",
                        color = TextPrimaryDark,
                        fontSize = 22.sp,
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

            // Dòng thời gian cập nhật tinh gọn & chuyên nghiệp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🕒 Cập nhật: ${data.updatedAt}",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Text(
                    text = "Nguồn: Petrolimex",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        // 2. Hero Card: RON 95-III (Nhiều người dùng nhất)
        item {
            val heroItem = data.items.firstOrNull { it.isFeatured } ?: data.items.first()
            val currentPrice = heroItem.getPrice(selectedZone)
            val previousPrice = currentPrice - heroItem.delta

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = heroItem.name,
                            color = TextSecondaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Badge Tăng / Giảm giá
                        DeltaPriceBadge(delta = heroItem.delta)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currencyFormat.format(currentPrice),
                            color = TextPrimaryDark,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = " đ/lít",
                            color = TextSecondaryDark,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 5.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Chống rớt dòng mồ côi chữ đ
                    Text(
                        text = "Kỳ trước: ${currencyFormat.format(previousPrice)}\u00A0đ  •  Vùng ${if (selectedZone == 1) "2: " + currencyFormat.format(heroItem.zone2Price) else "1: " + currencyFormat.format(heroItem.zone1Price)}\u00A0đ",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
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
                        // Khi còn lẻ 1 món (ví dụ Dầu DO): Trải rộng thanh lịch toàn hàng ngang, không để trống bên phải
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
                                text = "So sánh giá ASEAN",
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Nút "Xem tất cả →" (Chống rớt dòng dọc triệt để)
                        Surface(
                            color = PetrolTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Xem tất cả →",
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

                    // Bảng 3 cột trực quan & chuyên nghiệp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "🇻🇳 Việt Nam", color = TextSecondaryDark, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "28.250 đ", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🇲🇾 Malaysia", color = TextSecondaryDark, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "11.380 đ", color = PriceGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "🇹🇭 Thái Lan", color = TextSecondaryDark, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "29.750 đ", color = PriceRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(110.dp)) // Tránh che banner đáy
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
fun FuelWideCard(
    item: FuelItem,
    zone: Int,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
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
                    text = item.name,
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                DeltaPriceBadge(delta = item.delta, isCompact = true)
            }

            Text(
                text = "${currencyFormat.format(item.getPrice(zone))} đ/lít",
                color = TextPrimaryDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
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
                        text = "Vùng 1 & Vùng 2 là gì?",
                        color = TextPrimaryDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cơ cấu 34 tỉnh thành & quy định giá Petrolimex",
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
                    text = "Bộ Công Thương & Petrolimex phân chia 34 tỉnh thành thành 2 vùng giá theo chi phí vận chuyển:",
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
                            text = "21 tỉnh & TP đồng bằng, ven biển gần cảng/tổng kho (Hà Nội, TP.HCM, Hải Phòng, Đà Nẵng, Cần Thơ, Bắc Ninh...).",
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
                            text = "13 tỉnh miền núi, Tây Nguyên (Lạng Sơn, Cao Bằng, Tuyên Quang, Lào Cai, Sơn La, Lâm Đồng, Đắk Lắk...) & tất cả hải đảo.",
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
                        Text("🔍 Nhập tỉnh của bạn (vd: Lạng Sơn, Hà Giang, TP.HCM...)", color = TextSecondaryDark, fontSize = 12.sp)
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
                                        onZoneSelected(item.zone, "${item.name} (Vùng ${item.zone})")
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
                        listOf("Hà Nội" to 1, "TP.HCM" to 1, "Lạng Sơn" to 2, "Đà Nẵng" to 1, "Lâm Đồng" to 2).forEach { (prov, z) ->
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
