package com.example.social_media_tracker_app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.social_media_tracker_app.data.model.DailyStats
import com.example.social_media_tracker_app.data.model.formatSeconds
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.domain.pomodoro.PomodoroState
import com.example.social_media_tracker_app.ui.overlay.PomodoroSetupDialog
import com.example.social_media_tracker_app.ui.overlay.VideoAuditLogDialog
import com.example.social_media_tracker_app.ui.pet.PetWidget

enum class PlatformTab(val title: String, val icon: String, val color: Color) {
    YOUTUBE("YouTube", "🎬", Color(0xFFEF4444)),
    FACEBOOK("Facebook", "👥", Color(0xFF3B82F6)),
    TIKTOK("TikTok", "🎵", Color(0xFF06B6D4))
}

@Composable
fun HomeScreen(
    stats: DailyStats,
    petState: PetState,
    pomodoroState: PomodoroState,
    appConfig: com.example.social_media_tracker_app.data.model.AppConfig = com.example.social_media_tracker_app.data.model.AppConfig(),
    onPokePet: () -> Pair<String, Boolean>,
    onOpenWardrobe: () -> Unit,
    onTogglePomodoro: () -> Unit,
    onStartPomodoroSession: ((totalCycles: Int, focusMinutes: Int, breakMinutes: Int, focusMode: String) -> Unit)? = null,
    onTriggerBreathing: () -> Unit,
    onLaunchPlatform: (url: String, platformName: String) -> Unit,
    onUpdateVideoCategory: (videoId: String, platform: String, newCategory: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedPlatformTab by remember { mutableStateOf(PlatformTab.YOUTUBE) }
    var showVideoAuditDialog by remember { mutableStateOf(false) }
    var showPomodoroSetupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // App Header: Title & Streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mindful Hub",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Sử dụng mạng xã hội thông minh",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            // Streak Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x26FBBF24))
                    .border(1.dp, Color(0x66FBBF24), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🔥 ${petState.streakDays} Ngày",
                    color = Color(0xFFFBBF24),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Spacer(modifier = Modifier.height(14.dp))

        // ─── PET MASCOT HERO STAGE & SPEECH BUBBLE (MATCHING USER SKETCH) ───
        val petQuotesHigh = remember {
            listOf(
                "Bạn đang làm chủ tâm trí rất tốt! Tiếp tục phát huy nhé 🌱",
                "Tớ tràn đầy năng lượng! Cùng chánh niệm nào 🌟",
                "Xem có chọn lọc là cách tốt nhất để bảo vệ não bộ ✨",
                "Mỗi giây phút chú tâm là một món quà cho tâm trí 🎁"
            )
        }
        val petQuotesMedium = remember {
            listOf(
                "Cẩn thận bẫy dopamine đang chực chờ đấy nhé ☕",
                "Hãy hít thở sâu một nhịp trước khi quẹt tiếp nào 🌿",
                "Bạn đã lướt kha khá rồi, nhớ chú ý thời gian nhé!",
                "Đừng để thuật toán cuốn bạn đi quá xa mục tiêu 🎯"
            )
        }
        val petQuotesLow = remember {
            listOf(
                "Tớ sắp kiệt sức rồi... Hãy đặt điện thoại xuống nghỉ ngơi đi 🛑",
                "Cứu tớ với! Hãy tập thở 12s để hồi sinh lực nào 😢",
                "Năng lượng cạn rồi! Hãy rời mạng xã hội ngay thôi 💤"
            )
        }
        var speechIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
        var speechText by remember(petState.energy) {
            mutableStateOf(
                when {
                    petState.energy >= 70 -> petQuotesHigh[0]
                    petState.energy >= 40 -> petQuotesMedium[0]
                    else -> petQuotesLow[0]
                }
            )
        }

        val moodColor = when {
            petState.energy >= 70 -> Color(0xFF10B981)
            petState.energy >= 40 -> Color(0xFFFBBF24)
            else -> Color(0xFFEF4444)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row with Speech Bubble aligned over/to the right of Pet (As shown in sketch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.5.dp, Color(0xFFC4B5FD), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💬", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = speechText,
                            color = Color(0xFF1E1B4B),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Center Pet sitting right on top of the stage
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(moodColor.copy(alpha = 0.35f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                PetWidget(
                    petState = petState,
                    onPoke = {
                        val currentList = when {
                            petState.energy >= 70 -> petQuotesHigh
                            petState.energy >= 40 -> petQuotesMedium
                            else -> petQuotesLow
                        }
                        speechIndex = (speechIndex + 1) % currentList.size
                        speechText = currentList[speechIndex]
                        onPokePet()
                    },
                    modifier = Modifier.size(80.dp)
                )
            }

            // Stage Bar Box (Horizontal container resting under Pet)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = Color(0x448B5CF6))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                        )
                    )
                    .border(1.2.dp, Color(0x668B5CF6), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🐾 LINH VẬT TỈNH THỨC",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${petState.energy}/100 ⚡",
                            color = moodColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { petState.energy / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.5.dp)),
                        color = moodColor,
                        trackColor = Color(0x33FFFFFF)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                petState.energy >= 70 -> "Trạng thái: Tràn đầy sức sống"
                                petState.energy >= 40 -> "Trạng thái: Cần chánh niệm"
                                else -> "Trạng thái: Cảnh báo kiệt sức"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onTriggerBreathing,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6634D399)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "🧘 Thở 12s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onOpenWardrobe,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA78BFA)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66A78BFA)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "👗 Tủ Đồ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pomodoro Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1F6366F1))
                .border(1.dp, Color(0x446366F1), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = "⏱", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Chu kỳ Pomodoro",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (pomodoroState.isRunning) {
                                val modeLabel = if (pomodoroState.focusMode == "music") "🎵 Nhạc" else "📚 Học tập"
                                "${pomodoroState.statusLabel} • $modeLabel"
                            } else "Chưa kích hoạt (Nhấn Thiết lập để chọn chu kỳ & chế độ)",
                            color = if (pomodoroState.isRunning) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pomodoroState.isRunning) {
                            onTogglePomodoro()
                        } else {
                            showPomodoroSetupDialog = true
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pomodoroState.isRunning) Color(0xFFEF4444) else Color(0xFF6366F1)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (pomodoroState.isRunning) "Tạm dừng" else "Thiết lập",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Granular Platform Statistics
        Text(
            text = "THỐNG KÊ CHI TIẾT TỪNG NỀN TẢNG HÔM NAY",
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Platform Selector Tabs: YouTube | Facebook | TikTok
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1F1E293B))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PlatformTab.values().forEach { tab ->
                val isSelected = tab == selectedPlatformTab
                val tabBackground by animateColorAsState(
                    targetValue = if (isSelected) tab.color.copy(alpha = 0.25f) else Color.Transparent,
                    animationSpec = tween(250),
                    label = "tabBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) tab.color else Color.Transparent,
                    animationSpec = tween(250),
                    label = "tabBorder"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(tabBackground)
                        .border(1.dp, borderColor, RoundedCornerShape(9.dp))
                        .clickable { selectedPlatformTab = tab }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = tab.icon, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Granular Content for Selected Platform
        when (selectedPlatformTab) {
            PlatformTab.YOUTUBE -> {
                YoutubeStatsSection(
                    yt = stats.youtube,
                    thresholds = appConfig.thresholds.youtube,
                    watchedVideos = stats.watchedVideos,
                    onLaunch = { onLaunchPlatform("https://m.youtube.com", "YouTube") }
                )
            }
            PlatformTab.FACEBOOK -> {
                FacebookStatsSection(
                    fb = stats.facebook,
                    thresholds = appConfig.thresholds.facebook,
                    watchedVideos = stats.watchedVideos,
                    onLaunch = { onLaunchPlatform("https://m.facebook.com", "Facebook") }
                )
            }
            PlatformTab.TIKTOK -> {
                TiktokStatsSection(
                    tt = stats.tiktok,
                    thresholds = appConfig.thresholds.tiktok,
                    onLaunch = { onLaunchPlatform("https://www.tiktok.com", "TikTok") }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Content Alignment & Video Distribution Card ───
        Text(
            text = "PHÂN BỐ GIÁ TRỊ NỘI DUNG HÔM NAY",
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                    )
                )
                .border(1.2.dp, Color(0x668B5CF6), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header & Total Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Đánh Giá Mục Tiêu",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stats.totalCategorizedCount} video đã theo dõi",
                        color = Color(0xFFA78BFA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented Progress Bar
                if (stats.totalCategorizedCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        if (stats.goalVideosCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(stats.goalVideosCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFF10B981))
                            )
                        }
                        if (stats.leisureVideosCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(stats.leisureVideosCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFF0EA5E9))
                            )
                        }
                        if (stats.unclassifiedVideosCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(stats.unclassifiedVideosCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                        if (stats.distractionVideosCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(stats.distractionVideosCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFFF43F5E))
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x22FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Chưa có dữ liệu video", color = Color(0xFF64748B), fontSize = 8.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4 Metrics Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Goal
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(text = "🎯 Mục tiêu", color = Color(0xFF34D399), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${stats.goalVideosCount} (${stats.goalRatioPercentage}%)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Leisure
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "☕ Giải trí", color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${stats.leisureVideosCount} (${stats.leisureRatioPercentage}%)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Unclassified
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "❓ Chưa rõ", color = Color(0xFFFBBF24), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${stats.unclassifiedVideosCount} (${stats.unclassifiedRatioPercentage}%)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Distraction
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "⚠️ Lạc lối", color = Color(0xFFF87171), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${stats.distractionVideosCount} (${stats.distractionRatioPercentage}%)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Leisure Quota Note
                val usedLeisureMinutes = stats.totalLeisureWatchedSeconds / 60
                val quota = appConfig.leisureQuotaMinutes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1F000000))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quỹ giải trí hôm nay:",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${usedLeisureMinutes}p / ${quota}p",
                        color = if (usedLeisureMinutes <= quota) Color(0xFF38BDF8) else Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button to open VideoAuditLogDialog
                OutlinedButton(
                    onClick = { showVideoAuditDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA78BFA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66A78BFA))
                ) {
                    Text(
                        text = "📋 Xem & Ghi Đè Nhật Ký Video (${stats.totalCategorizedCount})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Breathing Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
                .clickable { onTriggerBreathing() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🧘 Thở Box Breathing (12s) - Hạ Dopamine Ngay",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Video Audit Log Modal Dialog
    if (showVideoAuditDialog) {
        VideoAuditLogDialog(
            stats = stats,
            onUpdateCategory = onUpdateVideoCategory,
            onDismiss = { showVideoAuditDialog = false }
        )
    }

    // Pomodoro Multi-Cycle Setup Dialog
    if (showPomodoroSetupDialog) {
        PomodoroSetupDialog(
            petState = petState,
            onStartPomodoro = { cycles, focus, brk, mode ->
                showPomodoroSetupDialog = false
                onStartPomodoroSession?.invoke(cycles, focus, brk, mode)
            },
            onDismiss = { showPomodoroSetupDialog = false }
        )
    }
}

// ─── Sub-section: YouTube Granular Stats ─────────────────────────────────────
@Composable
private fun YoutubeStatsSection(
    yt: com.example.social_media_tracker_app.data.model.YoutubeStats,
    thresholds: com.example.social_media_tracker_app.data.model.YoutubeThresholds,
    watchedVideos: List<com.example.social_media_tracker_app.data.model.WatchedVideoItem>,
    onLaunch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Tổng quan YouTube
        SectionCard(title = "📊 TỔNG QUAN YOUTUBE", accentColor = Color(0xFFEF4444)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Chủ động", value = yt.summary.formattedActiveTime, color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                MetricItem(label = "Bị động", value = yt.summary.formattedPassiveTime, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                MetricItem(label = "Reload F5", value = "${yt.summary.reloadCount} lần", color = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
            }
        }

        // 2. YouTube Shorts Card
        SectionCard(
            title = "📱 YouTube Shorts",
            accentColor = Color(0xFFEF4444),
            badgeText = "Lướt nhanh",
            badgeColor = Color(0xFF38BDF8),
            badgeBgColor = Color(0x3338BDF8)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Tổng lướt", value = "${yt.shorts.totalSwipes}", color = Color(0xFFA78BFA), modifier = Modifier.weight(1f))
                MetricItem(label = "Xem sâu (>2s)", value = "${yt.shorts.validViews}", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                MetricItem(label = "Lặp lại (Loop)", value = "${yt.shorts.loopViews}", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            MilestoneProgressWidget(
                current = yt.shorts.totalSwipes,
                m1 = thresholds.shorts.m1,
                m2 = thresholds.shorts.m2,
                m3 = thresholds.shorts.m3,
                unit = "lượt"
            )
        }

        // 3. Video dài (/watch) Card
        val ytLongTotal = yt.longVideos.totalWatched
        val ytImpulsiveRate = if (ytLongTotal > 0) ((yt.longVideos.impulsiveCount * 100) / ytLongTotal) else 0
        SectionCard(
            title = "🎬 Video dài (/watch)",
            accentColor = Color(0xFFEF4444),
            badgeText = "Lướt vội: $ytImpulsiveRate%",
            badgeColor = Color(0xFFEF4444),
            badgeBgColor = Color(0x33EF4444)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Đã xem", value = "$ytLongTotal", color = Color.White, modifier = Modifier.weight(1f))
                MetricItem(label = "Thực xem (≥80%)", value = "${yt.longVideos.usefulCount}", color = Color(0xFF34D399), modifier = Modifier.weight(1f))
                MetricItem(label = "Bỏ dở (<15%)", value = "${yt.longVideos.impulsiveCount}", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            MilestoneProgressWidget(
                current = ytLongTotal,
                m1 = thresholds.long.m1,
                m2 = thresholds.long.m2,
                m3 = thresholds.long.m3,
                unit = "video"
            )
            Spacer(modifier = Modifier.height(8.dp))
            val ytRecentVideos = watchedVideos.filter { it.platform.equals("youtube", ignoreCase = true) }.take(4)
            if (ytRecentVideos.isEmpty()) {
                Text(
                    text = "Chưa có video dài nào được ghi nhận hôm nay.",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ytRecentVideos.forEach { vid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = vid.title.ifEmpty { "Video không tên" },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatSeconds(vid.watchedSeconds.toLong()),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.5.sp
                                )
                            }
                            val (catText, catColor) = when (vid.category) {
                                "goal" -> "🎯 Mục tiêu" to Color(0xFF10B981)
                                "distraction" -> "⚠️ Bỏ ngang" to Color(0xFFEF4444)
                                else -> "☕ Giải trí" to Color(0xFF38BDF8)
                            }
                            Text(
                                text = catText,
                                color = catColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Nhạc Pomodoro Focus
        if (yt.musicVideos.totalWatched > 0) {
            SectionCard(
                title = "🎵 Nhạc Tập Trung (Music Focus)",
                accentColor = Color(0xFFA78BFA),
                badgeText = "Không tính vào giới hạn",
                badgeColor = Color(0xFFC4B5FD),
                badgeBgColor = Color(0x338B5CF6)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricItem(label = "Bài hát", value = "${yt.musicVideos.totalWatched}", color = Color(0xFFA78BFA), modifier = Modifier.weight(1f))
                    MetricItem(label = "Thời lượng", value = "${yt.musicVideos.totalDurationSeconds / 60}m ${yt.musicVideos.totalDurationSeconds % 60}s", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                }
            }
        }

        // Action launch button
        PlatformLaunchButton(
            platformName = "YouTube",
            icon = "🎬",
            buttonColor = Color(0xFFEF4444),
            onClick = onLaunch
        )
    }
}

// ─── Sub-section: Facebook Granular Stats ────────────────────────────────────
@Composable
private fun FacebookStatsSection(
    fb: com.example.social_media_tracker_app.data.model.FacebookStats,
    thresholds: com.example.social_media_tracker_app.data.model.FacebookThresholds,
    watchedVideos: List<com.example.social_media_tracker_app.data.model.WatchedVideoItem>,
    onLaunch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Tổng quan Facebook
        SectionCard(title = "📊 TỔNG QUAN FACEBOOK", accentColor = Color(0xFF3B82F6)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Chủ động", value = fb.summary.formattedActiveTime, color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                MetricItem(label = "Bị động", value = fb.summary.formattedPassiveTime, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                MetricItem(label = "Reload F5", value = "${fb.summary.reloadCount} lần", color = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
            }
        }

        // 2. Hành vi Cuộn News Feed
        SectionCard(
            title = "📜 Hành vi Cuộn News Feed",
            accentColor = Color(0xFF3B82F6),
            badgeText = "Feed Unit",
            badgeColor = Color(0xFF60A5FA),
            badgeBgColor = Color(0x333B82F6)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Bài viết đã cuộn qua", value = "${fb.feed.feedPostsScrolled}", color = Color(0xFF60A5FA), modifier = Modifier.weight(1f))
                MetricItem(label = "Click Logo / F5", value = "${fb.summary.reloadCount}", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            MilestoneProgressWidget(
                current = fb.feed.feedPostsScrolled,
                m1 = thresholds.feeds.m1,
                m2 = thresholds.feeds.m2,
                m3 = thresholds.feeds.m3,
                unit = "bài"
            )
        }

        // 3. Facebook Reels Card
        SectionCard(
            title = "🎥 Facebook Reels",
            accentColor = Color(0xFF3B82F6),
            badgeText = "Lướt nhanh",
            badgeColor = Color(0xFF38BDF8),
            badgeBgColor = Color(0x3338BDF8)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Tổng lướt", value = "${fb.reels.totalSwipes}", color = Color(0xFFA78BFA), modifier = Modifier.weight(1f))
                MetricItem(label = "Xem sâu (>2s)", value = "${fb.reels.validViews}", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                MetricItem(label = "Lặp lại (Loop)", value = "${fb.reels.loopViews}", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            MilestoneProgressWidget(
                current = fb.reels.totalSwipes,
                m1 = thresholds.reels.m1,
                m2 = thresholds.reels.m2,
                m3 = thresholds.reels.m3,
                unit = "lượt"
            )
        }

        // 4. Video FB Watch Card
        val fbLongTotal = fb.longVideos.totalWatched
        val fbImpulsiveRate = if (fbLongTotal > 0) ((fb.longVideos.impulsiveCount * 100) / fbLongTotal) else 0
        SectionCard(
            title = "📺 Video FB Watch",
            accentColor = Color(0xFF3B82F6),
            badgeText = "Lướt vội: $fbImpulsiveRate%",
            badgeColor = Color(0xFFEF4444),
            badgeBgColor = Color(0x33EF4444)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Đã xem", value = "$fbLongTotal", color = Color.White, modifier = Modifier.weight(1f))
                MetricItem(label = "Thực xem (≥80%)", value = "${fb.longVideos.usefulCount}", color = Color(0xFF34D399), modifier = Modifier.weight(1f))
                MetricItem(label = "Bỏ dở (<15%)", value = "${fb.longVideos.impulsiveCount}", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            MilestoneProgressWidget(
                current = fbLongTotal,
                m1 = thresholds.long.m1,
                m2 = thresholds.long.m2,
                m3 = thresholds.long.m3,
                unit = "video"
            )
            Spacer(modifier = Modifier.height(8.dp))
            val fbRecentVideos = watchedVideos.filter { it.platform.equals("facebook", ignoreCase = true) }.take(4)
            if (fbRecentVideos.isEmpty()) {
                Text(
                    text = "Chưa có video Watch nào được ghi nhận hôm nay.",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    fbRecentVideos.forEach { vid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = vid.title.ifEmpty { "Video không tên" },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatSeconds(vid.watchedSeconds.toLong()),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.5.sp
                                )
                            }
                            val (catText, catColor) = when (vid.category) {
                                "goal" -> "🎯 Mục tiêu" to Color(0xFF10B981)
                                "distraction" -> "⚠️ Bỏ dở" to Color(0xFFEF4444)
                                else -> "☕ Giải trí" to Color(0xFF38BDF8)
                            }
                            Text(
                                text = catText,
                                color = catColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action launch button
        PlatformLaunchButton(
            platformName = "Facebook",
            icon = "👥",
            buttonColor = Color(0xFF3B82F6),
            onClick = onLaunch
        )
    }
}

// ─── Sub-section: TikTok Granular Stats ──────────────────────────────────────
@Composable
private fun TiktokStatsSection(
    tt: com.example.social_media_tracker_app.data.model.TiktokStats,
    thresholds: com.example.social_media_tracker_app.data.model.TiktokThresholds,
    onLaunch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Tổng quan TikTok
        SectionCard(title = "📊 TỔNG QUAN TIKTOK", accentColor = Color(0xFF06B6D4)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Chủ động", value = tt.summary.formattedActiveTime, color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                MetricItem(label = "Bị động", value = tt.summary.formattedPassiveTime, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                MetricItem(label = "Reload", value = "${tt.summary.reloadCount} lần", color = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
            }
        }

        // 2. TikTok Shorts Card
        SectionCard(
            title = "🎵 TikTok Shorts",
            accentColor = Color(0xFF06B6D4),
            badgeText = "Lướt nhanh",
            badgeColor = Color(0xFF38BDF8),
            badgeBgColor = Color(0x3338BDF8)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricItem(label = "Tổng lướt", value = "${tt.shorts.totalSwipes}", color = Color(0xFFA78BFA), modifier = Modifier.weight(1f))
                MetricItem(label = "Xem sâu (>2s)", value = "${tt.shorts.validViews}", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                MetricItem(label = "Lặp lại (Loop)", value = "${tt.shorts.loopViews}", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            MilestoneProgressWidget(
                current = tt.shorts.totalSwipes,
                m1 = thresholds.shorts.m1,
                m2 = thresholds.shorts.m2,
                m3 = thresholds.shorts.m3,
                unit = "lượt"
            )
        }

        // Action launch button
        PlatformLaunchButton(
            platformName = "TikTok",
            icon = "🎵",
            buttonColor = Color(0xFF06B6D4),
            onClick = onLaunch
        )
    }
}

@Composable
fun MilestoneProgressWidget(
    current: Int,
    m1: Int,
    m2: Int,
    m3: Int,
    unit: String
) {
    val max = (m3 * 1.15f).coerceAtLeast(1f)
    val progress = (current.toFloat() / max).coerceIn(0f, 1f)

    val (status, statusColor, barColor) = when {
        current >= m3 -> Triple("Trần đỏ M3", Color(0xFFEF4444), Color(0xFFEF4444))
        current >= m2 -> Triple("Cảnh báo M2", Color(0xFFF97316), Color(0xFFF97316))
        current >= m1 -> Triple("Nhắc nhở M1", Color(0xFFF59E0B), Color(0xFFF59E0B))
        else -> Triple("An toàn", Color(0xFF10B981), Color(0xFF10B981))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x4D000000))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Tiến trình Mốc giới hạn",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$current / $m1 $unit ",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "($status)",
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = Color(0x1FFFFFFF)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Milestone ticks: 0, M1, M2, M3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0", color = Color(0xFF64748B), fontSize = 9.sp)
                Text(text = "M1: $m1", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "M2: $m2", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "M3: $m3", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ─── Reusable UI Components ──────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    accentColor: Color,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF38BDF8),
    badgeBgColor: Color = Color(0x3338BDF8),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F161E2E))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1AFFFFFF))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Column {
            Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlatformLaunchButton(
    platformName: String,
    icon: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(buttonColor.copy(alpha = 0.18f))
            .border(1.2.dp, buttonColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Mở $platformName Có Kiểm Soát",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Được bảo vệ bởi bộ đếm & ngắt nhịp",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(buttonColor)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Vào Lướt ➔",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
