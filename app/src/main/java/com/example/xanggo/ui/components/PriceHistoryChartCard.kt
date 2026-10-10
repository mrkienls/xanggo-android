package com.example.xanggo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xanggo.data.model.PriceHistoryPoint
import com.example.xanggo.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun PriceHistoryChartCard(
    history: List<PriceHistoryPoint>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    // 0: RON 95-III, 1: E5 RON 92, 2: Dầu DO
    var selectedFuelType by remember { mutableIntStateOf(0) }
    // Vị trí điểm đang được người dùng chạm/kéo ngón tay
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.GERMANY) }

    // Lấy danh sách giá theo loại nhiên liệu được chọn
    val pricePoints = remember(history, selectedFuelType) {
        history.map { point ->
            when (selectedFuelType) {
                0 -> point.ron95Price
                1 -> point.e5Price
                else -> point.dieselPrice
            }
        }
    }

    val activeIndex = selectedPointIndex ?: (history.size - 1)
    val activeHistoryPoint = history.getOrNull(activeIndex) ?: history.last()
    val activePrice = pricePoints.getOrNull(activeIndex) ?: pricePoints.last()

    // Tính mức biến động của điểm đang chọn so với điểm trước đó
    val prevPrice = if (activeIndex > 0) pricePoints[activeIndex - 1] else activePrice
    val pointDelta = activePrice - prevPrice

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header Card: Tiêu đề + Chọn loại xăng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📈", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Lịch Sử Biến Động Giá",
                            color = TextPrimaryDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${history.size} kỳ điều hành gần nhất",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Nhãn biến động của điểm đang chọn (Chống rớt dòng dọc tuyệt đối)
                Surface(
                    color = if (pointDelta > 0) PriceRed.copy(alpha = 0.15f)
                            else if (pointDelta < 0) PriceGreen.copy(alpha = 0.15f)
                            else DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (pointDelta > 0) "+${currencyFormat.format(pointDelta)} đ"
                               else if (pointDelta < 0) "-${currencyFormat.format(abs(pointDelta))} đ"
                               else "Không đổi",
                        color = if (pointDelta > 0) PriceRed
                                else if (pointDelta < 0) PriceGreen
                                else TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Tab chuyển loại nhiên liệu [ RON 95-III | E5 RON 92 | Dầu DO ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuelFilterChip(
                    label = "RON 95-III",
                    isSelected = selectedFuelType == 0,
                    onClick = {
                        selectedFuelType = 0
                        selectedPointIndex = null
                    },
                    modifier = Modifier.weight(1f)
                )
                FuelFilterChip(
                    label = "E5 RON 92",
                    isSelected = selectedFuelType == 1,
                    onClick = {
                        selectedFuelType = 1
                        selectedPointIndex = null
                    },
                    modifier = Modifier.weight(1f)
                )
                FuelFilterChip(
                    label = "Dầu DO",
                    isSelected = selectedFuelType == 2,
                    onClick = {
                        selectedFuelType = 2
                        selectedPointIndex = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Khung hiển thị thông tin điểm đang xem (Tooltip box)
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PetrolTeal.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kỳ điều hành:",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = activeHistoryPoint.fullDate.ifEmpty { activeHistoryPoint.date },
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Giá niêm yết:",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${currencyFormat.format(activePrice)} đ/lít",
                            color = PetrolTeal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. BIỂU ĐỒ NATIVE CANVAS
            SmoothSplineChart(
                points = pricePoints,
                dates = history.map { it.date },
                activeIndex = activeIndex,
                onPointSelected = { selectedPointIndex = it }
            )

            // Gợi ý tương tác
            Text(
                text = "💡 Chạm hoặc kéo ngón tay trên biểu đồ để xem chi tiết từng kỳ",
                color = TextSecondaryDark.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun FuelFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) PetrolTeal.copy(alpha = 0.2f) else DarkBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) PetrolTeal else DarkSurfaceVariant
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) PetrolTeal else TextSecondaryDark,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun SmoothSplineChart(
    points: List<Int>,
    dates: List<String>,
    activeIndex: Int,
    onPointSelected: (Int) -> Unit
) {
    if (points.size < 2) return

    val minPrice = points.minOrNull() ?: 20000
    val maxPrice = points.maxOrNull() ?: 30000
    // Thêm khoảng đệm 10% trên dưới để đường cong không chạm viền
    val padding = ((maxPrice - minPrice) * 0.15).coerceAtLeast(300.0).toInt()
    val yMin = (minPrice - padding).toFloat()
    val yMax = (maxPrice + padding).toFloat()
    val yRange = (yMax - yMin).coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val stepX = size.width / (points.size - 1)
                    val idx = (offset.x / stepX).toInt().coerceIn(0, points.size - 1)
                    onPointSelected(idx)
                }
            }
            .pointerInput(points) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val stepX = size.width / (points.size - 1)
                    val idx = (change.position.x / stepX).toInt().coerceIn(0, points.size - 1)
                    onPointSelected(idx)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height - 24.dp.toPx() // Chừa chỗ cho nhãn ngày bên dưới
            val stepX = width / (points.size - 1)

            // 1. Vẽ đường lưới mờ ngang (Grid lines)
            val gridColor = Color.White.copy(alpha = 0.06f)
            val gridSteps = 3
            for (i in 0..gridSteps) {
                val gridY = height * (i.toFloat() / gridSteps)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gridY),
                    end = Offset(width, gridY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            // Tính toán tọa độ (x, y) của tất cả các điểm
            val coords = points.mapIndexed { index, price ->
                val x = index * stepX
                val y = height - ((price - yMin) / yRange) * height
                Offset(x, y)
            }

            // 2. Tạo đường cong Bézier mượt mà (Cubic Path)
            val curvePath = Path()
            val fillPath = Path()

            curvePath.moveTo(coords[0].x, coords[0].y)
            fillPath.moveTo(coords[0].x, height)
            fillPath.lineTo(coords[0].x, coords[0].y)

            for (i in 1 until coords.size) {
                val prev = coords[i - 1]
                val curr = coords[i]
                val cX1 = prev.x + (curr.x - prev.x) / 2
                val cY1 = prev.y
                val cX2 = prev.x + (curr.x - prev.x) / 2
                val cY2 = curr.y

                curvePath.cubicTo(cX1, cY1, cX2, cY2, curr.x, curr.y)
                fillPath.cubicTo(cX1, cY1, cX2, cY2, curr.x, curr.y)
            }

            fillPath.lineTo(coords.last().x, height)
            fillPath.close()

            // 3. Tô màu Gradient phát sáng phía dưới đường cong
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PetrolTeal.copy(alpha = 0.35f),
                        PetrolTeal.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // 4. Vẽ đường cong biểu đồ chính
            drawPath(
                path = curvePath,
                color = PetrolTeal,
                style = Stroke(
                    width = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 5. Vẽ các chấm tròn trên đường cong
            coords.forEachIndexed { index, offset ->
                val isSelected = (index == activeIndex)
                if (isSelected) {
                    // Đường dóng dọc nét đứt
                    drawLine(
                        color = PetrolTeal.copy(alpha = 0.7f),
                        start = Offset(offset.x, 0f),
                        end = Offset(offset.x, height),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Chấm phát sáng lớn
                    drawCircle(
                        color = PetrolTeal.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = PetrolTeal,
                        radius = 6.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = offset
                    )
                } else {
                    // Chấm thường
                    drawCircle(
                        color = PetrolTeal.copy(alpha = 0.8f),
                        radius = 3.dp.toPx(),
                        center = offset
                    )
                }
            }
        }

        // 6. Nhãn ngày dưới trục X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dates.forEachIndexed { idx, date ->
                // Chỉ hiển thị nhãn của một số kỳ đại diện để không bị đè chữ
                val showLabel = (idx == 0 || idx == dates.size - 1 || idx == dates.size / 2)
                Text(
                    text = if (showLabel) date else "",
                    color = if (idx == activeIndex) PetrolTeal else TextSecondaryDark,
                    fontSize = 10.sp,
                    fontWeight = if (idx == activeIndex) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
