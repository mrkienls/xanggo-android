package com.example.xanggo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import kotlin.math.roundToInt

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
            // 1. Header Card: Tiêu đề + Icon Container + Trạng thái
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
                        color = PetrolTeal.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📊",
                            fontSize = 15.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                        text = if (pointDelta > 0) "+${currencyFormat.format(pointDelta)} đ/lít"
                               else if (pointDelta < 0) "-${currencyFormat.format(abs(pointDelta))} đ/lít"
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

            // 3. Khung hiển thị thông tin điểm đang xem (Tooltip box đẳng cấp)
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📅", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Kỳ điều hành",
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                            Text(
                                text = activeHistoryPoint.fullDate.ifEmpty { activeHistoryPoint.date },
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Đường phân cách mờ ở giữa
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(DarkSurfaceVariant)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🪙", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Giá niêm yết",
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${currencyFormat.format(activePrice)} đ/lít",
                                color = PetrolTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. BIỂU ĐỒ NATIVE CANVAS VỚI TRỤC Y, DẤU HÀO QUANG & TOOLTIP NỔI
            SmoothSplineChart(
                points = pricePoints,
                dates = history.map { it.date },
                fullDates = history.map { it.fullDate },
                activeIndex = activeIndex,
                currencyFormat = currencyFormat,
                onPointSelected = { selectedPointIndex = it }
            )

            // 5. Gợi ý tương tác (Pill button sang trọng)
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chạm vào điểm trên biểu đồ để xem chi tiết từng kỳ",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                    Text(text = "›", color = TextSecondaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
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
        color = if (isSelected) PetrolTeal else DarkBackground,
        shape = RoundedCornerShape(10.dp),
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
                color = if (isSelected) Color.White else TextSecondaryDark,
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
    fullDates: List<String>,
    activeIndex: Int,
    currencyFormat: NumberFormat,
    onPointSelected: (Int) -> Unit
) {
    if (points.size < 2) return

    val minPrice = points.minOrNull() ?: 20000
    val maxPrice = points.maxOrNull() ?: 30000
    val padding = ((maxPrice - minPrice) * 0.18).coerceAtLeast(300.0).toInt()
    val yMin = (minPrice - padding).toFloat()
    val yMax = (maxPrice + padding).toFloat()
    val yRange = (yMax - yMin).coerceAtLeast(1f)

    // Tạo 4 mốc giá trục Y tham chiếu
    val ySteps = 3
    val yLabels = (0..ySteps).map { i ->
        val price = (yMax - i * (yRange / ySteps)).roundToInt()
        val rounded = (price / 100) * 100
        currencyFormat.format(rounded)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val yAxisWidth = 42.dp.toPx()
                        val chartWidth = size.width - yAxisWidth
                        val stepX = chartWidth / (points.size - 1)
                        val relX = (offset.x - yAxisWidth).coerceIn(0f, chartWidth)
                        val idx = (relX / stepX).roundToInt().coerceIn(0, points.size - 1)
                        onPointSelected(idx)
                    }
                }
                .pointerInput(points) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val yAxisWidth = 42.dp.toPx()
                        val chartWidth = size.width - yAxisWidth
                        val stepX = chartWidth / (points.size - 1)
                        val relX = (change.position.x - yAxisWidth).coerceIn(0f, chartWidth)
                        val idx = (relX / stepX).roundToInt().coerceIn(0, points.size - 1)
                        onPointSelected(idx)
                    }
                }
        ) {
            val totalWidth = size.width
            val totalHeight = size.height
            val yAxisWidth = 42.dp.toPx()
            val bottomDateHeight = 22.dp.toPx()
            val chartWidth = totalWidth - yAxisWidth
            val chartHeight = totalHeight - bottomDateHeight
            val stepX = chartWidth / (points.size - 1)

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 9.sp.toPx()
                isAntiAlias = true
            }

            val datePaintNormal = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 8.5f.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val datePaintActive = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#14B8A6")
                textSize = 9.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // 1. Vẽ các đường lưới ngang và nhãn trục Y
            val gridColor = Color.White.copy(alpha = 0.07f)
            for (i in 0..ySteps) {
                val gridY = (chartHeight * (i.toFloat() / ySteps)).coerceIn(4.dp.toPx(), chartHeight - 4.dp.toPx())

                // Nhãn giá trục Y
                drawContext.canvas.nativeCanvas.drawText(
                    yLabels.getOrElse(i) { "" },
                    0f,
                    gridY + 3.dp.toPx(),
                    textPaint
                )

                // Đường lưới ngang đứt nét
                drawLine(
                    color = gridColor,
                    start = Offset(yAxisWidth, gridY),
                    end = Offset(totalWidth, gridY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            // Tọa độ (x, y) của tất cả các điểm
            val coords = points.mapIndexed { index, price ->
                val x = yAxisWidth + index * stepX
                val y = chartHeight - ((price - yMin) / yRange) * chartHeight
                Offset(x, y)
            }

            // 2. Tạo đường cong Bézier mượt mà (Cubic Spline)
            val curvePath = Path()
            val fillPath = Path()

            curvePath.moveTo(coords[0].x, coords[0].y)
            fillPath.moveTo(coords[0].x, chartHeight)
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

            fillPath.lineTo(coords.last().x, chartHeight)
            fillPath.close()

            // 3. Tô màu Gradient phát sáng dưới đường cong
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PetrolTeal.copy(alpha = 0.35f),
                        PetrolTeal.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = chartHeight
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

            // 5. Vẽ các điểm tròn, Halo Ring và Nhãn ngày trục X
            coords.forEachIndexed { index, offset ->
                val isSelected = (index == activeIndex)
                if (isSelected) {
                    // Đường dóng dọc nét đứt
                    drawLine(
                        color = PetrolTeal.copy(alpha = 0.6f),
                        start = Offset(offset.x, 0f),
                        end = Offset(offset.x, chartHeight),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )

                    // Vòng hào quang phát sáng (Halo Ring)
                    drawCircle(
                        color = PetrolTeal.copy(alpha = 0.25f),
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
                        color = PetrolTeal.copy(alpha = 0.75f),
                        radius = 3.dp.toPx(),
                        center = offset
                    )
                }

                // Vẽ nhãn ngày dưới trục X
                val dateText = dates.getOrElse(index) { "" }
                drawContext.canvas.nativeCanvas.drawText(
                    dateText,
                    offset.x,
                    totalHeight - 4.dp.toPx(),
                    if (isSelected) datePaintActive else datePaintNormal
                )
            }
        }

        // 6. Floating Tooltip ngay trên điểm đang chọn
        val tooltipDate = fullDates.getOrElse(activeIndex) { dates.getOrElse(activeIndex) { "" } }
        val tooltipPrice = points.getOrElse(activeIndex) { 0 }

        Box(
            modifier = Modifier
                .align(if (activeIndex > points.size / 2) Alignment.TopEnd else Alignment.TopStart)
                .padding(
                    start = if (activeIndex <= points.size / 2) 48.dp else 0.dp,
                    end = if (activeIndex > points.size / 2) 8.dp else 0.dp,
                    top = 2.dp
                )
                .background(Color(0xFF0F172A).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                .border(1.dp, PetrolTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tooltipDate,
                    color = TextSecondaryDark,
                    fontSize = 9.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(PetrolTeal, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currencyFormat.format(tooltipPrice)} đ/lít",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
