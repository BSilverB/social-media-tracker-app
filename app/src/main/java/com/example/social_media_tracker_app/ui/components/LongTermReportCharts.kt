package com.example.social_media_tracker_app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.social_media_tracker_app.data.model.DailyStats

/**
 * 1. Stacked Bar Chart (Cân bằng năng lượng: Mục tiêu vs Xao nhãng)
 * Trục Y: Thời lượng phút. Xanh (#10B981) = Mục tiêu/Học tập, Đỏ (#F43F5E) = Xao nhãng/Giải trí
 */
@Composable
fun StackedBarTimeChart(records: List<DailyStats>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (records.isEmpty()) {
            Text(
                text = "Chưa có đủ dữ liệu ghi nhận",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val paddingBottom = 22f
            val chartH = h - paddingBottom

            val maxMinutes = maxOf(
                30f,
                records.maxOfOrNull { (it.totalSeconds / 60).toFloat() }?.let {
                    kotlin.math.ceil(it / 30f) * 30f
                } ?: 60f
            )

            val n = records.size
            val stepX = w / n.toFloat()
            val colWidth = (stepX * 0.6f).coerceIn(8f, 28f)

            records.forEachIndexed { i, stat ->
                val x = i * stepX + (stepX - colWidth) / 2f
                val activeMin = (stat.activeSeconds / 60).toFloat()
                val usefulMin = activeMin * (stat.usefulVideosRatioPercentage / 100f)
                val distractMin = maxOf(0f, activeMin - usefulMin) + (stat.passiveSeconds / 60).toFloat()

                val usefulH = (usefulMin / maxMinutes) * chartH
                val distractH = (distractMin / maxMinutes) * chartH

                val yUseful = chartH - usefulH
                val yDistract = yUseful - distractH

                // Useful / Goal (Xanh Ngọc #10B981)
                if (usefulH > 0) {
                    drawRoundRect(
                        color = Color(0xFF10B981),
                        topLeft = Offset(x, yUseful),
                        size = Size(colWidth, usefulH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                // Distraction / Passive (Đỏ #F43F5E)
                if (distractH > 0) {
                    drawRoundRect(
                        color = Color(0xFFF43F5E),
                        topLeft = Offset(x, yDistract),
                        size = Size(colWidth, distractH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }

        // Legends & Dates labels row below canvas
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            records.forEach { r ->
                val dateLabel = if (r.date.length >= 5) r.date.takeLast(5) else r.date
                Text(text = dateLabel, color = Color(0xFF94A3B8), fontSize = 9.sp)
            }
        }
    }
}

/**
 * 2. Multi-Line Chart (Đường xu hướng: Swipes & F5 Reload)
 * Đỏ (#F43F5E) = Swipes, Vàng (#F59E0B) = Reloads F5
 */
@Composable
fun MultiLineSwipesF5Chart(records: List<DailyStats>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (records.isEmpty()) {
            Text(
                text = "Chưa có đủ dữ liệu ghi nhận",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val paddingBottom = 22f
            val chartH = h - paddingBottom

            val maxVal = maxOf(
                10f,
                records.maxOfOrNull { maxOf(it.totalSwipes, it.reloadCount).toFloat() }?.let {
                    kotlin.math.ceil(it / 10f) * 10f
                } ?: 20f
            )

            val n = records.size
            val stepX = if (n > 1) w / (n - 1).toFloat() else w / 2f

            val swipePoints = records.mapIndexed { idx, r ->
                val x = if (n > 1) idx * stepX else w / 2f
                val y = chartH - (r.totalSwipes.toFloat() / maxVal) * chartH
                Offset(x, y)
            }

            val reloadPoints = records.mapIndexed { idx, r ->
                val x = if (n > 1) idx * stepX else w / 2f
                val y = chartH - (r.reloadCount.toFloat() / maxVal) * chartH
                Offset(x, y)
            }

            // Fill gradient under Swipes line
            if (swipePoints.size > 1) {
                val fillPath = Path().apply {
                    moveTo(swipePoints.first().x, chartH)
                    swipePoints.forEach { lineTo(it.x, it.y) }
                    lineTo(swipePoints.last().x, chartH)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x44F43F5E), Color(0x00F43F5E)),
                        startY = 0f,
                        endY = chartH
                    )
                )
            }

            // Swipes line
            val swipePath = Path()
            swipePoints.forEachIndexed { i, p ->
                if (i == 0) swipePath.moveTo(p.x, p.y) else swipePath.lineTo(p.x, p.y)
            }
            drawPath(swipePath, color = Color(0xFFF43F5E), style = Stroke(width = 4f, cap = StrokeCap.Round))
            swipePoints.forEach { p ->
                drawCircle(Color(0xFFF43F5E), radius = 5f, center = p)
                drawCircle(Color.White, radius = 2.5f, center = p)
            }

            // Reloads line
            val reloadPath = Path()
            reloadPoints.forEachIndexed { i, p ->
                if (i == 0) reloadPath.moveTo(p.x, p.y) else reloadPath.lineTo(p.x, p.y)
            }
            drawPath(reloadPath, color = Color(0xFFF59E0B), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            reloadPoints.forEach { p ->
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(p.x - 3.5f, p.y - 3.5f),
                    size = Size(7f, 7f)
                )
            }
        }

        // Dates labels
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            records.forEach { r ->
                val dateLabel = if (r.date.length >= 5) r.date.takeLast(5) else r.date
                Text(text = dateLabel, color = Color(0xFF94A3B8), fontSize = 9.sp)
            }
        }
    }
}

/**
 * 3. Hourly Activity Heatmap (Ma trận 7 ngày x 24 giờ)
 * Phát hiện "Khung giờ điểm mù / dễ mất kiểm soát nhất" (Vulnerable Window).
 */
@Composable
fun HourlyHeatmapChart(records: List<DailyStats>, modifier: Modifier = Modifier) {
    val daysToDisplay = records.takeLast(7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        if (daysToDisplay.isEmpty()) {
            Text(
                text = "Chưa có đủ dữ liệu theo giờ",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Hours header (0h, 6h, 12h, 18h, 23h)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 38.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0h", color = Color(0xFF64748B), fontSize = 9.sp)
                Text(text = "6h", color = Color(0xFF64748B), fontSize = 9.sp)
                Text(text = "12h", color = Color(0xFF64748B), fontSize = 9.sp)
                Text(text = "18h", color = Color(0xFF64748B), fontSize = 9.sp)
                Text(text = "23h", color = Color(0xFF64748B), fontSize = 9.sp)
            }

            // Max score in matrix
            var maxScore = 5
            daysToDisplay.forEach { day ->
                day.hourly.forEach { h ->
                    val score = h.swipes + (h.reloads * 2) + (h.activeSeconds / 60).toInt()
                    if (score > maxScore) maxScore = score
                }
            }

            daysToDisplay.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateLabel = if (day.date.length >= 5) day.date.takeLast(5) else day.date
                    Text(
                        text = dateLabel,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        modifier = Modifier.width(36.dp)
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        for (h in 0..23) {
                            val bucket = day.hourly.getOrNull(h)
                            val score = (bucket?.swipes ?: 0) + ((bucket?.reloads ?: 0) * 2) + ((bucket?.activeSeconds ?: 0L) / 60).toInt()
                            val cellColor = when {
                                score == 0 -> Color(0x14FFFFFF)
                                score.toFloat() / maxScore < 0.25f -> Color(0x666366F1) // Xanh chàm nhạt
                                score.toFloat() / maxScore < 0.6f -> Color(0xBB8B5CF6)  // Tím vừa
                                else -> Color(0xEEEF4444)                              // Đỏ điểm mù
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(cellColor)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ít lướt", color = Color(0xFF64748B), fontSize = 9.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x14FFFFFF)))
                Spacer(modifier = Modifier.width(3.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x666366F1)))
                Spacer(modifier = Modifier.width(3.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xBB8B5CF6)))
                Spacer(modifier = Modifier.width(3.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xEEEF4444)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Dày đặc (Điểm mù)", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 4. Categories Donut Chart (Biểu đồ vành khuyên phân bổ danh mục)
 * 5 nhóm: Lập trình/Công nghệ (#3B82F6), Phát triển bản thân (#10B981), Tài chính (#F59E0B), Giải trí (#F43F5E), Khác (#64748B)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesDonutChart(records: List<DailyStats>, modifier: Modifier = Modifier) {
    var goalCount = 0
    var leisureCount = 0
    var distractCount = 0
    var unclassCount = 0

    records.forEach { r ->
        goalCount += r.goalVideosCount
        leisureCount += r.leisureVideosCount
        distractCount += r.distractionVideosCount
        unclassCount += r.unclassifiedVideosCount
    }

    val total = goalCount + leisureCount + distractCount + unclassCount

    val catList = listOf(
        Triple("Mục tiêu / Học tập", goalCount, Color(0xFF10B981)),
        Triple("Giải trí lành mạnh", leisureCount, Color(0xFF3B82F6)),
        Triple("Bẫy Dopamine / Xao nhãng", distractCount, Color(0xFFF43F5E)),
        Triple("Chưa phân loại", unclassCount, Color(0xFF64748B))
    ).filter { it.second > 0 || total == 0 }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(130.dp)) {
                    val strokeW = 24f
                    val radius = (size.minDimension - strokeW) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    if (total == 0) {
                        drawCircle(
                            color = Color(0x1AFFFFFF),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeW)
                        )
                    } else {
                        var startAngle = -90f
                        catList.forEach { (_, count, color) ->
                            val sweepAngle = (count.toFloat() / total.toFloat()) * 360f
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeW)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$total",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(text = "video", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Legends FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                maxItemsInEachRow = 2
            ) {
                catList.forEach { (label, count, color) ->
                    val pct = if (total > 0) ((count.toFloat() / total.toFloat()) * 100).toInt() else 0
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$label ($pct%)", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/**
 * 5. Attention Ratio Gauge (Thanh so sánh khả năng chú ý)
 * Xem trọn vẹn (≥80% hoặc >3p) vs Bỏ dở vội (<15%).
 */
@Composable
fun AttentionRatioGauge(records: List<DailyStats>, modifier: Modifier = Modifier) {
    val totalDeep = records.sumOf { it.deepWatchCount }
    val totalSkips = records.sumOf { it.impulsiveSkipCount }
    val total = totalDeep + totalSkips
    val deepPct = if (total > 0) ((totalDeep.toFloat() / total.toFloat()) * 100).toInt() else 50
    val skipPct = 100 - deepPct

    val (assessmentText, assessmentColor) = when {
        total == 0 -> Pair("Chưa có đủ video dài để đo lường Attention Span", Color(0xFF64748B))
        deepPct >= 70 -> Pair("✨ Khả năng chú ý và kiên nhẫn đang phục hồi rất tích cực!", Color(0xFF34D399))
        deepPct >= 40 -> Pair("⚡ Sự chú ý ở mức trung bình, hãy kiên nhẫn xem hết nội dung hữu ích.", Color(0xFFFBBF24))
        else -> Pair("⚠️ Xu hướng lướt vội chiếm ưu thế. Hãy tập trung vào video dài trên 3 phút.", Color(0xFFF87171))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$deepPct% Xem sâu ($totalDeep vid)",
                    color = Color(0xFF10B981),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$skipPct% Bỏ dở ($totalSkips vid)",
                    color = Color(0xFFEF4444),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gauge Split Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                Row(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .weight(deepPct.coerceAtLeast(1).toFloat())
                            .height(22.dp)
                            .background(Color(0xFF10B981))
                    )
                    Box(
                        modifier = Modifier
                            .weight(skipPct.coerceAtLeast(1).toFloat())
                            .height(22.dp)
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = assessmentText,
                color = assessmentColor,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
