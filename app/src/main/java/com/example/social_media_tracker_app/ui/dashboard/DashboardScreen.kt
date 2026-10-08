package com.example.social_media_tracker_app.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.social_media_tracker_app.data.repository.StatsRepository
import com.example.social_media_tracker_app.domain.gemini.GeminiCoachService
import com.example.social_media_tracker_app.domain.gemini.LongTermCoachResult
import com.example.social_media_tracker_app.ui.components.AttentionRatioGauge
import com.example.social_media_tracker_app.ui.components.CategoriesDonutChart
import com.example.social_media_tracker_app.ui.components.HourlyHeatmapChart
import com.example.social_media_tracker_app.ui.components.MultiLineSwipesF5Chart
import com.example.social_media_tracker_app.ui.components.StackedBarTimeChart
import kotlinx.coroutines.launch

@Composable
fun DashboardDialog(
    repository: StatsRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appConfig by repository.appConfig.collectAsState()
    val petState by repository.petState.collectAsState()

    var selectedDays by remember { mutableIntStateOf(7) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val allRecords = remember(refreshTrigger) { repository.getAllDailyRecords() }
    val filteredRecords = remember(selectedDays, allRecords) {
        if (selectedDays == 0 || allRecords.size <= selectedDays) allRecords
        else allRecords.takeLast(selectedDays)
    }

    // Calculations
    val n = maxOf(1, filteredRecords.size)
    val totalActiveSec = filteredRecords.sumOf { it.activeSeconds }
    val totalPassiveSec = filteredRecords.sumOf { it.passiveSeconds }
    val totalSwipes = filteredRecords.sumOf { it.totalSwipes }
    val avgSwipes = totalSwipes / n
    val totalReloads = filteredRecords.sumOf { it.reloadCount }

    val totalLong = filteredRecords.sumOf { it.totalLongVideosWatched }
    val totalUseful = filteredRecords.sumOf { it.youtube.longVideos.usefulCount + it.facebook.longVideos.usefulCount }
    val totalDeep = filteredRecords.sumOf { it.deepWatchCount }
    val totalSkips = filteredRecords.sumOf { it.impulsiveSkipCount }

    val usefulPct = if (totalLong > 0) ((totalUseful.toDouble() / totalLong) * 100).toInt() else 0
    val deepPct = if (totalLong > 0) ((totalDeep.toDouble() / totalLong) * 100).toInt() else 0
    val skipPct = if (totalLong > 0) ((totalSkips.toDouble() / totalLong) * 100).toInt() else 0

    val totalResisted = filteredRecords.sumOf { it.temptation.resistedCount }
    val totalSuccumbed = filteredRecords.sumOf { it.temptation.succumbedCount }
    val totalTemptation = totalResisted + totalSuccumbed
    val temptationPct = if (totalTemptation > 0) ((totalResisted.toDouble() / totalTemptation) * 100).toInt() else 100

    val petHealthSum = filteredRecords.sumOf { it.petEnergyEndOfDay }
    val avgPetHealth = petHealthSum / n

    val avgReloads = totalReloads / n
    val reloadAssess = when {
        avgReloads >= 10 -> "Rất bồn chồn (F5 liên tục)"
        avgReloads >= 5 -> "Hơi bồn chồn, tìm kiếm dopamine"
        else -> "Bình tĩnh, ít F5"
    }

    // Vulnerable Window
    val hourlyTotals = IntArray(24) { 0 }
    filteredRecords.forEach { r ->
        r.hourly.forEach { h ->
            val score = h.swipes + (h.reloads * 2) + (h.activeSeconds / 60).toInt()
            if (h.hour in 0..23) hourlyTotals[h.hour] += score
        }
    }
    var maxHScore = 0
    var worstHour = -1
    hourlyTotals.forEachIndexed { h, score ->
        if (score > maxHScore) {
            maxHScore = score
            worstHour = h
        }
    }
    val vulnerableWindow = if (worstHour >= 0 && maxHScore > 0) {
        val nextH = (worstHour + 1) % 24
        "${String.format("%02d", worstHour)}h00 - ${String.format("%02d", nextH)}h00 (Đỉnh xao nhãng)"
    } else {
        "Chưa phát hiện điểm mù rõ rệt"
    }

    var aiCoachResult by remember { mutableStateOf<LongTermCoachResult?>(null) }
    var isCallingAi by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xF50B101D))
                .border(1.5.dp, Color(0xFF6366F1), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(text = "📊", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Báo Cáo Phân Tích Dài Hạn",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Thống kê xu hướng nghiện & phục hồi tập trung",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Period Filter & Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(2.dp)
                    ) {
                        DialogFilterTab("7N", selectedDays == 7) { selectedDays = 7 }
                        DialogFilterTab("14N", selectedDays == 14) { selectedDays = 14 }
                        DialogFilterTab("30N", selectedDays == 30) { selectedDays = 30 }
                        DialogFilterTab("Tất cả", selectedDays == 0) { selectedDays = 0 }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── 3 NHÓM KPI CHÍNH ───
                // Nhóm 1: Impulsive
                Text(
                    text = "⚡ ĐỘ NGHIỆN & MẤT KIÊN NHẪN",
                    color = Color(0xFFF43F5E),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DialogKpiCard("📱 Lướt", "$totalSwipes", "Avg $avgSwipes/ngày", Color(0xFFF43F5E), Modifier.weight(1f))
                    DialogKpiCard("🔄 F5", "$totalReloads", reloadAssess, Color(0xFFF59E0B), Modifier.weight(1f))
                    DialogKpiCard("⏩ Bỏ dở", "$skipPct%", "$totalSkips video", Color(0xFFFB7185), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nhóm 2: Intentional
                Text(
                    text = "🎯 CHẤT LƯỢNG & GIÁ TRỊ",
                    color = Color(0xFF10B981),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DialogKpiCard("🧭 Hữu ích", "$usefulPct%", "$totalUseful video", Color(0xFF10B981), Modifier.weight(1f))
                    DialogKpiCard("⏳ Xem sâu", "$deepPct%", "$totalDeep video", Color(0xFF06B6D4), Modifier.weight(1f))
                    DialogKpiCard("⏱️ Thời gian", formatDuration(totalActiveSec + totalPassiveSec), "Chủ động ${formatDuration(totalActiveSec)}", Color(0xFF38BDF8), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nhóm 3: Discipline
                Text(
                    text = "🛡️ KỶ LUẬT & THÀNH TỰU",
                    color = Color(0xFFA78BFA),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DialogKpiCard("🔥 Streak", "${petState.streakDays}N", "Không chạm M3", Color(0xFFFBBF24), Modifier.weight(1f))
                    DialogKpiCard("🧘 Vượt cám dỗ", "$temptationPct%", "$totalResisted lần", Color(0xFF8B5CF6), Modifier.weight(1f))
                    DialogKpiCard("⚡ Pet", "${avgPetHealth}⚡", if (avgPetHealth >= 70) "Vui vẻ ✨" else "Cần chăm 🌿", Color(0xFF34D399), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ─── 5 BIỂU ĐỒ ───
                DialogChartSection("📊 Cột chồng: Cân bằng Năng lượng (Mục tiêu vs Xao nhãng)") {
                    StackedBarTimeChart(records = filteredRecords)
                }

                Spacer(modifier = Modifier.height(12.dp))

                DialogChartSection("📈 Xu hướng: Swipes & F5 Reload (Độ dốc dopamine)") {
                    MultiLineSwipesF5Chart(records = filteredRecords)
                }

                Spacer(modifier = Modifier.height(12.dp))

                DialogChartSection("🗺️ Bản đồ nhiệt 24h: Điểm mù tâm lý ($vulnerableWindow)") {
                    HourlyHeatmapChart(records = filteredRecords)
                }

                Spacer(modifier = Modifier.height(12.dp))

                DialogChartSection("🍩 Biểu đồ vành khuyên: Phân bổ danh mục nội dung") {
                    CategoriesDonutChart(records = filteredRecords)
                }

                Spacer(modifier = Modifier.height(12.dp))

                DialogChartSection("⚖️ Phục hồi khả năng chú ý (Attention Span)") {
                    AttentionRatioGauge(records = filteredRecords)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ─── AI REFLECTION COACH ───
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))))
                        .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "AI REFLECTION COACH", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val coachFeedback = aiCoachResult?.coachFeedback ?: "Trong $selectedDays ngày qua, bạn đã thực hiện $totalSwipes lượt vuốt và $totalReloads lần tải lại. Tỷ lệ xem sâu đạt $deepPct%, tỷ lệ đúng mục tiêu $usefulPct%."
                        val brightSpot = aiCoachResult?.brightSpot ?: "Điểm sáng: Tỷ lệ vượt qua cám dỗ thở Box Breathing đạt $temptationPct%."
                        val advice = aiCoachResult?.vulnerableWindowAdvice ?: "Khung giờ điểm mù: $vulnerableWindow."

                        Text(text = coachFeedback, color = Color(0xFFE2E8F0), fontSize = 11.5.sp, lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "🌟 $brightSpot", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "⚠️ $advice", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Medium)

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isCallingAi = true
                                scope.launch {
                                    val summaryMap = mapOf<String, Any>(
                                        "totalSwipes" to totalSwipes,
                                        "avgSwipes" to avgSwipes,
                                        "swipeTrendText" to "$avgSwipes lượt/ngày",
                                        "totalReloads" to totalReloads,
                                        "deepWatchPct" to deepPct,
                                        "deepWatchCount" to totalDeep,
                                        "impulsiveSkipPct" to skipPct,
                                        "impulsiveSkipCount" to totalSkips,
                                        "usefulPct" to usefulPct,
                                        "streakDays" to petState.streakDays,
                                        "temptationResistedPct" to temptationPct,
                                        "avgPetHealth" to avgPetHealth,
                                        "vulnerableWindow" to vulnerableWindow
                                    )
                                    val res = GeminiCoachService.getLongTermCoachInsight(
                                        summaryStats = summaryMap,
                                        periodLabel = if (selectedDays == 7) "tuần này" else "tháng này",
                                        masterGoal = appConfig.masterGoal,
                                        apiKey = appConfig.apiKey
                                    )
                                    aiCoachResult = res
                                    isCallingAi = false
                                }
                            },
                            enabled = !isCallingAi,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isCallingAi) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Đang tổng hợp...", fontSize = 11.sp)
                            } else {
                                Text(text = "✨ Đúc Kết Lại Cùng Cố Vấn AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Đóng Báo Cáo", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DialogChartSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun DialogKpiCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 9.5.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, color = Color(0xFFCBD5E1), fontSize = 8.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun DialogFilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF8B5CF6) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
