package com.example.social_media_tracker_app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.window.Dialog
import com.example.social_media_tracker_app.data.model.DailyStats
import com.example.social_media_tracker_app.data.model.WatchedVideoItem
import com.example.social_media_tracker_app.data.model.formatSeconds

enum class VideoCategoryFilter(val label: String, val categoryKey: String?) {
    ALL("Tất cả", null),
    UNCLASSIFIED("❓ Chưa rõ", "unclassified"),
    GOAL("🎯 Mục tiêu", "goal"),
    LEISURE("☕ Giải trí", "leisure"),
    DISTRACTION("⚠️ Lạc lối", "distraction")
}

@Composable
fun VideoAuditLogDialog(
    stats: DailyStats,
    onUpdateCategory: (videoId: String, platform: String, newCategory: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(VideoCategoryFilter.ALL) }

    val filteredVideos = remember(stats.watchedVideos, selectedFilter) {
        when (selectedFilter) {
            VideoCategoryFilter.ALL -> stats.watchedVideos
            else -> stats.watchedVideos.filter { it.category == selectedFilter.categoryKey }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF51E1B4B), // Deep indigo
                            Color(0xFA0F172A)  // Dark slate
                        )
                    )
                )
                .border(1.5.dp, Color(0xFF8B5CF6), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📋 Nhật Ký Video Hôm Nay",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Đánh giá mức độ phục vụ mục tiêu & gán nhãn thủ công",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ratio Summary Pill Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22000000))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 ${stats.goalVideosCount}",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "☕ ${stats.leisureVideosCount}",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "⚠️ ${stats.distractionVideosCount}",
                        color = Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "❓ ${stats.unclassifiedVideosCount}",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    VideoCategoryFilter.values().forEach { filter ->
                        val isSelected = filter == selectedFilter
                        val count = when (filter) {
                            VideoCategoryFilter.ALL -> stats.totalCategorizedCount
                            VideoCategoryFilter.UNCLASSIFIED -> stats.unclassifiedVideosCount
                            VideoCategoryFilter.GOAL -> stats.goalVideosCount
                            VideoCategoryFilter.LEISURE -> stats.leisureVideosCount
                            VideoCategoryFilter.DISTRACTION -> stats.distractionVideosCount
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF8B5CF6) else Color(0x22FFFFFF))
                                .clickable { selectedFilter = filter }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${filter.label}\n($count)",
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // List of Watched Videos
                if (filteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có video nào trong mục này.\nHãy lướt có chủ đích để ghi nhận dữ liệu!",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredVideos, key = { "${it.platform}_${it.id}_${it.timestamp}" }) { video ->
                            VideoAuditItemRow(
                                item = video,
                                onUpdateCategory = { newCategory ->
                                    onUpdateCategory(video.id, video.platform, newCategory)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text(
                        text = "Đóng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoAuditItemRow(
    item: WatchedVideoItem,
    onUpdateCategory: (String) -> Unit
) {
    var showCategoryMenu by remember { mutableStateOf(false) }

    val platformIcon = when (item.platform.lowercase()) {
        "youtube" -> "🎬"
        "facebook" -> "👥"
        "tiktok" -> "🎵"
        else -> "🌐"
    }

    val (badgeText, badgeBg, badgeTextColor) = when (item.category) {
        "goal" -> Triple("🎯 Mục tiêu", Color(0x3310B981), Color(0xFF34D399))
        "distraction" -> Triple("⚠️ Lạc lối", Color(0x33EF4444), Color(0xFFF87171))
        "unclassified" -> Triple("❓ Chưa rõ", Color(0x33F59E0B), Color(0xFFFBBF24))
        else -> Triple("☕ Giải trí", Color(0x330284C7), Color(0xFF38BDF8))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1F1E293B))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Platform Icon
            Text(
                text = platformIcon,
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Video Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.channel,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = " • ", color = Color(0xFF475569), fontSize = 10.sp)
                    Text(
                        text = formatSeconds(item.watchedSeconds.toLong()),
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.userOverridden) {
                        Text(
                            text = " (Đã sửa)",
                            color = Color(0xFFA78BFA),
                            fontSize = 9.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Clickable Category Badge
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeTextColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { showCategoryMenu = true }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text("🎯 Phục vụ mục tiêu", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showCategoryMenu = false
                            onUpdateCategory("goal")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("☕ Giải trí lành mạnh", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showCategoryMenu = false
                            onUpdateCategory("leisure")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⚠️ Bẫy Dopamine / Lạc lối", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showCategoryMenu = false
                            onUpdateCategory("distraction")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("❓ Cần xem xét / Chưa rõ", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showCategoryMenu = false
                            onUpdateCategory("unclassified")
                        }
                    )
                }
            }
        }
    }
}
