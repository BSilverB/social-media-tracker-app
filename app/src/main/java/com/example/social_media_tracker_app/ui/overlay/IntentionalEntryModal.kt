package com.example.social_media_tracker_app.ui.overlay

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun IntentionalEntryModal(
    masterGoal: String,
    emotionalAnchorImage: String = "",
    platformName: String = "Mạng Xã Hội",
    onSubmitIntent: (intentText: String, sessionMinutes: Int) -> Unit
) {
    var intentInput by remember { mutableStateOf("") }
    var sessionMinutesText by remember { mutableStateOf("15") }
    var isConfirmed by remember { mutableStateOf(false) }

    // Load photo from local path if available
    val anchorBitmap = remember(emotionalAnchorImage) {
        if (emotionalAnchorImage.isNotBlank()) {
            try {
                val f = File(emotionalAnchorImage)
                if (f.exists() && f.length() > 0) {
                    BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Gentle pulse animation for photo border
    val infiniteTransition = rememberInfiniteTransition(label = "anchorPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val finalMinutes = sessionMinutesText.toIntOrNull()?.coerceIn(1, 180) ?: 15

    // Auto-advance 2.5s after confirmation
    LaunchedEffect(isConfirmed) {
        if (isConfirmed) {
            delay(2500)
            val finalIntent = intentInput.trim().ifEmpty { "Giải trí có ý thức và tỉnh thức" }
            onSubmitIntent(finalIntent, finalMinutes)
        }
    }

    Dialog(onDismissRequest = { /* Require mindful answer */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF51E1B4B), // Deep indigo
                            Color(0xFA0F172A)  // Dark slate
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFEC4899).copy(alpha = pulseGlow),
                            Color(0xFF8B5CF6).copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = isConfirmed,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "modalStateTransition"
                ) { confirmed ->
                    if (!confirmed) {
                        // ─── STATE 1: QUESTION FROM EMOTIONAL ANCHOR ───
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Speech Bubble atop the photo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x33EC4899))
                                    .border(1.dp, Color(0x66F43F5E), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "💬 \"Chào bạn! Bạn mở $platformName với mục tiêu gì và trong bao lâu?\"",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    if (masterGoal.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "🎯 \"$masterGoal\"",
                                            color = Color(0xFFFDE047),
                                            fontSize = 11.sp,
                                            fontStyle = FontStyle.Italic,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Emotional Anchor Photo
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .shadow(16.dp, CircleShape, spotColor = Color(0xFFF43F5E))
                                    .clip(CircleShape)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFEC4899),
                                                Color(0xFF8B5CF6),
                                                Color(0xFFF59E0B),
                                                Color(0xFFEC4899)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (anchorBitmap != null) {
                                    Image(
                                        bitmap = anchorBitmap,
                                        contentDescription = "Mỏ neo cảm xúc",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(76.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFFFB7185), Color(0xFFE11D48))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "💖", fontSize = 34.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. Goal Input & Quick Suggestions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                QuickIntentChip("📚 Học 15p", onClick = {
                                    intentInput = "Học tập & tiếp thu kiến thức"
                                    sessionMinutesText = "15"
                                })
                                QuickIntentChip("📰 Xem tin 10p", onClick = {
                                    intentInput = "Cập nhật tin tức quan trọng"
                                    sessionMinutesText = "10"
                                })
                                QuickIntentChip("☕ Thư giãn 5p", onClick = {
                                    intentInput = "Thư giãn nhanh"
                                    sessionMinutesText = "5"
                                })
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = intentInput,
                                onValueChange = { intentInput = it },
                                placeholder = {
                                    Text(
                                        text = "Mục tiêu cụ thể phiên này...",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFEC4899),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Session Duration Configuration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏱ Thời gian phiên:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DurationChip("5p", selected = sessionMinutesText == "5", onClick = { sessionMinutesText = "5" })
                                    DurationChip("10p", selected = sessionMinutesText == "10", onClick = { sessionMinutesText = "10" })
                                    DurationChip("15p", selected = sessionMinutesText == "15", onClick = { sessionMinutesText = "15" })
                                    DurationChip("25p", selected = sessionMinutesText == "25", onClick = { sessionMinutesText = "25" })
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Confirm Button
                            Button(
                                onClick = { isConfirmed = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
                            ) {
                                Text(
                                    text = "💬 Cam Kết & Bắt Đầu ($finalMinutes Phút)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // ─── STATE 2: EMOTIONAL ANCHOR RESPONDS & ENCOURAGES ───
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Emotional Reply Bubble
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x3310B981))
                                    .border(1.dp, Color(0x6634D399), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "❤️ \"Mình tin bạn làm được!\"",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val displayIntent = intentInput.trim().ifEmpty { "Giải trí ngắn hạn có ý thức" }
                                    Text(
                                        text = "Hãy tập trung vào: \"$displayIntent\" trong $finalMinutes phút tới nhé.\nLinh vật sẽ ở đây đồng hành và nhắc nhở bạn! ❤️",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Glowing Photo
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .shadow(20.dp, CircleShape, spotColor = Color(0xFF10B981))
                                    .clip(CircleShape)
                                    .border(3.dp, Color(0xFF34D399), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (anchorBitmap != null) {
                                    Image(
                                        bitmap = anchorBitmap,
                                        contentDescription = "Mỏ neo cảm xúc",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(86.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(86.dp)
                                            .background(Color(0xFF059669)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🥰", fontSize = 38.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val finalIntent = intentInput.trim().ifEmpty { "Giải trí có ý thức và tỉnh thức" }
                                    onSubmitIntent(finalIntent, finalMinutes)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text(
                                    text = "🚀 Bắt Đầu Phiên ($finalMinutes Phút)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickIntentChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x26FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DurationChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF8B5CF6) else Color(0x22FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFFCBD5E1),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
