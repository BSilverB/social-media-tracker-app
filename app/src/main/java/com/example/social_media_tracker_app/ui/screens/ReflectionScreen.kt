package com.example.social_media_tracker_app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.social_media_tracker_app.data.model.DailyStats
import com.example.social_media_tracker_app.data.model.WatchedVideoItem
import com.example.social_media_tracker_app.domain.gemini.GeminiCoachResult
import com.example.social_media_tracker_app.domain.gemini.GeminiCoachService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReflectionScreen(
    stats: DailyStats,
    masterGoal: String,
    geminiApiKey: String,
    targetKeywords: List<String> = emptyList(),
    onSaveReflection: (lesson1: String, lesson2: String, rating: Int) -> Unit,
    onApplyAiFeedback: ((GeminiCoachResult) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var lesson1 by remember { mutableStateOf(stats.reflection.lesson1) }
    var lesson2 by remember { mutableStateOf(stats.reflection.lesson2) }
    var rating by remember { mutableIntStateOf(stats.reflection.rating) }
    var isSaved by remember { mutableStateOf(stats.reflection.submittedAt != null) }

    var isLogExpanded by remember { mutableStateOf(false) }
    var isLoadingAi by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<GeminiCoachResult?>(null) }

    // Partition watched videos into deep views vs impulsive skips
    val deepWatchedVideos = remember(stats.watchedVideos) {
        stats.watchedVideos.filter { it.watchedSeconds >= 2 }
    }
    val impulsiveVideosCount = remember(stats.watchedVideos, stats.totalSwipes, stats.validViews) {
        val listedImpulsive = stats.watchedVideos.count { it.watchedSeconds < 2 }
        val calculatedImpulsive = maxOf(0, stats.totalSwipes - stats.validViews)
        maxOf(listedImpulsive, calculatedImpulsive)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // Screen Header
        Text(
            text = "📝 Tâm Sự Cuối Ngày",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Đúc kết bài học & nuôi dưỡng tâm trí",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Digest Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x1F1E293B))
                .border(1.2.dp, Color(0x338B5CF6), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "BẢN TÓM TẮT HÔM NAY (DIGEST)",
                    color = Color(0xFFC4B5FD),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DigestMetric(label = "Thời gian lướt", value = stats.formattedActiveTime)
                    DigestMetric(label = "Tổng video", value = "${stats.totalSwipes}")
                    DigestMetric(label = "Xem sâu (>2s)", value = "${stats.validViews}")
                    DigestMetric(label = "Tỉ lệ chú tâm", value = "${stats.deepViewRatioPercentage}%")
                }

                if (stats.totalCategorizedCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DigestMetric(label = "🎯 Mục tiêu", value = "${stats.goalVideosCount} (${stats.goalRatioPercentage}%)")
                        DigestMetric(label = "☕ Giải trí", value = "${stats.leisureVideosCount} (${stats.leisureRatioPercentage}%)")
                        DigestMetric(label = "❓ Chưa rõ", value = "${stats.unclassifiedVideosCount} (${stats.unclassifiedRatioPercentage}%)")
                        DigestMetric(label = "⚠️ Lạc lối", value = "${stats.distractionVideosCount} (${stats.distractionRatioPercentage}%)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── ACCORDION: NHẬT KÝ NỘI DUNG HÔM NAY ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1F0F172A))
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                // Header (Clickable toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLogExpanded = !isLogExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📑", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Nhật ký nội dung hôm nay",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${deepWatchedVideos.size} video xem sâu • $impulsiveVideosCount lướt bốc đồng",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Text(
                        text = if (isLogExpanded) "▲ Thu gọn" else "▼ Xem chi tiết",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = isLogExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        androidx.compose.material3.HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Group 1: Deep Watched Videos
                        Text(
                            text = "🎯 NỘI DUNG ĐÃ XEM SÂU (>2s)",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (deepWatchedVideos.isEmpty()) {
                            Text(
                                text = "Chưa có nội dung xem sâu nào được ghi nhận hôm nay.",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                deepWatchedVideos.forEach { video ->
                                    val timeStr = formatTime(video.timestamp)
                                    val label = when (video.category) {
                                        "goal" -> "🎯 Mục tiêu"
                                        "distraction" -> "⚠️ Lạc lối"
                                        "leisure" -> "☕ Giải trí"
                                        else -> "❓ Chưa rõ"
                                    }
                                    val tagColor = when (video.category) {
                                        "goal" -> Color(0xFF34D399)
                                        "distraction" -> Color(0xFFF87171)
                                        "leisure" -> Color(0xFF38BDF8)
                                        else -> Color(0xFF94A3B8)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x14FFFFFF))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "[$timeStr] ${video.title}",
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            color = tagColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Group 2: Impulsive Skips Summary Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1AF59E0B))
                                .border(0.8.dp, Color(0x44F59E0B), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚡", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Đã lướt qua $impulsiveVideosCount video ngắn không xem (lướt bốc đồng <2s)",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 Lưu ý: Danh sách chi tiết thô sẽ tự động xóa sau khi lưu phản tư hoặc qua 00h00 để bảo vệ bộ nhớ và tính riêng tư.",
                            color = Color(0xFF64748B),
                            fontSize = 9.5.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Star Rating Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1F1E293B))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Mức độ hài lòng về sự tập trung hôm nay của bạn:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        Text(
                            text = if (i <= rating) "★" else "☆",
                            color = if (i <= rating) Color(0xFFFBBF24) else Color(0xFF475569),
                            fontSize = 32.sp,
                            modifier = Modifier
                                .clickable { rating = i }
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lesson 1 Input
        Text(
            text = "1. Điều giá trị nhất bạn đã học / xem được hôm nay là gì?",
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = lesson1,
            onValueChange = { lesson1 = it },
            placeholder = {
                Text(
                    text = "VD: Học được kỹ thuật hít thở 12s, cách viết code Jetpack Compose sạch...",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color(0x33FFFFFF)
            ),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Lesson 2 Input
        Text(
            text = "2. Một điều bạn muốn cải thiện cho ngày mai?",
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = lesson2,
            onValueChange = { lesson2 = it },
            placeholder = {
                Text(
                    text = "VD: Không mở video ngắn sau 22h, hoàn thành 2 phiên Pomodoro...",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color(0x33FFFFFF)
            ),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Gemini AI Coach Button
        Button(
            onClick = {
                if (isLoadingAi) return@Button
                isLoadingAi = true
                coroutineScope.launch {
                    val unclassifiedVideos = stats.watchedVideos.filter { it.category == "unclassified" }
                    val result = GeminiCoachService.getReflectionFeedback(
                        stats = stats,
                        masterGoal = masterGoal,
                        lesson1 = lesson1,
                        lesson2 = lesson2,
                        apiKey = geminiApiKey,
                        unclassifiedVideos = unclassifiedVideos,
                        existingTargetKeywords = targetKeywords
                    )
                    isLoadingAi = false
                    aiResult = result

                    // Apply feedback
                    if (result != null && onApplyAiFeedback != null) {
                        onApplyAiFeedback(result)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            if (isLoadingAi) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini AI đang đúc kết...", fontSize = 13.sp)
            } else {
                Text("🤖 Nhận Lời Khuyên & Tự Học Từ Khóa (Gemini AI)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
        }

        // Gemini AI Response Card
        if (aiResult != null) {
            val res = aiResult!!
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1F8B5CF6))
                    .border(1.2.dp, Color(0xFF8B5CF6), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lời khuyên từ Trợ lý Tỉnh thức:",
                            color = Color(0xFFA78BFA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = res.coachFeedback,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    if (res.tomorrowMission.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🎯 Nhiệm vụ ngày mai: ${res.tomorrowMission}",
                            color = Color(0xFF34D399),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Reclassified Videos Summary Card
                    if (res.reclassifiedVideos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2638BDF8))
                                .border(0.8.dp, Color(0x6638BDF8), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "✨ Đã phân loại lại ${res.reclassifiedVideos.size} video chưa rõ:",
                                    color = Color(0xFF7DD3FC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                res.reclassifiedVideos.entries.take(5).forEach { entry ->
                                    val icon = when (entry.value) {
                                        "goal" -> "🎯 Mục tiêu"
                                        "distraction" -> "⚠️ Lạc lối"
                                        else -> "☕ Giải trí"
                                    }
                                    Text(
                                        text = "• \"${entry.key.take(28)}...\": $icon",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Learned Keywords Summary Card
                    val totalLearned = res.learnedTargetKeywords.size + res.learnedLeisureKeywords.size + res.learnedDistractionKeywords.size
                    if (totalLearned > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2610B981))
                                .border(0.8.dp, Color(0x6610B981), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🧠 Đã tự động học thêm $totalLearned từ khóa mới vào bộ lọc máy:",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (res.learnedTargetKeywords.isNotEmpty()) {
                                    Text(
                                        text = "🎯 Mục tiêu: ${res.learnedTargetKeywords.joinToString(", ")}",
                                        color = Color(0xFF34D399),
                                        fontSize = 10.sp
                                    )
                                }
                                if (res.learnedLeisureKeywords.isNotEmpty()) {
                                    Text(
                                        text = "☕ Giải trí: ${res.learnedLeisureKeywords.joinToString(", ")}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp
                                    )
                                }
                                if (res.learnedDistractionKeywords.isNotEmpty()) {
                                    Text(
                                        text = "⚠️ Bẫy Dopamine: ${res.learnedDistractionKeywords.joinToString(", ")}",
                                        color = Color(0xFFF87171),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Reflection Button
        Button(
            onClick = {
                onSaveReflection(lesson1, lesson2, rating)
                isSaved = true
                Toast.makeText(context, "🌟 Đã lưu phản tư, thưởng +15⚡ và nén báo cáo thành công!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSaved) Color(0xFF10B981) else Color(0xFF8B5CF6)
            )
        ) {
            Text(
                text = if (isSaved) "✓ Đã Lưu Tâm Sự Hôm Nay (+15⚡)" else "Lưu Tâm Sự Cuối Ngày",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun DigestMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "--:--"
    }
}
