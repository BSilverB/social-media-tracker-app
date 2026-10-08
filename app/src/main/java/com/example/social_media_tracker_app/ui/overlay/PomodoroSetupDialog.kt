package com.example.social_media_tracker_app.ui.overlay

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.ui.pet.PetWidget

@Composable
fun PomodoroSetupDialog(
    petState: PetState,
    onStartPomodoro: (totalCycles: Int, focusMinutes: Int, breakMinutes: Int, focusMode: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCycles by remember { mutableIntStateOf(2) }
    var selectedFocus by remember { mutableIntStateOf(25) }
    var selectedBreak by remember { mutableIntStateOf(5) }
    var selectedMode by remember { mutableStateOf("music") } // "music" | "study"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF131B2E))
                .border(1.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with Pet Mascot
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x228B5CF6)),
                    contentAlignment = Alignment.Center
                ) {
                    PetWidget(
                        petState = petState,
                        onPoke = { Pair("", false) },
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "🍅 Thiết Lập Chu Kỳ Pomodoro",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Chọn số hiệp và phương thức tập trung phù hợp nhất",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // 1. Chọn số hiệp Pomodoro
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Số hiệp Pomodoro:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 4).forEach { cycle ->
                            val isSelected = selectedCycles == cycle
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x4D8B5CF6) else Color(0x1AFFFFFF))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF8B5CF6) else Color(0x26FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCycles = cycle }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$cycle Hiệp",
                                    color = if (isSelected) Color(0xFFC4B5FD) else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Thời lượng Focus & Break
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Focus select
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Focus (Tập trung):",
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(15, 25, 45, 60).forEach { mins ->
                                val isSel = selectedFocus == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF8B5CF6) else Color(0x1AFFFFFF))
                                        .clickable { selectedFocus = mins }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}p",
                                        color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Break select
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Break (Nghỉ ngơi):",
                            color = Color(0xFF06B6D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(3, 5, 10, 15).forEach { mins ->
                                val isSel = selectedBreak == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF06B6D4) else Color(0x1AFFFFFF))
                                        .clickable { selectedBreak = mins }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}p",
                                        color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Chọn Chế Độ Focus (Music vs Study)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "2. Chế độ làm việc:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Music Mode Card
                        val isMusic = selectedMode == "music"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMusic) Color(0x2E8B5CF6) else Color(0x0DFFFFFF))
                                .border(
                                    width = if (isMusic) 1.5.dp else 1.dp,
                                    color = if (isMusic) Color(0xFF8B5CF6) else Color(0x1FFFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMode = "music" }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isMusic,
                                        onClick = { selectedMode = "music" },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF8B5CF6)),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🎵 Mode Nhạc",
                                        color = Color(0xFFC4B5FD),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Màn hình Đen Trắng, video nhạc tính riêng không tính xao nhãng.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Study Mode Card
                        val isStudy = selectedMode == "study"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isStudy) Color(0x2E10B981) else Color(0x0DFFFFFF))
                                .border(
                                    width = if (isStudy) 1.5.dp else 1.dp,
                                    color = if (isStudy) Color(0xFF10B981) else Color(0x1FFFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMode = "study" }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isStudy,
                                        onClick = { selectedMode = "study" },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981)),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "📚 Mode Học",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Màn hình giữ màu, sau 10s hỏi mục tiêu bài học xem có ích không.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Text(text = "Hủy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onStartPomodoro(selectedCycles, selectedFocus, selectedBreak, selectedMode)
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text(
                            text = "🚀 Bắt Đầu ($selectedCycles hiệp)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
