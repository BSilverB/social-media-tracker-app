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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.ui.pet.PetWidget

@Composable
fun PetExtensionModal(
    petState: PetState,
    sessionMinutes: Int,
    onExtend: (extraMinutes: Int, energyCost: Int) -> Unit,
    onStopAndExit: () -> Unit
) {
    Dialog(onDismissRequest = { /* Require choice */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF51E1B4B),
                            Color(0xFA0F172A)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFF59E0B),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pet Avatar Header
                PetWidget(
                    petState = petState,
                    onPoke = { "Bạn ơi, hãy bảo vệ năng lượng của mình nhé!" to false },
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Speech Bubble
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x26F59E0B))
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔔 \"Hết thời gian phiên $sessionMinutes phút rồi bạn ơi!\"",
                            color = Color(0xFFFDE047),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Để không ngắt quãng video dang dở, bạn có muốn gia hạn thêm không?",
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Pet Energy Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Năng lượng linh vật hiện tại:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${petState.energy}⚡ / 100",
                        color = if (petState.energy > 30) Color(0xFFFBBF24) else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "LỰA CHỌN GIA HẠN (ĐÁNH ĐỔI NĂNG LƯỢNG):",
                    color = Color(0xFFA78BFA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Extension Option Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExtensionOptionButton(
                        minutes = 5,
                        energyCost = 5,
                        onClick = { onExtend(5, 5) },
                        modifier = Modifier.weight(1f)
                    )
                    ExtensionOptionButton(
                        minutes = 10,
                        energyCost = 12,
                        onClick = { onExtend(10, 12) },
                        modifier = Modifier.weight(1f)
                    )
                    ExtensionOptionButton(
                        minutes = 15,
                        energyCost = 20,
                        onClick = { onExtend(15, 20) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stop & Rest Button (Hero action: rewards pet +10 energy)
                Button(
                    onClick = onStopAndExit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(
                        text = "🛑 Dừng Lại & Về Nghỉ Ngơi (+10⚡ Thưởng)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "⚠️ Gia hạn càng lâu năng lượng giảm càng nhanh. Năng lượng về 0⚡ linh vật sẽ biến mất vì thất vọng!",
                    color = Color(0xFFFCA5A5),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ExtensionOptionButton(
    minutes: Int,
    energyCost: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x22F59E0B))
            .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+$minutes Phút",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "-$energyCost⚡",
                color = Color(0xFFF87171),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
