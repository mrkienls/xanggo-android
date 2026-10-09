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
            Text(
                text = "So Sánh Giá Xăng Thế Giới",
                color = TextPrimaryDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cập nhật ${data.updatedAt} • Dữ liệu 168 quốc gia",
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
                    Column {
                        Text(
                            text = "Việt Nam: Hạng ${data.vnRank} / ${data.totalCountries} thế giới",
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Giá xăng thuộc nhóm rẻ hơn trung bình thế giới",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 5. Danh sách các quốc gia
        itemsIndexed(filteredCountries) { index, country ->
            CountryFuelRow(
                index = index + 1,
                country = country,
                maxPriceUsd = maxPriceUsd,
                currency = selectedCurrency,
                currencyFormat = currencyFormat
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Tránh che banner đáy
        }
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
    currencyFormat: NumberFormat
) {
    val isVn = country.isCurrent

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

                Spacer(modifier = Modifier.width(12.dp))

                // Chỉ số Sức mua (Affordability Tag)
                Surface(
                    color = DarkSurfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Sức mua: ${country.affordabilityPercent}% thu nhập/L",
                        color = if (country.affordabilityPercent > 10.0) PriceRed else TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
