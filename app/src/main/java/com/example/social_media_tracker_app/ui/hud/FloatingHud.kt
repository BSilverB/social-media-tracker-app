package com.example.social_media_tracker_app.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.social_media_tracker_app.data.model.ActivityState
import com.example.social_media_tracker_app.data.model.DailyStats
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.ui.pet.PetWidget
import kotlin.math.roundToInt

@Composable
fun FloatingHud(
    stats: DailyStats,
    petState: PetState,
    activityState: ActivityState,
    pomodoroLabel: String = "",
    isBreakSession: Boolean = false,
    isNavVisible: Boolean = true,
    currentVideoCategory: String? = null,
    distractionWarning: String? = null,
    onPokePet: () -> Pair<String, Boolean>,
    onTogglePomodoro: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenDashboard: () -> Unit,
    onTriggerBreathing: () -> Unit,
    onExitBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Initial position: docked to top-right
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - with(density) { 260.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(with(density) { 60.dp.toPx() }) }
    var isExpanded by remember { mutableStateOf(false) }

    // Status colors based on thresholds (M1=15, M2=30, M3=45) or Break Session
    val currentPlatformSwipes = when (activityState.platform.lowercase()) {
        "youtube" -> stats.youtube.shorts.totalSwipes
        "facebook" -> stats.facebook.reels.totalSwipes + stats.facebook.feed.feedPostsScrolled
        "tiktok" -> stats.tiktok.shorts.totalSwipes
        else -> stats.totalSwipes
    }

    val statusColor = when {
        isBreakSession -> Color(0xFF06B6D4) // Cyan Break
        currentPlatformSwipes >= 45 || stats.totalSwipes >= 45 -> Color(0xFFF43F5E) // Red Danger
        currentPlatformSwipes >= 30 || stats.totalSwipes >= 30 -> Color(0xFFF97316) // Orange Warning
        currentPlatformSwipes >= 15 || stats.totalSwipes >= 15 -> Color(0xFFF59E0B) // Yellow Notice
        else -> Color(0xFF10B981) // Emerald Safe
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(durationMillis = 400),
        label = "hudBorderColor"
    )

    // Pulsing animation for the live tracking dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Auto-collapse when user is scrolling down (YouTube style), re-expand when scrolling up
    val effectiveExpanded = isExpanded && isNavVisible

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = offsetX.roundToInt().coerceIn(10, (screenWidthPx - with(density) { 200.dp.toPx() }).roundToInt().coerceAtLeast(10)),
                    y = offsetY.roundToInt().coerceIn(20, (screenHeightPx - with(density) { 180.dp.toPx() }).roundToInt().coerceAtLeast(20))
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .shadow(
                elevation = if (isNavVisible) 10.dp else 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = statusColor.copy(alpha = 0.5f),
                ambientColor = Color.Black
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF0111827), // Frosted dark slate
                        Color(0xFA0B0F19)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                color = animatedBorderColor.copy(alpha = if (isNavVisible) 0.85f else 0.4f),
                shape = RoundedCornerShape(22.dp)
            )
            .animateContentSize(animationSpec = tween(durationMillis = 250))
            .clickable { isExpanded = !isExpanded }
            .padding(
                horizontal = if (!isNavVisible && !isExpanded) 8.dp else 10.dp,
                vertical = if (!isNavVisible && !isExpanded) 5.dp else 7.dp
            )
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Row: Pet Slot + Dynamic Context Info + Expand Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Interactive Pet
                PetWidget(
                    petState = petState,
                    onPoke = onPokePet,
                    modifier = Modifier.size(24.dp)
                )

                // Pulsing Live Indicator
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = pulseAlpha))
                )

                // When user is scrolling down and NOT expanded: render sleek minimal badge
                if (!isNavVisible && !isExpanded) {
                    Text(
                        text = "${activityState.displayIcon} $currentPlatformSwipes",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Context-Aware Content
                    PlatformActivityBanner(
                        activityState = activityState,
                        stats = stats
                    )

                    // Live Video Classification Badge
                    if (currentVideoCategory != null && (activityState.isShortVideo || activityState.isLongVideo)) {
                        val (catIcon, catBg, catColor) = when (currentVideoCategory) {
                            "goal" -> Triple("🎯", Color(0x3310B981), Color(0xFF34D399))
                            "distraction" -> Triple("⚠️", Color(0x33EF4444), Color(0xFFF87171))
                            "unclassified" -> Triple("❓", Color(0x33F59E0B), Color(0xFFFBBF24))
                            else -> Triple("☕", Color(0x330284C7), Color(0xFF38BDF8))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(catBg)
                                .border(0.8.dp, catColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = catIcon,
                                color = catColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Pomodoro Label if active
                    if (pomodoroLabel.isNotBlank()) {
                        Text(
                            text = pomodoroLabel,
                            color = if (isBreakSession) Color(0xFF06B6D4) else Color(0xFFA78BFA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Expand / Collapse Chevron indicator
                    Text(
                        text = if (effectiveExpanded) "▲" else "▼",
                        color = Color(0xFF64748B),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            // Expanded Details View & Mindful Control Center
            AnimatedVisibility(
                visible = effectiveExpanded,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(260.dp)
                ) {
                    HorizontalDivider(
                        color = Color(0x2EFFFFFF),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Distraction Warning Intervention Banner
                    if (!distractionWarning.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33EF4444))
                                .border(1.dp, Color(0x66EF4444), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = distractionWarning,
                                color = Color(0xFFFCA5A5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Platform Details
                    ExpandedPlatformMetrics(
                        activityState = activityState,
                        stats = stats
                    )

                    // Video Alignment Metric
                    if (currentVideoCategory != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val catLabel = when (currentVideoCategory) {
                            "goal" -> "🎯 Phục vụ mục tiêu"
                            "distraction" -> "⚠️ Lạc lối"
                            "unclassified" -> "❓ Chưa phân loại"
                            else -> "☕ Giải trí lành mạnh"
                        }
                        val catColor = when (currentVideoCategory) {
                            "goal" -> Color(0xFF34D399)
                            "distraction" -> Color(0xFFF87171)
                            "unclassified" -> Color(0xFFFBBF24)
                            else -> Color(0xFF38BDF8)
                        }
                        MetricRow("🏷️ Video hiện tại", catLabel, catColor)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pet Status Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Năng lượng: ${petState.energy}/100",
                            color = Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "🔥 Chuỗi: ${petState.streakDays} ngày",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action buttons grid (Pomodoro, Wardrobe, Reflection, Box Breathing)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HudActionButton(
                            icon = if (pomodoroLabel.isNotBlank()) "⏹" else "⏱",
                            label = "Pomodoro",
                            onClick = onTogglePomodoro,
                            modifier = Modifier.weight(1f)
                        )

                        HudActionButton(
                            icon = "👗",
                            label = "Tủ Đồ",
                            onClick = onOpenWardrobe,
                            modifier = Modifier.weight(1f)
                        )

                        HudActionButton(
                            icon = "📝",
                            label = "Phản Tư",
                            onClick = onOpenReflection,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Secondary row: Box Breathing & Exit
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
                                .clickable { onTriggerBreathing() }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🧘 Thở 12s",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Subtle Exit Button inside HUD
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33EF4444))
                                .clickable { onExitBrowser() }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🚪 Rời Web",
                                color = Color(0xFFFCA5A5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact pill banner displaying tailored metrics based on the current platform and user activity.
 */
@Composable
private fun PlatformActivityBanner(
    activityState: ActivityState,
    stats: DailyStats
) {
    val platform = activityState.platform.lowercase()

    when (platform) {
        "youtube" -> {
            val yt = stats.youtube
            when {
                activityState.isShortVideo -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎬 Shorts: ", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${yt.shorts.totalSwipes}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = " | ", color = Color(0xFF475569), fontSize = 11.sp)
                        Text(text = "Sâu: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(text = "${yt.shorts.validViews}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (yt.shorts.loopViews > 0) {
                            Text(text = " 🔁${yt.shorts.loopViews}", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                activityState.isLongVideo -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎬 Video: ", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${yt.longVideos.totalWatched} xem", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = " | ", color = Color(0xFF475569), fontSize = 11.sp)
                        Text(text = "🎯 ${yt.longVideos.usefulCount}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎬 YouTube: ", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = yt.summary.formattedActiveTime, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = " | 🔄 ${yt.summary.reloadCount}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }

        "facebook" -> {
            val fb = stats.facebook
            when {
                activityState.isFeed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👥 Feed: ", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${fb.feed.feedPostsScrolled}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = " | ", color = Color(0xFF475569), fontSize = 11.sp)
                        Text(text = "Đọc: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(text = "${fb.feed.feedPostsRead}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                activityState.isShortVideo -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👥 Reels: ", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${fb.reels.totalSwipes}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = " | ", color = Color(0xFF475569), fontSize = 11.sp)
                        Text(text = "Sâu: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(text = "${fb.reels.validViews}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                activityState.isLongVideo -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👥 Watch: ", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${fb.longVideos.totalWatched} xem", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = " | 🎯 ${fb.longVideos.usefulCount}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👥 FB: ", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = fb.summary.formattedActiveTime, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = " | 🔄 ${fb.summary.reloadCount}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }

        "tiktok" -> {
            val tt = stats.tiktok
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎵 TikTok: ", color = Color(0xFF2DD4BF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "${tt.shorts.totalSwipes}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = " | ", color = Color(0xFF475569), fontSize = 11.sp)
                Text(text = "Sâu: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(text = "${tt.shorts.validViews}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (tt.shorts.loopViews > 0) {
                    Text(text = " 🔁${tt.shorts.loopViews}", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        else -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌐 Lướt: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(text = "${stats.totalSwipes}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = " | Sâu: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(text = "${stats.validViews}", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Detailed platform statistics in expanded view.
 */
@Composable
private fun ExpandedPlatformMetrics(
    activityState: ActivityState,
    stats: DailyStats
) {
    val platform = activityState.platform.lowercase()

    when (platform) {
        "youtube" -> {
            val yt = stats.youtube
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MetricRow("⏱ Chủ động / Bị động", "${yt.summary.formattedActiveTime} / ${yt.summary.formattedPassiveTime}", Color(0xFF38BDF8))
                MetricRow("🔄 Số lần Reload (F5)", "${yt.summary.reloadCount} lần", Color(0xFF94A3B8))
                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                MetricRow("⚡ Shorts lướt / Xem sâu", "${yt.shorts.totalSwipes} / ${yt.shorts.validViews}", Color(0xFF34D399))
                MetricRow("🏃 Shorts bốc đồng / Lặp", "${yt.shorts.impulsiveCount} / ${yt.shorts.loopViews}", Color(0xFFFBBF24))
                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                MetricRow("🎬 Video dài đã xem", "${yt.longVideos.totalWatched} video", Color(0xFFA78BFA))
                MetricRow("🎯 Video thực xem / Bốc đồng", "${yt.longVideos.usefulCount} / ${yt.longVideos.impulsiveCount}", Color(0xFF34D399))
            }
        }

        "facebook" -> {
            val fb = stats.facebook
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MetricRow("⏱ Chủ động / Bị động", "${fb.summary.formattedActiveTime} / ${fb.summary.formattedPassiveTime}", Color(0xFF38BDF8))
                MetricRow("🔄 Số lần Reload (F5)", "${fb.summary.reloadCount} lần", Color(0xFF94A3B8))
                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                MetricRow("📜 Bảng tin lướt / Đọc sâu", "${fb.feed.feedPostsScrolled} / ${fb.feed.feedPostsRead}", Color(0xFF60A5FA))
                MetricRow("⚡ Reels lướt / Xem sâu", "${fb.reels.totalSwipes} / ${fb.reels.validViews}", Color(0xFF34D399))
                MetricRow("🏃 Reels bốc đồng / Lặp", "${fb.reels.impulsiveCount} / ${fb.reels.loopViews}", Color(0xFFFBBF24))
                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                MetricRow("🎬 Watch đã xem / Hữu ích", "${fb.longVideos.totalWatched} / ${fb.longVideos.usefulCount}", Color(0xFFA78BFA))
            }
        }

        "tiktok" -> {
            val tt = stats.tiktok
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MetricRow("⏱ Thời gian sử dụng", tt.summary.formattedActiveTime, Color(0xFF38BDF8))
                MetricRow("🔄 Số lần Reload", "${tt.summary.reloadCount} lần", Color(0xFF94A3B8))
                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.5.dp)
                MetricRow("⚡ TikToks lướt / Xem sâu", "${tt.shorts.totalSwipes} / ${tt.shorts.validViews}", Color(0xFF34D399))
                MetricRow("🏃 Xem bốc đồng / Lặp lại", "${tt.shorts.impulsiveCount} / ${tt.shorts.loopViews}", Color(0xFFFBBF24))
            }
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MetricRow("⏱ Hoạt động", stats.formattedActiveTime, Color(0xFF38BDF8))
                MetricRow("⚡ Tổng lượt lướt", "${stats.totalSwipes}", Color.White)
                MetricRow("👁 Xem sâu (>=2s)", "${stats.validViews}", Color(0xFF34D399))
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        Text(text = value, color = valueColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HudActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x22FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
