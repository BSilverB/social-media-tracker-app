package com.example.social_media_tracker_app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.data.model.PuppetPhotos
import com.example.social_media_tracker_app.data.repository.StatsRepository
import com.example.social_media_tracker_app.ui.pet.PetWidget
import java.io.File
import java.io.FileOutputStream

@Composable
fun PetScreen(
    repository: StatsRepository,
    onTriggerBreathing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val petState by repository.petState.collectAsState()
    val scrollState = rememberScrollState()

    var petMode by remember(petState.mode) { mutableStateOf(petState.mode) }
    var happyPhotoPath by remember(petState.puppetPhotos.happyImage) { mutableStateOf(petState.puppetPhotos.happyImage) }
    var neutralPhotoPath by remember(petState.puppetPhotos.neutralImage) { mutableStateOf(petState.puppetPhotos.neutralImage) }
    var sadPhotoPath by remember(petState.puppetPhotos.sadImage) { mutableStateOf(petState.puppetPhotos.sadImage) }

    var activePhotoSlot by remember { mutableStateOf<String?>(null) }

    val puppetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activePhotoSlot != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "pet_puppet_${activePhotoSlot}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(targetFile).use { out ->
                    inputStream?.copyTo(out)
                }
                val savedPath = targetFile.absolutePath
                when (activePhotoSlot) {
                    "happy" -> happyPhotoPath = savedPath
                    "neutral" -> neutralPhotoPath = savedPath
                    "sad" -> sadPhotoPath = savedPath
                }

                // Cập nhật ngay vào repository
                repository.updatePetPuppetPhotos(
                    PuppetPhotos(
                        happyImage = happyPhotoPath,
                        neutralImage = neutralPhotoPath,
                        sadImage = sadPhotoPath
                    )
                )
                Toast.makeText(context, "📸 Đã lưu ảnh trạng thái $activePhotoSlot!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi lưu ảnh: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
        activePhotoSlot = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ─── Header ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LUVPET",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Bảo vệ dopamine & sự tập trung",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

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

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Sân Khấu Linh Vật Tương Tác (Interactive Stage) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color(0x668B5CF6))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                    )
                )
                .border(1.5.dp, Color(0x668B5CF6), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Glow Halo & Mascot (Enlarged)
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    when {
                                        petState.energy >= 70 -> Color(0x5510B981)
                                        petState.energy >= 40 -> Color(0x55FBBF24)
                                        else -> Color(0x55EF4444)
                                    },
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PetWidget(
                        petState = petState,
                        onPoke = { repository.pokePet() },
                        modifier = Modifier.size(145.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mood & Level
                val (moodLabel, moodColor) = when {
                    petState.energy >= 70 -> Pair("Hạnh Phúc & Tràn Đầy Sức Sống 🌟", Color(0xFF34D399))
                    petState.energy >= 40 -> Pair("Bình Thường • Cần Tiếp Tục Chánh Niệm ☕", Color(0xFFFBBF24))
                    else -> Pair("Kiệt Sức • Hãy Nghỉ Ngơi Rời Mạng Xã Hội 🚨", Color(0xFFF87171))
                }

                Text(
                    text = moodLabel,
                    color = moodColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Energy Bar & Metric
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Năng lượng sinh học:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = "${petState.energy}/100 ⚡",
                        color = moodColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { petState.energy / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = moodColor,
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val (msg, energyGained) = repository.pokePet()
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "👋 Chạm Pet (+3⚡)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTriggerBreathing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6638BDF8)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🧘 Thở 12s Cùng Pet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Tủ Đồ Phụ Kiện (Pet Wardrobe) ───
        Text(
            text = "👗 TỦ ĐỒ PHỤ KIỆN LINH VẬT",
            color = Color(0xFFA78BFA),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x1F161E2E))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Duy trì chuỗi ngày có kỷ luật (Streak) để tự động mở khóa phụ kiện mới cho Pet:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                val accessoriesList = listOf(
                    Triple("sunglasses", "🕶️ Kính Râm Siêu Ngầu", 3),
                    Triple("laurel", "👑 Vòng Nguyệt Quế Kỷ Luật", 7)
                )

                accessoriesList.forEach { (id, name, requiredStreak) ->
                    val isUnlocked = petState.streakDays >= requiredStreak || petState.accessories.unlockedItems.contains(id)
                    val isEquipped = petState.accessories.equippedHead == id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isEquipped) Color(0x228B5CF6) else Color(0x14FFFFFF))
                            .border(
                                1.dp,
                                if (isEquipped) Color(0xFF8B5CF6) else Color(0x22FFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = if (isUnlocked) Color.White else Color(0xFF64748B),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUnlocked) "Đã mở khóa (Yêu cầu $requiredStreak ngày)" else "🔒 Cần chuỗi Streak $requiredStreak ngày",
                                color = if (isUnlocked) Color(0xFF34D399) else Color(0xFFEF4444),
                                fontSize = 10.sp
                            )
                        }

                        if (isUnlocked) {
                            Button(
                                onClick = {
                                    if (isEquipped) {
                                        repository.equipAccessory(null)
                                        Toast.makeText(context, "Đã tháo phụ kiện!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        repository.equipAccessory(id)
                                        Toast.makeText(context, "Đã trang bị $name!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEquipped) Color(0xFFEF4444) else Color(0xFF8B5CF6)
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isEquipped) "Tháo" else "Mặc",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "🔒 Khóa",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Quản lý Chế Độ Hiển Thị Pet (Default vs Puppet vs AI) ───
        Text(
            text = "🎭 CHẾ ĐỘ HIỂN THỊ LINH VẬT",
            color = Color(0xFFA78BFA),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x1F161E2E))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column {
                // Tab chuyển đổi 3 chế độ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22000000))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PetModeOptionButton(
                        title = "🦊 Chibi",
                        isSelected = petMode == "default",
                        onClick = {
                            petMode = "default"
                            repository.updatePetMode("default")
                            Toast.makeText(context, "Đã chọn chế độ Chibi Mặc Định!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PetModeOptionButton(
                        title = "🎭 Pet Puppet",
                        isSelected = petMode == "puppet",
                        onClick = {
                            petMode = "puppet"
                            repository.updatePetMode("puppet")
                            Toast.makeText(context, "Đã chọn chế độ Pet Puppet!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PetModeOptionButton(
                        title = "🤖 AI Sprites",
                        isSelected = petMode == "ai_generated",
                        onClick = {
                            petMode = "ai_generated"
                            repository.updatePetMode("ai_generated")
                            Toast.makeText(context, "Đã chọn chế độ AI Generated!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (petMode) {
                    "puppet" -> {
                        Text(
                            text = "Gắn 3 ảnh người thật/thú cưng tương ứng 3 mức năng lượng cảm xúc:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PuppetPhotoSlot(
                                label = "😊 Vui Vẻ",
                                energyDesc = "≥70⚡",
                                imagePath = happyPhotoPath,
                                accentColor = Color(0xFF10B981),
                                onPickPhoto = {
                                    activePhotoSlot = "happy"
                                    puppetPickerLauncher.launch("image/*")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PuppetPhotoSlot(
                                label = "😐 Bình Thường",
                                energyDesc = "40-69⚡",
                                imagePath = neutralPhotoPath,
                                accentColor = Color(0xFFFBBF24),
                                onPickPhoto = {
                                    activePhotoSlot = "neutral"
                                    puppetPickerLauncher.launch("image/*")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PuppetPhotoSlot(
                                label = "😢 Buồn / Mệt",
                                energyDesc = "<40⚡",
                                imagePath = sadPhotoPath,
                                accentColor = Color(0xFFEF4444),
                                onPickPhoto = {
                                    activePhotoSlot = "sad"
                                    puppetPickerLauncher.launch("image/*")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    "ai_generated" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "🎨 Chế độ Linh Vật AI tạo qua Gemini Vision API",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Linh vật sẽ tự động cập nhật ngoại hình dựa trên tính cách & thói quen sử dụng số của bạn sau khi tổng hợp phản tư tối.",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "🌿 Linh vật Vector Chibi Chánh Niệm Nguyên Bản",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tự động đổi trạng thái khuôn mặt động theo năng lượng thực tế.",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Quy Tắc Kỷ Luật & Hồi Phục Digital Detox ───
        Text(
            text = "🌱 CƠ CHẾ NĂNG LƯỢNG & DIGITAL DETOX",
            color = Color(0xFFA78BFA),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x1F059669))
                .border(1.dp, Color(0x44059669), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🌱 Hồi phục tự nhiên: +5⚡ mỗi 30 phút rời xa mạng xã hội / tắt app.",
                    color = Color(0xFF6EE7B7),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "⚠️ Mốc 1 (M1): Phạt -5⚡ khi bắt đầu lướt vượt ngưỡng.",
                    color = Color(0xFFFDE68A),
                    fontSize = 11.sp
                )
                Text(
                    text = "🚨 Mốc 2 (M2): Phạt -10⚡ & Kích hoạt chế độ Đen Trắng làm dịu dopamine.",
                    color = Color(0xFFFDBA74),
                    fontSize = 11.sp
                )
                Text(
                    text = "🛑 Mốc 3 (M3): Phạt -20⚡ & Bắt buộc thở Box Breathing 12s.",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PetModeOptionButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF8B5CF6) else Color.Transparent,
        animationSpec = tween(200),
        label = "modeBg"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PuppetPhotoSlot(
    label: String,
    energyDesc: String,
    imagePath: String,
    accentColor: Color,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imagePath) {
        if (imagePath.isNotBlank() && File(imagePath).exists()) {
            try {
                BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, accentColor, CircleShape)
                    .clickable { onPickPhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp)
                    )
                } else {
                    Text(text = "📷", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(text = energyDesc, color = accentColor, fontSize = 8.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.25f))
                    .clickable { onPickPhoto() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(text = if (bitmap != null) "Đổi" else "Chọn", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
