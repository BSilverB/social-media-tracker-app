package com.example.social_media_tracker_app.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.social_media_tracker_app.data.model.PetState

@Composable
fun PetWardrobeModal(
    petState: PetState,
    onEquip: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xF5131C2D))
                .border(1.5.dp, Color(0xFF8B5CF6), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Text(
                    text = "🌱 Linh Vật & Tủ Đồ Phụ Kiện",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pet Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Năng lượng: ${petState.energy}/100⚡",
                                color = Color(0xFFFBBF24),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "🔥 Streak: ${petState.streakDays} ngày",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { petState.energy / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                petState.energy >= 70 -> Color(0xFF10B981)
                                petState.energy >= 40 -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            },
                            trackColor = Color(0x33FFFFFF)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tâm trạng: " + when (petState.mood) {
                                "happy" -> "Vui vẻ, phấn khởi (>=70⚡) 😊"
                                "neutral" -> "Bình thường (40-69⚡) 😐"
                                else -> "Ủ rũ, mệt mỏi (<40⚡) 😢"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "TỦ ĐỒ KIÊN TRÌ (STREAK WARDROBE)",
                    color = Color(0xFFC4B5FD),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Item 1: Sunglasses (Streak >= 3)
                val hasSunglasses = petState.streakDays >= 3 || petState.accessories.unlockedItems.contains("sunglasses")
                val isSunglassesEquipped = petState.accessories.equippedHead == "sunglasses"

                AccessoryRow(
                    icon = "😎",
                    name = "Kính Râm Sành Điệu",
                    requirement = "Streak ≥ 3 ngày",
                    isUnlocked = hasSunglasses,
                    isEquipped = isSunglassesEquipped,
                    onToggleEquip = {
                        if (isSunglassesEquipped) onEquip(null) else onEquip("sunglasses")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Item 2: Laurel Wreath (Streak >= 7)
                val hasLaurel = petState.streakDays >= 7 || petState.accessories.unlockedItems.contains("laurel")
                val isLaurelEquipped = petState.accessories.equippedHead == "laurel"

                AccessoryRow(
                    icon = "🌿",
                    name = "Vòng Nguyệt Quế",
                    requirement = "Streak ≥ 7 ngày",
                    isUnlocked = hasLaurel,
                    isEquipped = isLaurelEquipped,
                    onToggleEquip = {
                        if (isLaurelEquipped) onEquip(null) else onEquip("laurel")
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Đóng", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AccessoryRow(
    icon: String,
    name: String,
    requirement: String,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onToggleEquip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1AFFFFFF))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isUnlocked) "Đã mở khóa ✓" else "🔒 $requirement",
                        color = if (isUnlocked) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            if (isUnlocked) {
                if (isEquipped) {
                    OutlinedButton(
                        onClick = onToggleEquip,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Text(text = "Tháo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onToggleEquip,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(text = "Đeo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
