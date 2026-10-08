package com.example.social_media_tracker_app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.social_media_tracker_app.data.model.DailyStats
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
fun ProfileScreen(
    repository: StatsRepository,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appConfig by repository.appConfig.collectAsState()
    val petState by repository.petState.collectAsState()
    val scrollState = rememberScrollState()

    var selectedDays by remember { mutableIntStateOf(7) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val allRecords = remember(refreshTrigger) { repository.getAllDailyRecords() }
    val filteredRecords = remember(selectedDays, allRecords) {
        if (selectedDays == 0 || allRecords.size <= selectedDays) allRecords
        else allRecords.takeLast(selectedDays)
    }

    // ─── TÍNH TOÁN METRICS ───────────────────────────────────────────
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

    // Đánh giá bồn chồn
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

    // Persona
    val mindfulRatio = if (totalSwipes > 0) (filteredRecords.sumOf { it.validViews } * 100) / totalSwipes else 0
    val personaTitle = when {
        mindfulRatio >= 70 -> "🧘 Bậc Thầy Tỉnh Thức"
        mindfulRatio >= 40 -> "🌿 Người Gieo Thói Quen"
        else -> "🌱 Mầm Non Tập Tỉnh Thức"
    }
    val personaDesc = when {
        mindfulRatio >= 70 -> "Bạn kiểm soát sự chú ý rất tốt, xem sâu có chủ đích thay vì lướt vô thức."
        mindfulRatio >= 40 -> "Bạn đang duy trì thói quen lọc nội dung tích cực và hạn chế cám dỗ dopamine."
        else -> "Hãy dừng lại 3 giây trước mỗi lần quẹt để nâng cao tỉ lệ tỉnh thức nhé!"
    }

    // AI Coach State
    var aiCoachResult by remember { mutableStateOf<LongTermCoachResult?>(null) }
    var isCallingAi by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ─── TOP BAR ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "👤 Hồ Sơ Của Tôi",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Báo cáo phân tích dài hạn & hành trình tỉnh thức",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Prominent Settings Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x268B5CF6))
                    .border(1.2.dp, Color(0xFF8B5CF6), RoundedCornerShape(12.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "⚙️", fontSize = 15.sp)
                    Text(
                        text = "Cài Đặt",
                        color = Color(0xFFC4B5FD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── USER PERSONA HERO CARD ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF2E1065))
                    )
                )
                .border(1.dp, Color(0x668B5CF6), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = personaTitle,
                            color = Color(0xFFA78BFA),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tỷ lệ chú tâm: $mindfulRatio%",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33F59E0B))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔥 ${petState.streakDays} ngày streak",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = personaDesc,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                if (appConfig.masterGoal.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22000000))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mục tiêu: \"${appConfig.masterGoal}\"",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── PERIOD FILTER TABS ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 BÁO CÁO DÀI HẠN",
                color = Color(0xFFC4B5FD),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(2.dp)
            ) {
                ProfileFilterTab("7 ngày", selectedDays == 7) { selectedDays = 7 }
                ProfileFilterTab("14 ngày", selectedDays == 14) { selectedDays = 14 }
                ProfileFilterTab("30 ngày", selectedDays == 30) { selectedDays = 30 }
                ProfileFilterTab("Tất cả", selectedDays == 0) { selectedDays = 0 }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── KHỐI 1: CHỈ SỐ ĐỘ NGHIỆN & MẤT KIÊN NHẪN (IMPULSIVE METRICS) ───
        MetricSectionHeader(title = "⚡ ĐỘ NGHIỆN & MẤT KIÊN NHẪN (IMPULSIVE METRICS)", color = Color(0xFFF43F5E))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiMetricCard(
                icon = "📱",
                title = "Tổng lượt vuốt",
                value = "$totalSwipes",
                subtext = "Trung bình: $avgSwipes lượt/ngày",
                color = Color(0xFFF43F5E),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                icon = "🔄",
                title = "Tải lại trang (F5)",
                value = "$totalReloads",
                subtext = reloadAssess,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                icon = "⏩",
                title = "Bỏ dở vội (<15%)",
                value = "$skipPct%",
                subtext = "$totalSkips video bị lướt vội",
                color = Color(0xFFFB7185),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── KHỐI 2: CHỈ SỐ CHẤT LƯỢNG & GIÁ TRỊ (INTENTIONAL METRICS) ───
        MetricSectionHeader(title = "🎯 CHẤT LƯỢNG & GIÁ TRỊ (INTENTIONAL METRICS)", color = Color(0xFF10B981))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiMetricCard(
                icon = "🧭",
                title = "Nội dung hữu ích",
                value = "$usefulPct%",
                subtext = "$totalUseful video bám sát mục tiêu",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                icon = "⏳",
                title = "Xem sâu (≥80%)",
                value = "$deepPct%",
                subtext = "$totalDeep video xem trọn vẹn",
                color = Color(0xFF06B6D4),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                icon = "⏱️",
                title = "Tổng thời gian",
                value = formatDuration(totalActiveSec + totalPassiveSec),
                subtext = "Chủ động: ${formatDuration(totalActiveSec)}",
                color = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── KHỐI 3: KỶ LUẬT & THÀNH TỰU (DISCIPLINE & GAMIFICATION) ───
        MetricSectionHeader(title = "🛡️ KỶ LUẬT & THÀNH TỰU (GAMIFICATION)", color = Color(0xFFA78BFA))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiMetricCard(
                icon = "🔥",
                title = "Focus Streak",
                value = "${petState.streakDays} ngày",
                subtext = "Không chạm Mốc 3 trần",
                color = Color(0xFFFBBF24),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                icon = "🧘",
                title = "Vượt qua cám dỗ",
                value = "$temptationPct%",
                subtext = "$totalResisted / $totalTemptation lần đóng app",
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                icon = "⚡",
                title = "Sức khỏe Pet",
                value = "${avgPetHealth}⚡",
                subtext = if (avgPetHealth >= 70) "Trạng thái: Vui vẻ ✨" else "Trạng thái: Cần hồi phục 🌿",
                color = Color(0xFF34D399),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── 5 BIỂU ĐỒ TRỰC QUAN HÓA (COMPOSE CANVAS) ───

        // Biểu đồ 1: Cột chồng cân bằng năng lượng
        ChartCardContainer(
            title = "📊 CỘT CHỒNG: CÂN BẰNG NĂNG LƯỢNG (MỤC TIÊU VS XAO NHÃNG)",
            description = "Xanh = Mục tiêu/Học tập, Đỏ = Giải trí/Xao nhãng"
        ) {
            StackedBarTimeChart(records = filteredRecords)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Biểu đồ 2: Đường xu hướng Swipes & F5 Reload
        ChartCardContainer(
            title = "📈 ĐƯỜNG XU HƯỚNG: TỔNG LƯỢT VUỐT & TẦN SUẤT F5 RELOAD",
            description = "Đỏ = Swipes, Vàng = F5/Reloads. Đối chiếu mức độ hạ nhiệt dopamine."
        ) {
            MultiLineSwipesF5Chart(records = filteredRecords)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Biểu đồ 3: Bản đồ nhiệt 7x24h (Hourly Heatmap)
        ChartCardContainer(
            title = "🗺️ BẢN ĐỒ NHIỆT 24H: ĐIỂM MÙ TÂM LÝ (VULNERABLE WINDOWS)",
            description = "Bóc trần khung giờ mất kiểm soát nhất trong ngày: $vulnerableWindow"
        ) {
            HourlyHeatmapChart(records = filteredRecords)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Biểu đồ 4: Phân bổ danh mục Donut Chart
        ChartCardContainer(
            title = "🍩 BIỂU ĐỒ VÀNH KHUYÊN: PHÂN BỔ DANH MỤC NỘI DUNG",
            description = "Tỷ lệ thời gian và số video đổ vào từng chủ đề"
        ) {
            CategoriesDonutChart(records = filteredRecords)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Biểu đồ 5: Thanh so sánh Attention Span
        ChartCardContainer(
            title = "⚖️ THANH SO SÁNH: PHỤC HỒI KHẢ NĂNG CHÚ Ý (ATTENTION SPAN)",
            description = "Tỷ lệ Xem trọn vẹn (≥80%) vs Bỏ dở giữa chừng (<15%)"
        ) {
            AttentionRatioGauge(records = filteredRecords)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── KHỐI AI REFLECTION COACH DÀI HẠN ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                    )
                )
                .border(1.2.dp, Color(0xFF8B5CF6), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI REFLECTION COACH",
                            color = Color(0xFFA78BFA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x338B5CF6))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedDays == 7) "Tuần này" else if (selectedDays == 30) "Tháng này" else "Gần đây",
                            color = Color(0xFFC4B5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Default Heuristic Coach Insights
                val coachFeedback = aiCoachResult?.coachFeedback ?: "Trong $selectedDays ngày qua, bạn đã thực hiện tổng cộng $totalSwipes lượt vuốt (${avgSwipes} lượt/ngày) và $totalReloads lần tải lại trang. Tỷ lệ xem sâu đạt $deepPct%, tỷ lệ bám sát mục tiêu đạt $usefulPct%. Bạn đang từng bước rèn luyện năng lực chánh niệm số!"
                val brightSpot = aiCoachResult?.brightSpot ?: "Điểm sáng lớn nhất: Tỷ lệ vượt qua cám dỗ thở Box Breathing đạt $temptationPct% với $totalResisted lần rời đi thành công."
                val advice = aiCoachResult?.vulnerableWindowAdvice ?: "Khung giờ điểm mù dễ mất kiểm soát: $vulnerableWindow. Hãy chủ động cất điện thoại cách xa tầm tay."
                val action = aiCoachResult?.actionSuggestion ?: "Kích hoạt chế độ thư giãn hoặc Màn hình Đen Trắng trước 22h00."

                Text(
                    text = coachFeedback,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x2210B981))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🌟 $brightSpot",
                        color = Color(0xFF34D399),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22F43F5E))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⚠️ $advice",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x2238BDF8))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 Hành động đề xuất: $action",
                        color = Color(0xFF7DD3FC),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isCallingAi) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Đang tổng hợp Insight...", fontSize = 12.sp)
                    } else {
                        Text(text = "✨ Đúc Kết Lại Cùng Cố Vấn AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── RECENT DAILY LOGS ───
        Text(
            text = "📜 LỊCH SỬ CÁC NGÀY GẦN ĐÂY",
            color = Color(0xFFC4B5FD),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có bản ghi lịch sử nào được ghi nhận.",
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredRecords.reversed().take(5).forEach { stat ->
                    DailyRecordRow(stat = stat)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun KpiMetricCard(
    icon: String,
    title: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtext,
                color = Color(0xFFCBD5E1),
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChartCardContainer(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProfileFilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF8B5CF6) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DailyRecordRow(stat: DailyStats) {
    val mindfulPercent = if (stat.totalSwipes > 0) (stat.validViews * 100) / stat.totalSwipes else 0
    val isGood = mindfulPercent >= 50

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(0.8.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stat.date,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stat.totalSwipes} lượt lướt • ${formatDuration(stat.activeSeconds)}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGood) Color(0x2210B981) else Color(0x22EF4444))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$mindfulPercent% chú tâm",
                        color = if (isGood) Color(0xFF34D399) else Color(0xFFFCA5A5),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
