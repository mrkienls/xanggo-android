package com.example.xanggo.ui.world

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xanggo.data.model.CountryFuel
import com.example.xanggo.data.model.WorldFuelSummary
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.GlobeVector
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

@Composable
fun WorldCompareScreen(
    data: WorldFuelSummary
) {
    var selectedCurrency by remember { mutableStateOf("VND") } // "VND" hoặc "USD"
    var selectedTab by remember { mutableIntStateOf(0) } // 0: ASEAN, 1: Top Rẻ, 2: Toàn Cầu
    var searchQuery by remember { mutableStateOf("") } // Từ khóa tìm kiếm quốc gia
    var showInfoDialog by remember { mutableStateOf(false) } // Hộp thoại giải thích Sức mua
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }
    val listState = rememberLazyListState()

    val aseanCountries = remember(data.countries) {
        data.countries.filter { it.region == "ASEAN" }.sortedBy { it.priceUsd }
    }
    val topCheapCountries = remember(data.countries) {
        data.countries.sortedBy { it.priceUsd }.take(15)
    }
    val allCountries = remember(data.countries) {
        data.countries.sortedBy { it.priceUsd }
    }

    val currentTabList = when (selectedTab) {
        0 -> aseanCountries
        1 -> topCheapCountries
        else -> allCountries
    }

    // Lọc danh sách quốc gia theo từ khóa tìm kiếm (hỗ trợ tiếng Việt không dấu & có dấu)
    val filteredCountries = remember(searchQuery, currentTabList, allCountries, selectedTab) {
        val query = removeVietnameseAccents(searchQuery)
        if (query.isEmpty()) {
            currentTabList
        } else {
            val tabMatches = currentTabList.filter {
                removeVietnameseAccents(it.name).contains(query) ||
                it.code.lowercase().contains(query) ||
                removeVietnameseAccents(it.note).contains(query)
            }
            // Nếu tìm kiếm từ khóa không có trong tab hiện tại (ví dụ đang ở Đông Nam Á mà gõ "Mỹ", "Đức"),
            // tự động tìm trên toàn bộ 170 quốc gia để người dùng luôn tìm thấy kết quả!
            if (tabMatches.isEmpty() && selectedTab != 2) {
                allCountries.filter {
                    removeVietnameseAccents(it.name).contains(query) ||
                    it.code.lowercase().contains(query) ||
                    removeVietnameseAccents(it.note).contains(query)
                }
            } else {
                tabMatches
            }
        }
    }

    // Tự động cuộn lên đầu danh sách khi người dùng đổi Tab hoặc tìm kiếm
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(searchQuery) {
        listState.scrollToItem(0)
    }

    val maxPriceUsd = remember(filteredCountries) {
        filteredCountries.maxOfOrNull { it.priceUsd } ?: 3.0
    }

    val vnCountry = remember(data.countries) {
        data.countries.find { it.isCurrent || it.code == "VN" }
    }
    val vnPriceUsd = vnCountry?.priceUsd ?: 1.09
    val vnWorkMinutes = remember(vnCountry) {
        if (vnCountry != null && vnCountry.dailyIncomeUsd > 0) {
            (vnCountry.priceUsd / vnCountry.dailyIncomeUsd) * 480
        } else {
            41.0
        }
    }

    // Format ngày giờ sạch đẹp từ chuỗi ISO
    val displayDate = remember(data.updatedAt) {
        formatDisplayDate(data.updatedAt)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // -----------------------------------------------------------
        // 1. THANH ĐIỀU HƯỚNG & TÌM KIẾM CỐ ĐỊNH TRÊN CÙNG (Sticky Nav)
        // Khi người dùng cuộn danh sách, phần này vẫn luôn hiển thị rõ ràng!
        // -----------------------------------------------------------
        Surface(
            color = DarkBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hàng 1: Tiêu đề + Nút hướng dẫn + Đơn vị tiền tệ [ VND | USD ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PetrolTeal.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = GlobeVector,
                                    contentDescription = null,
                                    tint = PetrolTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Giá Xăng ",
                                color = TextPrimaryDark,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Thế Giới",
                                color = PetrolTeal,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        // Nút (?) giải thích sức mua
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Surface(
                                color = PetrolTeal.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = "💡", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Chuyển đổi tiền tệ [ VND ] | [ USD ]
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(2.5.dp)) {
                            CurrencyToggleChip(
                                label = "VND",
                                isSelected = selectedCurrency == "VND",
                                onClick = { selectedCurrency = "VND" }
                            )
                            CurrencyToggleChip(
                                label = "USD",
                                isSelected = selectedCurrency == "USD",
                                onClick = { selectedCurrency = "USD" }
                            )
                        }
                    }
                }

                // Hàng 2: Ô tìm kiếm quốc gia nhanh
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "🔍 Tìm nhanh 170 nước (vd: Mỹ, Nhật, Lào, Đức...)",
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

                // Hàng 3: Tab chuyển danh mục [ Đông Nam Á | Top Rẻ | Toàn Cầu ] CỐ ĐỊNH
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = PetrolTeal,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Đông Nam Á (${aseanCountries.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Top Rẻ Nhất",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "Toàn Cầu (${allCountries.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.4f), thickness = 1.dp)

        // -----------------------------------------------------------
        // 2. DANH SÁCH CUỘN DƯỚI THANH NAV CỐ ĐỊNH
        // -----------------------------------------------------------
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // 4. Highlight Thẻ Việt Nam (Chỉ hiện khi chưa tìm kiếm hoặc kết quả tìm kiếm có Việt Nam)
            if (searchQuery.isBlank() || filteredCountries.any { it.isCurrent }) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🇻🇳", fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Việt Nam: Hạng ${data.vnRank} / ${data.totalCountries} quốc gia",
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Giá xăng thuộc nhóm trung bình khu vực ASEAN",
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Thanh giải thích mini có thể bấm (Ẩn khi đang tìm kiếm)
            if (searchQuery.isBlank()) {
                item {
                    Surface(
                        color = PetrolTeal.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInfoDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⏱️", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Quy đổi: Thời gian làm việc / 1L",
                                    color = PetrolTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = PetrolTeal.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Tìm hiểu ▾",
                                    color = PetrolTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Khi đang tìm kiếm: hiển thị số lượng kết quả
            if (searchQuery.isNotBlank() && filteredCountries.isNotEmpty()) {
                item {
                    Text(
                        text = "Kết quả tìm kiếm: ${filteredCountries.size} quốc gia",
                        color = PetrolTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // 6. Trường hợp không tìm thấy quốc gia nào
            if (filteredCountries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Không tìm thấy \"$searchQuery\"",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hãy thử tìm bằng tiếng Việt không dấu (my, nhat, duc...) hoặc mã nước",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { searchQuery = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                            ) {
                                Text("Xem lại danh sách", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Table Column Header (Chỉ hiển thị khi có dữ liệu)
            if (filteredCountries.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = "Quốc gia",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (selectedCurrency == "VND") "Giá xăng (đ/L)" else "Giá xăng ($/L)",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "So với VN",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 7. Danh sách các quốc gia
            itemsIndexed(filteredCountries) { index, country ->
                CountryFuelRow(
                    index = index + 1,
                    country = country,
                    maxPriceUsd = maxPriceUsd,
                    vnPriceUsd = vnPriceUsd,
                    vnWorkMinutes = vnWorkMinutes,
                    currency = selectedCurrency,
                    currencyFormat = currencyFormat,
                    onInfoClick = { showInfoDialog = true }
                )
            }

            // 8. Bảng chú thích biểu đồ & ký hiệu (Legend Bar)
            if (filteredCountries.isNotEmpty()) {
                item {
                    Surface(
                        color = DarkSurface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PetrolTeal)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Rẻ hơn VN",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Cao hơn VN",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⏱️", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Số phút làm/L",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Tránh che banner đáy
            }
        }
    }

    // HỘP THOẠI GIẢI THÍCH CÔNG THỨC & SỨC MUA MINH BẠCH
    if (showInfoDialog) {
        val scrollState = rememberScrollState()
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📐", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Công thức tính số phút mua xăng",
                            color = TextPrimaryDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Phương pháp chuẩn quốc tế đo lường sức mua",
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
                        .heightIn(max = 480.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Chỉ nhìn giá tiền (đ/L) chưa đủ, vì 50.000đ ở nước phát triển rất nhẹ, nhưng ở nước nghèo lại là gánh nặng lớn. Vì vậy, chỉ số \"Số phút làm việc để mua 1L xăng\" phản ánh chính xác nhất mức độ rẻ/đắt đối với người dân bản địa.",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    // KHỐI CÔNG THỨC CHUẨN TOÁN HỌC
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚡ CÔNG THỨC TOÁN HỌC:",
                                color = PetrolTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Số phút làm việc = ( Giá 1L xăng ÷ Thu nhập 1 ngày ) × 480 phút",
                                color = TextPrimaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Quy chuẩn quốc tế: 1 ngày làm việc chuẩn = 8 tiếng = 480 phút.\n" +
                                       "• Hoặc: Thu nhập 1 phút = Thu nhập ngày ÷ 480.\n" +
                                       "  ➔ Số phút = Giá 1L xăng ÷ Thu nhập 1 phút.",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // VÍ DỤ TỪNG BƯỚC TẠI VIỆT NAM
                    Surface(
                        color = EnergyAmber.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EnergyAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🇻🇳 VÍ DỤ TÍNH THỰC TẾ (VIỆT NAM):",
                                color = EnergyAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Thu nhập bình quân: ~325.000 đ/ngày (~13 USD, lương ~7.8 triệu/tháng).\n" +
                                       "2. Thu nhập mỗi phút: 325.000 đ ÷ 480 phút = ~677 đ/phút.\n" +
                                       "3. Giá 1L xăng RON 95: ~28.000 đ/Lít.\n" +
                                       "➔ Thời gian làm việc: 28.000 đ ÷ 677 đ = ~41 phút làm việc/Lít.",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // SO SÁNH QUỐC TẾ
                    Text(
                        text = "📌 So sánh nhanh với các nước:",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• 🇸🇬 Singapore: Xăng ~55.000 đ/L, nhưng lương ~230 USD/ngày (11.980 đ/phút) ➔ Chỉ mất ~4.5 phút làm việc/Lít.\n\n" +
                               "• 🇲🇾 Malaysia: Nhờ chính phủ trợ giá xăng, người dân chỉ mất ~6 phút làm việc/Lít.\n\n" +
                               "• 🇱🇦 Lào: Xăng ~35.000 đ/L, thu nhập ~6 USD/ngày (312 đ/phút) ➔ Phải làm việc tới ~126 phút (~2 giờ 6 phút)/Lít.",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    // NGUỒN DỮ LIỆU MINH BẠCH
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🏛️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nguồn dữ liệu: Giá xăng từ GlobalPetrolPrices & Bộ Công Thương. Thu nhập bình quân từ Ngân hàng Thế giới (World Bank) & Tổ chức Lao động Quốc tế (ILO).",
                                color = TextSecondaryDark,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                ) {
                    Text("Đã hiểu rõ", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

fun formatDisplayDate(raw: String): String {
    return try {
        if (raw.contains("T")) {
            val datePart = raw.split("T")[0]
            val parts = datePart.split("-")
            if (parts.size == 3) {
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } else datePart
        } else raw
    } catch (e: Exception) {
        raw
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

@Composable
fun CurrencyToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) PetrolTeal else androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) androidx.compose.ui.graphics.Color.Black else TextSecondaryDark,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.5.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CountryFuelRow(
    index: Int,
    country: CountryFuel,
    maxPriceUsd: Double,
    vnPriceUsd: Double,
    vnWorkMinutes: Double,
    currency: String,
    currencyFormat: NumberFormat,
    onInfoClick: () -> Unit
) {
    val isVn = country.isCurrent

    // Tính thời gian làm việc để mua 1L xăng (1 ngày làm việc chuẩn = 8 giờ = 480 phút)
    val workingMinutes = if (country.dailyIncomeUsd > 0) {
        (country.priceUsd / country.dailyIncomeUsd) * 480
    } else {
        0.0
    }
    val diffMinutes = (workingMinutes - vnWorkMinutes).roundToInt()

    // Phụ đề dưới tên quốc gia
    val subtagText = when {
        isVn -> "Chuẩn đối chiếu Việt Nam"
        country.note.isNotBlank() -> country.note
        country.priceUsd < vnPriceUsd -> {
            val percentCheaper = ((1.0 - country.priceUsd / vnPriceUsd) * 100).roundToInt()
            "Rẻ hơn VN $percentCheaper%"
        }
        else -> {
            val percentHigher = ((country.priceUsd / vnPriceUsd - 1.0) * 100).roundToInt()
            "Cao hơn VN $percentHigher%"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVn) PetrolTeal.copy(alpha = 0.12f) else DarkSurface
        ),
        border = if (isVn) {
            androidx.compose.foundation.BorderStroke(1.5.dp, PetrolTeal)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.4f))
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bên trái: Số thứ tự + Cờ + Tên quốc gia + Subtag
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$index.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(text = country.flag, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = country.name,
                            color = if (isVn) PetrolTeal else TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = if (isVn) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtagText,
                            color = if (isVn) PetrolTeal.copy(alpha = 0.85f) else TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Bên phải: Giá tiền + Pill so sánh tương quan số phút
                Column(horizontalAlignment = Alignment.End) {
                    val priceText = if (currency == "USD") {
                        "$${country.priceUsd}/L"
                    } else {
                        "${currencyFormat.format(country.priceVnd)} đ/L"
                    }

                    Text(
                        text = priceText,
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Pill so với VN (Thời gian làm việc)
                    Surface(
                        color = when {
                            isVn -> PetrolTeal.copy(alpha = 0.2f)
                            diffMinutes < 0 -> PetrolTeal.copy(alpha = 0.15f)
                            diffMinutes > 0 -> Color(0xFF38BDF8).copy(alpha = 0.15f)
                            else -> DarkSurfaceVariant.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onInfoClick() }
                    ) {
                        val badgeText = when {
                            isVn -> "⏱ ~${workingMinutes.roundToInt()} ph/L"
                            diffMinutes < 0 -> "⏱ $diffMinutes ph/L"
                            diffMinutes > 0 -> "⏱ +$diffMinutes ph/L"
                            else -> "⏱ Bằng VN"
                        }
                        val badgeColor = when {
                            isVn -> PetrolTeal
                            diffMinutes < 0 -> PetrolTeal
                            diffMinutes > 0 -> Color(0xFF38BDF8)
                            else -> TextSecondaryDark
                        }

                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thanh tỉ lệ so sánh trực quan (Dual-tone: PetrolTeal nếu rẻ hơn hoặc bằng VN, Slate Blue nếu đắt hơn)
            val ratio = (country.priceUsd / maxPriceUsd).toFloat().coerceIn(0.04f, 1f)
            val barColor = if (country.priceUsd <= vnPriceUsd) PetrolTeal else Color(0xFF38BDF8)

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = DarkSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
