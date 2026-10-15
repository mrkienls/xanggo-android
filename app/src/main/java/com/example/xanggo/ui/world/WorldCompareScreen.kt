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
import androidx.compose.ui.platform.LocalContext
import com.example.xanggo.data.model.CountryFuel
import com.example.xanggo.data.model.WorldFuelSummary
import com.example.xanggo.i18n.AppLang
import com.example.xanggo.i18n.LocalAppLanguage
import com.example.xanggo.i18n.tr
import com.example.xanggo.i18n.translateCountryName
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.GlobeVector
import com.example.xanggo.util.ShareCardGenerator
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

@Composable
fun WorldCompareScreen(
    data: WorldFuelSummary
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
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

    // Lọc danh sách quốc gia theo từ khóa tìm kiếm (hỗ trợ tiếng Việt & tiếng Anh)
    val filteredCountries = remember(searchQuery, currentTabList, allCountries, selectedTab) {
        val query = removeVietnameseAccents(searchQuery)
        if (query.isEmpty()) {
            currentTabList
        } else {
            val matchesFn: (CountryFuel) -> Boolean = {
                removeVietnameseAccents(it.name).contains(query) ||
                removeVietnameseAccents(translateCountryName(it.name, it.code, AppLang.EN)).contains(query) ||
                it.code.lowercase().contains(query) ||
                removeVietnameseAccents(it.note).contains(query)
            }
            val tabMatches = currentTabList.filter(matchesFn)
            if (tabMatches.isEmpty() && selectedTab != 2) {
                allCountries.filter(matchesFn)
            } else {
                tabMatches
            }
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. THANH ĐIỀU HƯỚNG & TÌM KIẾM CỐ ĐỊNH TRÊN CÙNG (Sticky Nav)
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
                // Hàng 1: Tiêu đề + Nút (?) + Nút Chia sẻ Infographic + [ VND | USD ]
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

                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                append(tr("Xăng ", "World "))
                                pushStyle(androidx.compose.ui.text.SpanStyle(color = PetrolTeal))
                                append(tr("Thế Giới", "Fuel"))
                                pop()
                            },
                            color = TextPrimaryDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Nút Chia sẻ Infographic ASEAN 1 chạm
                        Surface(
                            color = PetrolTeal.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.45f)),
                            modifier = Modifier.clickable {
                                ShareCardGenerator.shareWorldCompareCard(
                                    context = context,
                                    data = data,
                                    lang = lang
                                )
                            }
                        ) {
                            Text(
                                text = tr("📤 Chia sẻ", "📤 Share"),
                                color = PetrolTeal,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }

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
                }

                // Hàng 2: Ô tìm kiếm quốc gia nhanh
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = tr("🔍 Tìm nước: Mỹ, Nhật, Lào…", "🔍 Search country: USA, Japan, Laos…"),
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                // Hàng 3: Tab chuyển danh mục [ ASEAN | Rẻ nhất | Tất cả ]
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = PetrolTeal,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    listOf(
                        "ASEAN (${aseanCountries.size})",
                        tr("Rẻ nhất", "Cheapest"),
                        tr("Tất cả (${allCountries.size})", "All (${allCountries.size})")
                    ).forEachIndexed { i, label ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.4f), thickness = 1.dp)

        // 2. DANH SÁCH CUỘN DƯỚI THANH NAV CỐ ĐỊNH
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

            // Highlight Thẻ Việt Nam
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
                                    text = tr(
                                        "Việt Nam: Hạng ${data.vnRank} / ${data.totalCountries} quốc gia",
                                        "Vietnam: Rank ${data.vnRank} / ${data.totalCountries} countries"
                                    ),
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        "Giá xăng thuộc nhóm trung bình khu vực ASEAN",
                                        "Mid-range fuel price in the ASEAN region"
                                    ),
                                    color = TextSecondaryDark,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Thanh giải thích mini có thể bấm
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
                                    text = tr("Phút lao động / 1 lít", "Work minutes / 1 Liter"),
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
                                    text = tr("Tìm hiểu ▾", "How it works ▾"),
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

            if (searchQuery.isNotBlank() && filteredCountries.isNotEmpty()) {
                item {
                    Text(
                        text = tr(
                            "Kết quả tìm kiếm: ${filteredCountries.size} quốc gia",
                            "Search results: ${filteredCountries.size} countries"
                        ),
                        color = PetrolTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

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
                                text = tr("Không tìm thấy \"$searchQuery\"", "No country found for \"$searchQuery\""),
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tr(
                                    "Hãy thử tìm bằng tiếng Việt không dấu (my, nhat, duc...) hoặc mã nước",
                                    "Try searching by country name or ISO code (US, JP, SG, TH...)"
                                ),
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { searchQuery = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                            ) {
                                Text(tr("Xem lại danh sách", "Reset search"), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Table Column Header
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
                            text = tr("Quốc gia", "Country"),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (selectedCurrency == "VND") tr("Giá xăng (đ/L)", "Price (VND/L)") else tr("Giá xăng ($/L)", "Price ($/L)"),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = tr("So với VN", "vs VN"),
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Danh sách các quốc gia
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

            // Legend Bar
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
                                    text = tr("Rẻ hơn VN", "Cheaper than VN"),
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
                                    text = tr("Cao hơn VN", "Higher than VN"),
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⏱️", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr("Số phút làm/L", "Work mins/L"),
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // HỘP THOẠI GIẢI THÍCH CÔNG THỨC & SỨC MUA
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
                            text = tr("Công thức tính số phút mua xăng", "Fuel Affordability Formula"),
                            color = TextPrimaryDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr("Phương pháp chuẩn quốc tế đo lường sức mua", "International purchasing power metric"),
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
                        text = tr(
                            "Chỉ nhìn giá tiền (đ/L) chưa đủ, vì 50.000đ ở nước phát triển rất nhẹ, nhưng ở nước đang phát triển lại đáng kể. Vì vậy, chỉ số \"Số phút làm việc để mua 1L xăng\" phản ánh chính xác nhất mức độ rẻ/đắt đối với người dân bản địa.",
                            "Raw price per liter doesn't tell the full story because wages vary widely across countries. \"Work minutes needed to buy 1L of fuel\" measures real affordability for local residents."
                        ),
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tr("⚡ CÔNG THỨC TOÁN HỌC:", "⚡ FORMULA:"),
                                color = PetrolTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tr(
                                    "Số phút làm việc = ( Giá 1L xăng ÷ Thu nhập 1 ngày ) × 480 phút",
                                    "Work Minutes = ( Price of 1L Fuel ÷ Average Daily Income ) × 480 mins"
                                ),
                                color = TextPrimaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tr(
                                    "• Quy chuẩn quốc tế: 1 ngày làm việc chuẩn = 8 tiếng = 480 phút.\n• Hoặc: Thu nhập 1 phút = Thu nhập ngày ÷ 480.\n  ➔ Số phút = Giá 1L xăng ÷ Thu nhập 1 phút.",
                                    "• Standard workday = 8 hours = 480 minutes.\n• Minute wage = Daily income ÷ 480.\n  ➔ Work minutes = 1L fuel price ÷ Minute wage."
                                ),
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Surface(
                        color = EnergyAmber.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EnergyAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tr("🇻🇳 VÍ DỤ TÍNH THỰC TẾ (VIỆT NAM):", "🇻🇳 REAL EXAMPLE (VIETNAM):"),
                                color = EnergyAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tr(
                                    "1. Thu nhập bình quân: ~325.000 đ/ngày (~13 USD).\n2. Thu nhập mỗi phút: 325.000 đ ÷ 480 phút = ~677 đ/phút.\n3. Giá 1L xăng RON 95: ~28.000 đ/Lít.\n➔ Thời gian làm việc: 28.000 đ ÷ 677 đ = ~41 phút làm việc/Lít.",
                                    "1. Average daily income: ~325,000 VND/day (~$13 USD).\n2. Income per minute: 325,000 ÷ 480 = ~677 VND/min.\n3. 1L RON 95 price: ~28,000 VND/L.\n➔ Work time: 28,000 ÷ 677 = ~41 mins of work per Liter."
                                ),
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

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
                                text = tr(
                                    "Nguồn dữ liệu: Giá xăng từ GlobalPetrolPrices & Bộ Công Thương. Thu nhập bình quân từ Ngân hàng Thế giới (World Bank) & ILO.",
                                    "Sources: Fuel prices from GlobalPetrolPrices & MOIT Vietnam. Income data from World Bank & ILO."
                                ),
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
                    Text(tr("Đã hiểu rõ", "Got it"), color = Color.Black, fontWeight = FontWeight.Bold)
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
        color = if (isSelected) PetrolTeal else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextSecondaryDark,
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
    val lang = LocalAppLanguage.current
    val isVn = country.isCurrent

    val workingMinutes = if (country.dailyIncomeUsd > 0) {
        (country.priceUsd / country.dailyIncomeUsd) * 480
    } else {
        0.0
    }
    val diffMinutes = (workingMinutes - vnWorkMinutes).roundToInt()

    val subtagText = when {
        isVn -> tr("Chuẩn đối chiếu Việt Nam", "Vietnam Benchmark")
        country.note.isNotBlank() && lang == AppLang.VI -> country.note
        country.priceUsd < vnPriceUsd -> {
            val percentCheaper = ((1.0 - country.priceUsd / vnPriceUsd) * 100).roundToInt()
            tr("Rẻ hơn VN $percentCheaper%", "$percentCheaper% cheaper than VN")
        }
        else -> {
            val percentHigher = ((country.priceUsd / vnPriceUsd - 1.0) * 100).roundToInt()
            tr("Cao hơn VN $percentHigher%", "$percentHigher% higher than VN")
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
                            text = translateCountryName(country.name, country.code, lang),
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

                Column(horizontalAlignment = Alignment.End) {
                    val priceText = if (currency == "USD") {
                        "$${country.priceUsd}/L"
                    } else {
                        "${currencyFormat.format(country.priceVnd)} ${tr("đ/L", "VND/L")}"
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
                        val unitMin = tr("ph/L", "m/L")
                        val badgeText = when {
                            isVn -> "⏱ ~${workingMinutes.roundToInt()} $unitMin"
                            diffMinutes < 0 -> "⏱ $diffMinutes $unitMin"
                            diffMinutes > 0 -> "⏱ +$diffMinutes $unitMin"
                            else -> tr("⏱ Bằng VN", "⏱ Same as VN")
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
