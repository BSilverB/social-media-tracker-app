package com.example.social_media_tracker_app.ui.pet

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.social_media_tracker_app.data.model.PetState
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PetWidget(
    petState: PetState,
    onPoke: () -> Pair<String, Boolean>,
    modifier: Modifier = Modifier
) {
    var bubbleText by remember { mutableStateOf<String?>(null) }
    var showEnergyBonus by remember { mutableStateOf(false) }

    // Auto-dismiss bubble after 4.0 seconds
    LaunchedEffect(bubbleText) {
        if (bubbleText != null) {
            delay(4000L)
            bubbleText = null
            showEnergyBonus = false
        }
    }

    // Bounce / Breathe animation
    val transition = rememberInfiniteTransition(label = "petBounce")
    val bounceY by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (petState.mood == "happy" && !petState.isDisappeared) -4f else -1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Load puppet bitmap if in puppet mode
    val puppetImagePath = petState.getActivePuppetImagePath()
    val puppetBitmap = remember(puppetImagePath, petState.mood, petState.energy) {
        if (!puppetImagePath.isNullOrBlank()) {
            try {
                val f = File(puppetImagePath)
                if (f.exists() && f.length() > 0) {
                    BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Speech Bubble (Above the pet)
        AnimatedVisibility(
            visible = bubbleText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.offset(y = (-52).dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 140.dp, max = 230.dp)
                        .shadow(10.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xF51E293B))
                        .border(1.2.dp, Color(0xFF8B5CF6), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = bubbleText ?: "",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (showEnergyBonus) {
                    Text(
                        text = "+3⚡",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ─── PET VISUAL: DISAPPEARED / PUPPET / DEFAULT ───
        if (petState.isDisappeared) {
            // Pet has disappeared because energy is 0
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .offset(y = bounceY.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x66EF4444), CircleShape)
                    .clickable {
                        bubbleText = "💔 Pet đang kiệt sức...\nHãy thở 12s hoặc phản tư để hồi sinh nhé!"
                        showEnergyBonus = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💨", fontSize = 18.sp)
            }
        } else if (petState.mode == "puppet") {
            // Puppet Mode
            val moodColor = when (petState.mood) {
                "happy" -> Color(0xFF10B981)
                "neutral" -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .offset(y = bounceY.dp)
                    .clip(CircleShape)
                    .border(2.dp, moodColor, CircleShape)
                    .clickable {
                        val (msg, gained) = onPoke()
                        bubbleText = msg
                        showEnergyBonus = gained
                    },
                contentAlignment = Alignment.Center
            ) {
                if (puppetBitmap != null) {
                    Image(
                        bitmap = puppetBitmap,
                        contentDescription = "Pet Puppet",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    // Fallback puppet emoji based on mood
                    val emoji = when (petState.mood) {
                        "happy" -> "🥰"
                        "neutral" -> "😐"
                        else -> "😢"
                    }
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        } else {
            // Default Sprout Canvas Mascot
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .offset(y = bounceY.dp)
                    .clickable {
                        val (msg, gained) = onPoke()
                        bubbleText = msg
                        showEnergyBonus = gained
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(34.dp)) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)

                    // 1. Sprout Body
                    val bodyColor = when (petState.mood) {
                        "happy" -> Color(0xFF10B981)
                        "neutral" -> Color(0xFF34D399)
                        else -> Color(0xFF64748B) // Sad
                    }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(bodyColor, bodyColor.copy(alpha = 0.85f)),
                            center = center,
                            radius = w * 0.38f
                        ),
                        radius = w * 0.38f,
                        center = center
                    )

                    // Sprout Stem & Leaf
                    val stemPath = Path().apply {
                        moveTo(center.x, center.y - h * 0.35f)
                        quadraticTo(center.x - 4f, center.y - h * 0.55f, center.x - 8f, center.y - h * 0.65f)
                    }
                    drawPath(stemPath, color = bodyColor, style = Stroke(width = 3f))

                    val leafPath = Path().apply {
                        moveTo(center.x - 8f, center.y - h * 0.65f)
                        quadraticTo(center.x - 14f, center.y - h * 0.75f, center.x - 4f, center.y - h * 0.8f)
                        quadraticTo(center.x - 2f, center.y - h * 0.7f, center.x - 8f, center.y - h * 0.65f)
                    }
                    drawPath(leafPath, color = Color(0xFF22C55E), style = Fill)

                    // 2. Eyes & Facial Expression
                    val eyeY = center.y - 2f
                    val eyeSpacing = 6f
                    when (petState.mood) {
                        "happy" -> {
                            val leftEye = Path().apply {
                                moveTo(center.x - eyeSpacing - 3f, eyeY + 1f)
                                quadraticTo(center.x - eyeSpacing, eyeY - 3f, center.x - eyeSpacing + 3f, eyeY + 1f)
                            }
                            val rightEye = Path().apply {
                                moveTo(center.x + eyeSpacing - 3f, eyeY + 1f)
                                quadraticTo(center.x + eyeSpacing, eyeY - 3f, center.x + eyeSpacing + 3f, eyeY + 1f)
                            }
                            drawPath(leftEye, color = Color(0xFF0F172A), style = Stroke(width = 2.5f))
                            drawPath(rightEye, color = Color(0xFF0F172A), style = Stroke(width = 2.5f))

                            drawCircle(Color(0x88F43F5E), radius = 2.5f, center = Offset(center.x - eyeSpacing - 4f, eyeY + 5f))
                            drawCircle(Color(0x88F43F5E), radius = 2.5f, center = Offset(center.x + eyeSpacing + 4f, eyeY + 5f))
                        }
                        "neutral" -> {
                            drawCircle(Color(0xFF0F172A), radius = 2f, center = Offset(center.x - eyeSpacing, eyeY))
                            drawCircle(Color(0xFF0F172A), radius = 2f, center = Offset(center.x + eyeSpacing, eyeY))
                        }
                        else -> {
                            val leftSad = Path().apply {
                                moveTo(center.x - eyeSpacing - 3f, eyeY - 1f)
                                quadraticTo(center.x - eyeSpacing, eyeY + 3f, center.x - eyeSpacing + 3f, eyeY - 1f)
                            }
                            val rightSad = Path().apply {
                                moveTo(center.x + eyeSpacing - 3f, eyeY - 1f)
                                quadraticTo(center.x + eyeSpacing, eyeY + 3f, center.x + eyeSpacing + 3f, eyeY - 1f)
                            }
                            drawPath(leftSad, color = Color(0xFF0F172A), style = Stroke(width = 2.5f))
                            drawPath(rightSad, color = Color(0xFF0F172A), style = Stroke(width = 2.5f))
                        }
                    }

                    // Small mouth
                    val mouthPath = Path().apply {
                        if (petState.mood == "happy") {
                            moveTo(center.x - 3f, center.y + 4f)
                            quadraticTo(center.x, center.y + 7f, center.x + 3f, center.y + 4f)
                        } else if (petState.mood == "sad") {
                            moveTo(center.x - 3f, center.y + 7f)
                            quadraticTo(center.x, center.y + 4f, center.x + 3f, center.y + 7f)
                        } else {
                            moveTo(center.x - 2f, center.y + 5f)
                            lineTo(center.x + 2f, center.y + 5f)
                        }
                    }
                    drawPath(mouthPath, color = Color(0xFF0F172A), style = Stroke(width = 2f))

                    // 3. Accessories Layer
                    when (petState.accessories.equippedHead) {
                        "sunglasses" -> {
                            val glassY = center.y - 4f
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(center.x - eyeSpacing - 5f, glassY),
                                size = Size(8f, 7f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(center.x + eyeSpacing - 3f, glassY),
                                size = Size(8f, 7f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawLine(
                                color = Color(0xFF0F172A),
                                start = Offset(center.x - 1f, glassY + 2f),
                                end = Offset(center.x + 1f, glassY + 2f),
                                strokeWidth = 2f
                            )
                        }
                        "laurel" -> {
                            val crownY = center.y - h * 0.42f
                            val crownPath = Path().apply {
                                moveTo(center.x - 10f, crownY + 2f)
                                quadraticTo(center.x, crownY - 4f, center.x + 10f, crownY + 2f)
                            }
                            drawPath(crownPath, color = Color(0xFFF59E0B), style = Stroke(width = 3.5f))
                            drawCircle(Color(0xFFFBBF24), radius = 2.5f, center = Offset(center.x - 6f, crownY - 2f))
                            drawCircle(Color(0xFFFBBF24), radius = 2.5f, center = Offset(center.x + 6f, crownY - 2f))
                            drawCircle(Color(0xFFFBBF24), radius = 3f, center = Offset(center.x, crownY - 4f))
                        }
                    }
                }
            }
        }
    }
}
