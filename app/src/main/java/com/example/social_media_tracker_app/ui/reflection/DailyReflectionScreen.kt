package com.example.social_media_tracker_app.ui.reflection

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.social_media_tracker_app.data.model.DailyStats

@Composable
fun DailyReflectionDialog(
    stats: DailyStats,
    onSaveReflection: (lesson1: String, lesson2: String, rating: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var lesson1 by remember { mutableStateOf(stats.reflection.lesson1) }
    var lesson2 by remember { mutableStateOf(stats.reflection.lesson2) }
    var rating by remember { mutableIntStateOf(stats.reflection.rating) }
    var isSaved by remember { mutableStateOf(stats.reflection.submittedAt != null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xF50F172A))
                .border(1.5.dp, Color(0xFF8B5CF6), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📝", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhật Ký Phản Tư Cuối Ngày",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dành 2 phút ghi lại điều bạn học được hôm nay",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricMiniCard(
                        title = "Thời gian lướt",
                        value = stats.formattedActiveTime,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Tổng video",
                        value = "${stats.totalSwipes}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Tỉ lệ sâu",
                        value = "${stats.deepViewRatioPercentage}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Star Rating
                Text(
                    text = "Hôm nay bạn tự đánh giá bản thân mấy sao?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        Text(
                            text = if (i <= rating) "★" else "☆",
                            color = if (i <= rating) Color(0xFFFBBF24) else Color(0xFF475569),
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable { rating = i }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lesson 1
                OutlinedTextField(
                    value = lesson1,
                    onValueChange = { lesson1 = it },
                    label = { Text("1. Bài học / Nội dung tâm đắc nhất", fontSize = 11.sp) },
                    placeholder = { Text("VD: Học được cách thở Box Breathing...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Lesson 2
                OutlinedTextField(
                    value = lesson2,
                    onValueChange = { lesson2 = it },
                    label = { Text("2. Điều cần cải thiện cho ngày mai", fontSize = 11.sp) },
                    placeholder = { Text("VD: Không mở Shorts sau 22h...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Button(
                    onClick = {
                        onSaveReflection(lesson1, lesson2, rating)
                        isSaved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) Color(0xFF10B981) else Color(0xFF8B5CF6)
                    )
                ) {
                    Text(
                        text = if (isSaved) "✓ Đã Lưu Phản Tư (+15⚡)" else "Lưu Phản Tư Cuối Ngày",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF))
                ) {
                    Text(text = "Đóng", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1AFFFFFF))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 10.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
