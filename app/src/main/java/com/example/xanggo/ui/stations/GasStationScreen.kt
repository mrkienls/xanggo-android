package com.example.xanggo.ui.stations

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.GasPumpVector
import com.example.xanggo.ui.components.LocationPinVector
import java.text.Normalizer
import java.util.regex.Pattern

data class StationInfo(
    val id: String,
    val name: String,
    val brand: String,
    val brandColor: Color,
    val brandLogo: String,
    val address: String,
    val distance: String,
    val eta: String,
    val isOpen: Boolean = true,
    val openHours: String = "24/7",
    val fuels: List<String>,
    val highlightTag: String = "",
    val searchKeyword: String
)

@Composable
fun GasStationScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrandFilter by remember { mutableStateOf("ALL") } // "ALL", "PETROLIMEX", "PVOIL", "SAIGON_PETRO", "IDEMITSU"

    val stations = remember {
        listOf(
            StationInfo(
                id = "1",
                name = "Petrolimex - Cửa Hàng Số 01",
                brand = "Petrolimex",
                brandColor = Color(0xFFF97316),
                brandLogo = "🟠",
                address = "Trục đường chính gần bạn nhất",
                distance = "0.8 km",
                eta = "3 phút",
                isOpen = true,
                openHours = "Mở cửa 24/7",
                fuels = listOf("RON 95-V", "E5 RON 92", "Dầu DO 0.001S"),
                highlightTag = "Chính hãng",
                searchKeyword = "cây xăng Petrolimex gần đây"
            ),
            StationInfo(
                id = "2",
                name = "PVOIL - Cửa Hàng Xăng Dầu Số 15",
                brand = "PVOIL",
                brandColor = Color(0xFF38BDF8),
                brandLogo = "🔵",
                address = "Khu vực trung tâm đô thị",
                distance = "1.5 km",
                eta = "5 phút",
                isOpen = true,
                openHours = "Mở cửa 24/7",
                fuels = listOf("RON 95-III", "E5 RON 92", "Dầu DO 0.05S"),
                highlightTag = "PVOIL Easy",
                searchKeyword = "cây xăng PVOIL gần đây"
            ),
            StationInfo(
                id = "3",
                name = "Saigon Petro - Trạm Xăng Dầu 08",
                brand = "Saigon Petro",
                brandColor = Color(0xFF10B981),
                brandLogo = "🟢",
                address = "Tuyến giao lộ vành đai",
                distance = "2.2 km",
                eta = "7 phút",
                isOpen = true,
                openHours = "05:00 - 23:00",
                fuels = listOf("RON 95-III", "E5 RON 92", "Dầu DO"),
                highlightTag = "Phổ biến",
                searchKeyword = "cây xăng Saigon Petro gần đây"
            ),
            StationInfo(
                id = "4",
                name = "Idemitsu Q8 - Trạm Xăng Chuẩn Nhật",
                brand = "Idemitsu",
                brandColor = Color(0xFFEF4444),
                brandLogo = "🔴",
                address = "Cửa ngõ khu công nghệ",
                distance = "3.4 km",
                eta = "10 phút",
                isOpen = true,
                openHours = "Mở cửa 24/7",
                fuels = listOf("RON 95-V Euro 5", "Dầu DO 0.001S"),
                highlightTag = "Dịch vụ 5 sao",
                searchKeyword = "cây xăng Idemitsu gần đây"
            )
        )
    }

    val filteredStations = remember(searchQuery, selectedBrandFilter) {
        val query = removeVietnameseAccents(searchQuery)
        stations.filter { station ->
            val matchBrand = when (selectedBrandFilter) {
                "PETROLIMEX" -> station.brand == "Petrolimex"
                "PVOIL" -> station.brand == "PVOIL"
                "SAIGON_PETRO" -> station.brand == "Saigon Petro"
                "IDEMITSU" -> station.brand == "Idemitsu"
                else -> true
            }
            val matchQuery = query.isEmpty() ||
                removeVietnameseAccents(station.name).contains(query) ||
                removeVietnameseAccents(station.brand).contains(query) ||
                removeVietnameseAccents(station.address).contains(query)
            matchBrand && matchQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. Header: Biểu tượng Vị trí & Trạng thái GPS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        PetrolTeal.copy(alpha = 0.25f),
                                        PetrolTeal.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LocationPinVector,
                            contentDescription = null,
                            tint = PetrolTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tìm Trạm ",
                                color = TextPrimaryDark,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Xăng",
                                color = PetrolTeal,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Định vị trạm chính hãng quanh bạn",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                // Huy hiệu GPS Sẵn sàng
                Surface(
                    color = PriceGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PriceGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(PriceGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GPS Sẵn sàng",
                            color = PriceGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2. Ô tìm kiếm trạm xăng nhanh
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "🔍 Tìm theo quận, đường, tên cây xăng...",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", color = TextSecondaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = PetrolTeal,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    cursorColor = PetrolTeal
                )
            )
        }

        // 3. THẺ HERO RADAR LỚN (Theo phong cách Mockup bạn cung cấp)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PetrolTeal.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header thẻ: Icon to + Tiêu đề + Nút "Xem bản đồ >"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
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
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tìm Trạm Xăng Gần Bạn",
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Xem giá, khoảng cách và lộ trình đến các trạm xăng trong khu vực.",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Nút "Xem bản đồ >"
                        Button(
                            onClick = { openMapsSearchDirect(context, "cây xăng gần đây") },
                            colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🗺️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Xem bản đồ ›",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Lưới 4 thương hiệu lớn tại Việt Nam (Petrolimex, PVOIL, Saigon Petro, Idemitsu Q8)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrandOptionCard(
                            name = "Petrolimex",
                            subTag = "Phổ biến",
                            logoEmoji = "🟠",
                            accentColor = Color(0xFFF97316),
                            isSelected = selectedBrandFilter == "PETROLIMEX",
                            onClick = {
                                selectedBrandFilter = if (selectedBrandFilter == "PETROLIMEX") "ALL" else "PETROLIMEX"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BrandOptionCard(
                            name = "PVOIL",
                            subTag = "Phổ biến",
                            logoEmoji = "🔵",
                            accentColor = Color(0xFF38BDF8),
                            isSelected = selectedBrandFilter == "PVOIL",
                            onClick = {
                                selectedBrandFilter = if (selectedBrandFilter == "PVOIL") "ALL" else "PVOIL"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BrandOptionCard(
                            name = "Saigon Petro",
                            subTag = "Miền Nam",
                            logoEmoji = "🟢",
                            accentColor = Color(0xFF10B981),
                            isSelected = selectedBrandFilter == "SAIGON_PETRO",
                            onClick = {
                                selectedBrandFilter = if (selectedBrandFilter == "SAIGON_PETRO") "ALL" else "SAIGON_PETRO"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        BrandOptionCard(
                            name = "Idemitsu",
                            subTag = "Chuẩn Nhật",
                            logoEmoji = "🔴",
                            accentColor = Color(0xFFEF4444),
                            isSelected = selectedBrandFilter == "IDEMITSU",
                            onClick = {
                                selectedBrandFilter = if (selectedBrandFilter == "IDEMITSU") "ALL" else "IDEMITSU"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // KHỐI MINI RADAR PREVIEW & CHỈ ĐƯỜNG TRỰC TIẾP
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openMapsSearchDirect(context, "cây xăng gần đây nhất") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Canvas mô phỏng bản đồ Radar với điểm định vị xanh phát sáng
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    // Đường phố mô phỏng
                                    drawLine(
                                        color = Color(0xFF1E293B),
                                        start = Offset(0f, h * 0.4f),
                                        end = Offset(w, h * 0.5f),
                                        strokeWidth = 3f
                                    )
                                    drawLine(
                                        color = Color(0xFF1E293B),
                                        start = Offset(w * 0.35f, 0f),
                                        end = Offset(w * 0.4f, h),
                                        strokeWidth = 3f
                                    )
                                    drawLine(
                                        color = Color(0xFF1E293B),
                                        start = Offset(w * 0.7f, 0f),
                                        end = Offset(w * 0.65f, h),
                                        strokeWidth = 2f
                                    )

                                    // Vòng hào quang Radar phát sáng
                                    drawCircle(
                                        color = PetrolTeal.copy(alpha = 0.25f),
                                        radius = 20f,
                                        center = Offset(w * 0.48f, h * 0.52f)
                                    )
                                    // Điểm người dùng (User Location Dot)
                                    drawCircle(
                                        color = Color(0xFF38BDF8),
                                        radius = 7f,
                                        center = Offset(w * 0.48f, h * 0.52f)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 3f,
                                        center = Offset(w * 0.48f, h * 0.52f)
                                    )

                                    // Điểm cây xăng lân cận
                                    drawCircle(color = PetrolTeal, radius = 5f, center = Offset(w * 0.22f, h * 0.3f))
                                    drawCircle(color = PetrolTeal, radius = 5f, center = Offset(w * 0.78f, h * 0.35f))
                                    drawCircle(color = PetrolTeal, radius = 5f, center = Offset(w * 0.82f, h * 0.72f))
                                }
                                Text(
                                    text = "GPS LIVE",
                                    color = PetrolTeal.copy(alpha = 0.8f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Thông tin cây xăng gần nhất bên phải
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = LocationPinVector,
                                        contentDescription = null,
                                        tint = PetrolTeal,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Gần bạn nhất: 0.8 km • 3 phút",
                                        color = TextPrimaryDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🧭", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Chỉ đường: Xem lộ trình trên Maps",
                                        color = PetrolTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "›",
                                color = PetrolTeal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. DANH SÁCH TRẠM XĂNG NỔI BẬT
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trạm xăng gần bạn (${filteredStations.size})",
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Mở tất cả trên Maps →",
                    color = PetrolTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        openMapsSearchDirect(context, "cây xăng gần đây")
                    }
                )
            }
        }

        // Lặp qua các trạm xăng
        items(filteredStations) { station ->
            StationDetailCard(
                station = station,
                onDirectClick = {
                    openMapsSearchDirect(context, station.searchKeyword)
                }
            )
        }

        // 5. Thẻ Mẹo Đổ Xăng Tiết Kiệm & An Toàn
        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mẹo đổ xăng thông minh từ XăngGo",
                            color = EnergyAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Luôn ưu tiên trạm xăng chính hãng (Petrolimex, PVOIL, Saigon Petro) để đảm bảo chuẩn chất lượng và đo đúng dung tích.\n" +
                               "• Đổ xăng vào sáng sớm hoặc tối muộn khi nhiệt độ mát giúp xăng đậm đặc nhất, tránh bay hơi nhiệt.",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp)) // Tránh che bottom navigation bar
        }
    }
}

@Composable
fun BrandOptionCard(
    name: String,
    subTag: String,
    logoEmoji: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else DarkBackground,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accentColor else DarkSurfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = logoEmoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                color = if (isSelected) accentColor else TextPrimaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = subTag,
                    color = TextSecondaryDark,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun StationDetailCard(
    station: StationInfo,
    onDirectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Hàng 1: Logo hãng + Tên trạm + Khoảng cách pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = station.brandLogo, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = station.name,
                            color = TextPrimaryDark,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = station.address,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Khoảng cách
                Surface(
                    color = PetrolTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = LocationPinVector,
                            contentDescription = null,
                            tint = PetrolTeal,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = station.distance,
                            color = PetrolTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hàng 2: Trạng thái mở cửa + Danh sách loại xăng có sẵn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PriceGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = station.openHours,
                        color = PriceGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Nút chỉ đường 1 chạm
                Surface(
                    color = PetrolTeal,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onDirectClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🧭", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Chỉ đường (${station.eta})",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hàng 3: Chips các loại xăng có bán tại trạm
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                station.fuels.forEach { fuel ->
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = fuel,
                            color = TextSecondaryDark,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

fun openMapsSearchDirect(context: Context, query: String) {
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

fun removeVietnameseAccents(str: String): String {
    return try {
        val nfd = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        pattern.matcher(nfd).replaceAll("").replace("đ", "d").replace("Đ", "D").lowercase().trim()
    } catch (e: Exception) {
        str.lowercase().trim()
    }
}
