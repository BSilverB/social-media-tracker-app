package com.example.social_media_tracker_app.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.social_media_tracker_app.data.model.PetState
import kotlinx.coroutines.delay

@Composable
fun MindfulBreathingOverlay(
    masterGoal: String = "Muốn trở thành phiên bản tốt hơn",
    currentSwipes: Int,
    threshold: Int = 45,
    petState: PetState? = null,
    onDismiss: () -> Unit,
    onCloseApp: () -> Unit,
    onResistTemptation: (() -> Unit)? = null,
    onSuccumbTemptation: (() -> Unit)? = null
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val totalDurationSeconds = 12
    val isComplete = elapsedSeconds >= totalDurationSeconds

    LaunchedEffect(Unit) {
        while (elapsedSeconds < totalDurationSeconds) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val (phaseText, phaseColor) = when {
        elapsedSeconds < 4 -> Pair("🌬️ Hít vào thật sâu bằng mũi (4s)...", Color(0xFF06B6D4))
        elapsedSeconds < 8 -> Pair("🧘 Nín thở & thả lỏng toàn thân (4s)...", Color(0xFFA78BFA))
        elapsedSeconds < 12 -> Pair("💨 Thở ra từ từ bằng miệng (4s)...", Color(0xFF10B981))
        else -> Pair("✨ Tâm trí đã bình tĩnh lại. Bạn làm chủ được hành vi!", Color(0xFF34D399))
    }

    // Breathing pulsating animation
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0080C14))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Heart & Emotional Anchor
            Text(
                text = "❤️",
                fontSize = 44.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Dừng Lại Một Chút Nào!",
                color = Color(0xFFEF4444),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Bạn đã lướt $currentSwipes lượt (Vượt ngưỡng đỏ $threshold lượt hôm nay).",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Emotional Anchor Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1F8B5CF6))
                    .border(1.dp, Color(0x668B5CF6), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LỜI NHẮC TỪ MỤC TIÊU LỚN CỦA BẠN:",
                        color = Color(0xFFC4B5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$masterGoal\"",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Người thương luôn tin bạn sẽ làm chủ được bản thân!",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 12-second Box Breathing Visual Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Circular Progress Ring
                val progress = (elapsedSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(160.dp),
                    color = phaseColor,
                    strokeWidth = 6.dp,
                    trackColor = Color(0x22FFFFFF),
                )

                // Animated breathing orb (Pet embraces the breathing orb synchronously)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .scale(if (isComplete) 1f else pulseScale)
                        .shadow(20.dp, CircleShape, spotColor = phaseColor)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(phaseColor.copy(alpha = 0.85f), phaseColor.copy(alpha = 0.25f))
                            )
                        )
                        .border(2.dp, phaseColor.copy(alpha = 0.6f), CircleShape)
                ) {
                    if (petState != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val petEmoji = when {
                                petState.isWilted -> "🥀"
                                petState.mode == "puppet" -> "🧸"
                                petState.mode == "ai_generated" -> "✨"
                                petState.computedEvolutionStage == "flowering" -> "🌸"
                                petState.computedEvolutionStage == "growing" -> "🌿"
                                else -> "🌱"
                            }
                            Text(
                                text = petEmoji,
                                fontSize = 30.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isComplete) "✓ Tốt lắm" else "${totalDurationSeconds - elapsedSeconds}s",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Text(
                            text = if (isComplete) "✓" else "${totalDurationSeconds - elapsedSeconds}s",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = phaseText,
                color = phaseColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(24.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onResistTemptation?.invoke()
                        onCloseApp()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "🛑 Rời đi (+10⚡)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onSuccumbTemptation?.invoke()
                        onDismiss()
                    },
                    enabled = isComplete,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6),
                        disabledContainerColor = Color(0x33FFFFFF),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isComplete) "Tiếp tục (+5⚡)" else "⏳ Thở đủ 12s",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isComplete) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
