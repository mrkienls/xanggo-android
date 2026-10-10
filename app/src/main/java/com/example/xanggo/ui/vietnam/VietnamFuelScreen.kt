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
import com.example.xanggo.data.model.FuelItem
import com.example.xanggo.data.model.VietnamFuelData
import com.example.xanggo.theme.*
import com.example.xanggo.ui.components.PriceHistoryChartCard
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VietnamFuelScreen(
    data: VietnamFuelData,
    onNavigateToWorld: () -> Unit = {}
) {
    var selectedZone by remember { mutableIntStateOf(1) }
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

                // Nút chọn Vùng 1 / Vùng 2
                Surface(
                    color = PetrolTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        selectedZone = if (selectedZone == 1) 2 else 1
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vùng $selectedZone",
                            color = PetrolTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
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
