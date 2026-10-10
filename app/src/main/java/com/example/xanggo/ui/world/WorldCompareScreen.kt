package com.example.xanggo.ui.world

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xanggo.data.model.CountryFuel
import com.example.xanggo.data.model.WorldFuelSummary
import com.example.xanggo.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WorldCompareScreen(
    data: WorldFuelSummary
) {
    var selectedCurrency by remember { mutableStateOf("VND") } // "VND" hoặc "USD"
    var selectedTab by remember { mutableIntStateOf(0) } // 0: ASEAN, 1: Top Rẻ, 2: Toàn Cầu
    var showInfoDialog by remember { mutableStateOf(false) } // Hộp thoại giải thích Sức mua
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    val filteredCountries = remember(selectedTab, data.countries) {
        when (selectedTab) {
            0 -> data.countries.filter { it.region == "ASEAN" }.sortedBy { it.priceUsd }
            1 -> data.countries.sortedBy { it.priceUsd }.take(7)
            else -> data.countries.sortedBy { it.priceUsd }
        }
    }

    val maxPriceUsd = remember(filteredCountries) {
        filteredCountries.maxOfOrNull { it.priceUsd } ?: 3.0
    }

    // Format ngày giờ sạch đẹp từ chuỗi ISO
    val displayDate = remember(data.updatedAt) {
        formatDisplayDate(data.updatedAt)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "So Sánh Giá Xăng Thế Giới",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                // Nút (?) giải thích sức mua
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        color = PetrolTeal.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "💡", fontSize = 16.sp)
                        }
                    }
                }
            }

            Text(
                text = "Cập nhật hôm nay • $displayDate • Dữ liệu 168 quốc gia",
                color = TextSecondaryDark,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // 2. Chuyển đổi tiền tệ [ VND ] | [ USD ]
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đơn vị tiền tệ:",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )

                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
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

        // 3. Tab chuyển danh mục [ ASEAN | Top Rẻ | Toàn Cầu ]
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = PetrolTeal,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Đông Nam Á (ASEAN)", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Top Rẻ Nhất", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Toàn Cầu", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // 4. Highlight Thẻ Việt Nam
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

        // 5. Thanh giải thích mini có thể bấm
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏱️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quy đổi: Thời gian làm việc để mua 1L xăng",
                            color = PetrolTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(text = "Tìm hiểu ▾", color = PetrolTeal, fontSize = 11.sp)
                }
            }
        }

        // 6. Danh sách các quốc gia
        itemsIndexed(filteredCountries) { index, country ->
            CountryFuelRow(
                index = index + 1,
                country = country,
                maxPriceUsd = maxPriceUsd,
                currency = selectedCurrency,
                currencyFormat = currencyFormat,
                onInfoClick = { showInfoDialog = true }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Tránh che banner đáy
        }
    }

    // HỘP THOẠI GIẢI THÍCH SỨC MUA TRỰC QUAN
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💡", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thời gian làm việc mua 1L xăng là gì?",
                        color = TextPrimaryDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Chỉ số này so sánh xem người dân mỗi nước phải làm việc bao nhiêu phút (dựa trên mức lương bình quân 8 giờ/ngày) để đổ được 1 lít xăng.",
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = DarkSurfaceVariant)

                    Text(
                        text = "📌 Ví dụ thực tế dễ hiểu:",
                        color = EnergyAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "• 🇸🇬 Singapore: Xăng ~55.000 đ/L (nghe rất đắt), nhưng lương họ cao nên chỉ cần làm việc ~4 phút là mua được 1 lít!\n\n" +
                               "• 🇲🇾 Malaysia: Nhờ chính phủ trợ giá xăng, người dân chỉ mất ~6 phút làm việc/Lít.\n\n" +
                               "• 🇻🇳 Việt Nam: Xăng ~28.000 đ/L, người dân cần làm việc ~41 phút để mua 1 lít.\n\n" +
                               "• 🇱🇦 Lào: Xăng ~35.000 đ/L, nhưng người dân phải làm việc tới ~2 giờ 5 phút mới mua được 1 lít.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Surface(
                        color = PetrolTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "👉 Thời gian làm việc càng ít nghĩa là xăng càng rẻ và nhẹ gánh đối với người dân nước đó.",
                            color = PetrolTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolTeal)
                ) {
                    Text("Đã hiểu", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CountryFuelRow(
    index: Int,
    country: CountryFuel,
    maxPriceUsd: Double,
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

    val workingTimeDisplay = when {
        workingMinutes < 1.0 -> "~1 phút"
        workingMinutes < 60.0 -> "~${workingMinutes.toInt()} phút"
        else -> {
            val hours = (workingMinutes / 60).toInt()
            val mins = (workingMinutes % 60).toInt()
            if (mins > 0) "~${hours}h ${mins}p" else "~${hours}h"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVn) PetrolTeal.copy(alpha = 0.12f) else DarkSurface
        ),
        border = if (isVn) androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.6f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$index.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(text = country.flag, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = country.name,
                        color = if (isVn) PetrolTeal else TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = if (isVn) FontWeight.Bold else FontWeight.SemiBold
                    )
                }

                // Con số giá tiền
                val priceText = if (currency == "USD") {
                    "$${country.priceUsd}/L"
                } else {
                    "${currencyFormat.format(country.priceVnd)} đ/L"
                }

                Text(
                    text = priceText,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thanh tỉ lệ so sánh trực quan
            val ratio = (country.priceUsd / maxPriceUsd).toFloat().coerceIn(0.05f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isVn) PetrolTeal else EnergyAmber,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Tag Thời Gian Làm Việc Để Mua 1L Xăng (Dễ hiểu ngay)
                Surface(
                    color = if (isVn) PetrolTeal.copy(alpha = 0.2f) else DarkSurfaceVariant.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onInfoClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ $workingTimeDisplay/L",
                            color = if (isVn) PetrolTeal else if (workingMinutes > 60.0) PriceRed else TextPrimaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
