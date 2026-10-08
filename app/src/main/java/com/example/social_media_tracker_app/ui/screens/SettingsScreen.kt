package com.example.social_media_tracker_app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.social_media_tracker_app.data.model.BedtimeConfig
import com.example.social_media_tracker_app.data.model.FacebookLimits
import com.example.social_media_tracker_app.data.model.FacebookThresholds
import com.example.social_media_tracker_app.data.model.PlatformLimits
import com.example.social_media_tracker_app.data.model.PlatformThresholds
import com.example.social_media_tracker_app.data.model.ReflectionConfig
import com.example.social_media_tracker_app.data.model.ThresholdConfig
import com.example.social_media_tracker_app.data.model.TiktokLimits
import com.example.social_media_tracker_app.data.model.TiktokThresholds
import com.example.social_media_tracker_app.data.model.YoutubeLimits
import com.example.social_media_tracker_app.data.model.YoutubeThresholds
import com.example.social_media_tracker_app.data.repository.FirebaseSyncManager
import com.example.social_media_tracker_app.data.repository.StatsRepository
import java.io.File
import java.io.FileOutputStream

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    repository: StatsRepository,
    syncManager: FirebaseSyncManager = FirebaseSyncManager.getInstance(LocalContext.current, repository),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentConfig = repository.appConfig.value
    val petState by repository.petState.collectAsState()
    val scrollState = rememberScrollState()

    val isConnected by syncManager.isConnected.collectAsState()
    val desktopStats by syncManager.desktopStats.collectAsState()
    var syncCodeInput by remember { mutableStateOf(currentConfig.syncCode) }

    var masterGoal by remember { mutableStateOf(currentConfig.masterGoal) }
    var emotionalAnchor by remember { mutableStateOf(currentConfig.emotionalAnchorImage) }
    var geminiKey by remember { mutableStateOf(currentConfig.geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    // ─── PLATFORM THRESHOLDS STATES (M1, M2, M3 MATCHING EXTENSION) ───
    val th = currentConfig.thresholds
    var ytShortsM1 by remember { mutableStateOf(th.youtube.shorts.m1.toString()) }
    var ytShortsM2 by remember { mutableStateOf(th.youtube.shorts.m2.toString()) }
    var ytShortsM3 by remember { mutableStateOf(th.youtube.shorts.m3.toString()) }

    var ytLongM1 by remember { mutableStateOf(th.youtube.long.m1.toString()) }
    var ytLongM2 by remember { mutableStateOf(th.youtube.long.m2.toString()) }
    var ytLongM3 by remember { mutableStateOf(th.youtube.long.m3.toString()) }

    var fbReelsM1 by remember { mutableStateOf(th.facebook.reels.m1.toString()) }
    var fbReelsM2 by remember { mutableStateOf(th.facebook.reels.m2.toString()) }
    var fbReelsM3 by remember { mutableStateOf(th.facebook.reels.m3.toString()) }

    var fbFeedsM1 by remember { mutableStateOf(th.facebook.feeds.m1.toString()) }
    var fbFeedsM2 by remember { mutableStateOf(th.facebook.feeds.m2.toString()) }
    var fbFeedsM3 by remember { mutableStateOf(th.facebook.feeds.m3.toString()) }

    var fbWatchM1 by remember { mutableStateOf(th.facebook.long.m1.toString()) }
    var fbWatchM2 by remember { mutableStateOf(th.facebook.long.m2.toString()) }
    var fbWatchM3 by remember { mutableStateOf(th.facebook.long.m3.toString()) }

    var ttShortsM1 by remember { mutableStateOf(th.tiktok.shorts.m1.toString()) }
    var ttShortsM2 by remember { mutableStateOf(th.tiktok.shorts.m2.toString()) }
    var ttShortsM3 by remember { mutableStateOf(th.tiktok.shorts.m3.toString()) }

    var leisureQuotaText by remember { mutableStateOf(currentConfig.leisureQuotaMinutes.toString()) }

    // Keyword Filter States
    var targetKeywords by remember { mutableStateOf(currentConfig.targetKeywords) }
    var leisureKeywords by remember { mutableStateOf(currentConfig.leisureKeywords) }
    var distractionKeywords by remember { mutableStateOf(currentConfig.distractionKeywords) }
    var selectedKeywordCategory by remember { mutableIntStateOf(0) }
    var newKeywordInput by remember { mutableStateOf("") }

    // Bedtime & Reflection Reminder States
    var bedtimeEnabled by remember { mutableStateOf(currentConfig.bedtime.enabled) }
    var bedtimeStart by remember { mutableStateOf(currentConfig.bedtime.startTime) }
    var bedtimeEnd by remember { mutableStateOf(currentConfig.bedtime.endTime) }
    var reflectionReminderTime by remember { mutableStateOf(currentConfig.reflection.reminderTime) }

    // QA Test Mode States
    val isQaMode by repository.isQaMode.collectAsState()
    var testEnergyInput by remember { mutableFloatStateOf(petState.energy.toFloat()) }

    // Photo picker launcher for Emotional Anchor
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "emotional_anchor_${System.currentTimeMillis()}.jpg")
                FileOutputStream(targetFile).use { out ->
                    inputStream?.copyTo(out)
                }
                emotionalAnchor = targetFile.absolutePath
                Toast.makeText(context, "📸 Đã tải ảnh mỏ neo cảm xúc thành công!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Bitmap preview helper
    fun decodeBitmap(path: String) = if (path.isNotBlank()) {
        try {
            val f = File(path)
            if (f.exists() && f.length() > 0) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
        } catch (e: Exception) { null }
    } else null

    val previewBitmap = remember(emotionalAnchor) { decodeBitmap(emotionalAnchor) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ─── TOP BAR WITH BACK BUTTON ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable { onBack() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "← Quay lại",
                    color = Color(0xFFA78BFA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "⚙️ Cài Đặt Hệ Thống",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Thiết lập mốc ma sát M1/M2/M3, giới hạn nền tảng & Gemini AI",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── NOTICE: PET RELOCATION ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1F10B981))
                .border(1.dp, Color(0x4410B981), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🐾", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tùy chỉnh Linh vật, Pet Puppet và Tủ đồ đã được chuyển riêng sang tab Pet để bạn quản lý tập trung và trực quan hơn.",
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── SECTION 1: MASTER GOAL ───
        SettingsSectionHeader(title = "🎯 MỤC TIÊU CỐT LÕI (MASTER GOAL)")
        OutlinedTextField(
            value = masterGoal,
            onValueChange = { masterGoal = it },
            label = { Text("Mục tiêu lớn nhắc nhở bạn khi lướt", fontSize = 11.sp) },
            placeholder = { Text("VD: Trở thành kỹ sư AI giỏi, rèn kỷ luật bản thân...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color(0x33FFFFFF)
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ─── SECTION 1.5: FIREBASE REALTIME CLOUD SYNC ───
        SettingsSectionHeader(title = "☁️ ĐỒNG BỘ THIẾT BỊ (FIREBASE CLOUD)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1F8B5CF6))
                .border(1.2.dp, Color(0x668B5CF6), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Đồng Bộ 2 Chiều Realtime Engine",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Khớp số liệu lướt và ma sát giữa App điện thoại và Chrome Extension máy tính.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection Status Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isConnected) Color(0x2210B981) else Color(0x22EF4444))
                        .border(1.dp, if (isConnected) Color(0x4410B981) else Color(0x44EF4444), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isConnected) "🟢" else "🔴",
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected)
                                "Đã kết nối Firebase Cloud • Trạng thái: Sẵn sàng"
                            else
                                "Đang ngoại tuyến (Offline First - Tự động đẩy bù khi có mạng)",
                            color = if (isConnected) Color(0xFF34D399) else Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Desktop Live Stats Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1A0EA5E9))
                        .border(1.dp, Color(0x330EA5E9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💻", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Máy tính hôm nay: ${desktopStats.swipes} swipes • ${desktopStats.validViews} xem sâu (${desktopStats.activeSeconds / 60}m)",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Mã Đồng Bộ Cá Nhân (Sync Code):",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = syncCodeInput,
                    onValueChange = { syncCodeInput = it.uppercase() },
                    placeholder = { Text("VD: MF-8924", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFA78BFA),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SyncCode", syncCodeInput.trim().uppercase())
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "📋 Đã sao chép mã đồng bộ: ${syncCodeInput.trim().uppercase()}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
                    ) {
                        Text("📋 Sao chép mã", fontSize = 11.sp, color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val clean = syncCodeInput.trim().uppercase()
                            if (clean.isNotBlank()) {
                                syncManager.updateSyncCode(clean)
                                Toast.makeText(context, "🔗 Đã chuyển sang mã đồng bộ: $clean", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
                    ) {
                        Text("🔗 Cập nhật mã", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── SECTION 2: MỐC GIỚI HẠN TỪNG NỀN TẢNG (GIỐNG EXTENSION) ───
        SettingsSectionHeader(title = "🚦 THIẾT LẬP MỐC GIỚI HẠN NỀN TẢNG (M1 - M2 - M3)")
        Text(
            text = "Cài đặt 3 mốc ma sát (Mốc 1: Nhắc nhở, Mốc 2: Grayscale đen trắng, Mốc 3: Chặn thở Box Breathing) cho từng nội dung:",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 🔴 MỐC GIỚI HẠN YOUTUBE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x33EF4444), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MỐC GIỚI HẠN YOUTUBE",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // YouTube Shorts
                MilestoneTripleInput(
                    label = "YouTube Shorts (Lượt lướt):",
                    m1Value = ytShortsM1,
                    onM1Change = { ytShortsM1 = it },
                    m2Value = ytShortsM2,
                    onM2Change = { ytShortsM2 = it },
                    m3Value = ytShortsM3,
                    onM3Change = { ytShortsM3 = it }
                )

                // YouTube Video Dài
                MilestoneTripleInput(
                    label = "Video dài (/watch - số video):",
                    m1Value = ytLongM1,
                    onM1Change = { ytLongM1 = it },
                    m2Value = ytLongM2,
                    onM2Change = { ytLongM2 = it },
                    m3Value = ytLongM3,
                    onM3Change = { ytLongM3 = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔵 MỐC GIỚI HẠN FACEBOOK
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x333B82F6), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MỐC GIỚI HẠN FACEBOOK",
                        color = Color(0xFF93C5FD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // FB Reels
                MilestoneTripleInput(
                    label = "FB Reels (Lượt lướt):",
                    m1Value = fbReelsM1,
                    onM1Change = { fbReelsM1 = it },
                    m2Value = fbReelsM2,
                    onM2Change = { fbReelsM2 = it },
                    m3Value = fbReelsM3,
                    onM3Change = { fbReelsM3 = it }
                )

                // News Feed
                MilestoneTripleInput(
                    label = "News Feed (Số bài viết cuộn qua):",
                    m1Value = fbFeedsM1,
                    onM1Change = { fbFeedsM1 = it },
                    m2Value = fbFeedsM2,
                    onM2Change = { fbFeedsM2 = it },
                    m3Value = fbFeedsM3,
                    onM3Change = { fbFeedsM3 = it }
                )

                // Video FB Watch
                MilestoneTripleInput(
                    label = "Video FB Watch (Số video):",
                    m1Value = fbWatchM1,
                    onM1Change = { fbWatchM1 = it },
                    m2Value = fbWatchM2,
                    onM2Change = { fbWatchM2 = it },
                    m3Value = fbWatchM3,
                    onM3Change = { fbWatchM3 = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🎵 MỐC GIỚI HẠN TIKTOK
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x3306B6D4), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06B6D4))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MỐC GIỚI HẠN TIKTOK",
                        color = Color(0xFF67E8F9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // TikTok Shorts
                MilestoneTripleInput(
                    label = "TikTok (Lượt lướt video ngắn):",
                    m1Value = ttShortsM1,
                    onM1Change = { ttShortsM1 = it },
                    m2Value = ttShortsM2,
                    onM2Change = { ttShortsM2 = it },
                    m3Value = ttShortsM3,
                    onM3Change = { ttShortsM3 = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Leisure Quota Card
        PlatformLimitCard(
            title = "☕ Quỹ Giải Trí Lành Mạnh (Mỗi Ngày)",
            accentColor = Color(0xFFF59E0B)
        ) {
            LimitInputField(
                label = "Thời gian giải trí cho phép (Phút / ngày)",
                value = leisureQuotaText,
                onValueChange = { leisureQuotaText = it },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Nội dung giải trí vượt quá quỹ này sẽ tự động chuyển thành 'Bẫy Dopamine / Lạc lối'.",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── SECTION 3: EMOTIONAL ANCHOR PHOTO PICKER ───
        SettingsSectionHeader(title = "💖 MỎ NEO CẢM XÚC (ẢNH NGƯỜI THƯƠNG / ĐỘNG LỰC)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1AEC4899))
                .border(1.dp, Color(0x44EC4899), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(2.dp, Color(0xFFEC4899), CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = "Emotional Anchor Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(68.dp)
                        )
                    } else {
                        Text(text = "💖", fontSize = 28.sp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (emotionalAnchor.isNotBlank()) "Đã cài đặt ảnh mỏ neo" else "Chưa cài ảnh mỏ neo",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ảnh sẽ xuất hiện khi lướt quá giới hạn để đánh thức cảm xúc tỉnh thức.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = "📷 Tải ảnh lên", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (previewBitmap != null) {
                            OutlinedButton(
                                onClick = { emotionalAnchor = "" },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(text = "Gỡ ảnh", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── SECTION 5: GEMINI AI API KEY ───
        SettingsSectionHeader(title = "🤖 TRỢ LÝ AI GEMINI (PHẢN HỒI PHẢN TƯ 22H00)")
        OutlinedTextField(
            value = geminiKey,
            onValueChange = { geminiKey = it },
            label = { Text("Google Gemini API Key (Bắt đầu với AIza...)", fontSize = 11.sp) },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Text(
                    text = if (showApiKey) "Ẩn" else "Hiện",
                    color = Color(0xFFA78BFA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showApiKey = !showApiKey }
                        .padding(horizontal = 8.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color(0x33FFFFFF)
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ─── SECTION 6: CONTENT FILTER KEYWORDS ───
        SettingsSectionHeader(title = "🏷️ BỘ LỌC TỪ KHÓA NỘI DUNG (AI & MÁY CỤC BỘ)")
        Text(
            text = "Bộ lọc máy sử dụng các từ khóa này để tự động dán nhãn tức thì khi bạn lướt. Cuối ngày, Gemini AI cũng sẽ tự học và thêm từ khóa mới vào đây.",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x331E293B))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val categories = listOf(
                Triple(0, "🎯 Mục tiêu (${targetKeywords.size})", Color(0xFF10B981)),
                Triple(1, "☕ Giải trí (${leisureKeywords.size})", Color(0xFF0EA5E9)),
                Triple(2, "⚠️ Lạc lối (${distractionKeywords.size})", Color(0xFFEF4444))
            )
            categories.forEach { (index, title, activeColor) ->
                val isSelected = selectedKeywordCategory == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) activeColor.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) activeColor else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedKeywordCategory = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Add Keyword Input Row
        val activeKwColor = when (selectedKeywordCategory) {
            0 -> Color(0xFF10B981)
            1 -> Color(0xFF0EA5E9)
            else -> Color(0xFFEF4444)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newKeywordInput,
                onValueChange = { newKeywordInput = it },
                placeholder = {
                    val hint = when (selectedKeywordCategory) {
                        0 -> "VD: react, python, tài chính, sách..."
                        1 -> "VD: vlog, du lịch, hoạt hình, game..."
                        else -> "VD: drama, hóng hớt, phốt, cá cược..."
                    }
                    Text(hint, fontSize = 11.sp)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = activeKwColor,
                    unfocusedBorderColor = Color(0x33FFFFFF)
                )
            )

            Button(
                onClick = {
                    val clean = newKeywordInput.trim().lowercase()
                    if (clean.isNotBlank()) {
                        when (selectedKeywordCategory) {
                            0 -> {
                                if (targetKeywords.none { it.equals(clean, ignoreCase = true) }) {
                                    targetKeywords = targetKeywords + clean
                                    repository.addKeyword("goal", clean)
                                    Toast.makeText(context, "➕ Đã thêm từ khóa mục tiêu: \"$clean\"", Toast.LENGTH_SHORT).show()
                                }
                            }
                            1 -> {
                                if (leisureKeywords.none { it.equals(clean, ignoreCase = true) }) {
                                    leisureKeywords = leisureKeywords + clean
                                    repository.addKeyword("leisure", clean)
                                    Toast.makeText(context, "➕ Đã thêm từ khóa giải trí: \"$clean\"", Toast.LENGTH_SHORT).show()
                                }
                            }
                            2 -> {
                                if (distractionKeywords.none { it.equals(clean, ignoreCase = true) }) {
                                    distractionKeywords = distractionKeywords + clean
                                    repository.addKeyword("distraction", clean)
                                    Toast.makeText(context, "➕ Đã thêm từ khóa bẫy dopamine: \"$clean\"", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        newKeywordInput = ""
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeKwColor),
                modifier = Modifier.height(48.dp)
            ) {
                Text("➕ Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Keywords Chips Display
        val currentDisplayKeywords = when (selectedKeywordCategory) {
            0 -> targetKeywords
            1 -> leisureKeywords
            else -> distractionKeywords
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1F0F172A))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (currentDisplayKeywords.isEmpty()) {
                Text(
                    text = "Chưa có từ khóa nào trong danh mục này. Hãy thêm từ khóa ở trên!",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentDisplayKeywords.forEach { kw ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeKwColor.copy(alpha = 0.15f))
                                .border(0.8.dp, activeKwColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = kw,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✕",
                                color = activeKwColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        when (selectedKeywordCategory) {
                                            0 -> {
                                                targetKeywords = targetKeywords.filterNot { it.equals(kw, ignoreCase = true) }
                                                repository.removeKeyword("goal", kw)
                                            }
                                            1 -> {
                                                leisureKeywords = leisureKeywords.filterNot { it.equals(kw, ignoreCase = true) }
                                                repository.removeKeyword("leisure", kw)
                                            }
                                            2 -> {
                                                distractionKeywords = distractionKeywords.filterNot { it.equals(kw, ignoreCase = true) }
                                                repository.removeKeyword("distraction", kw)
                                            }
                                        }
                                    }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Reset keywords button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = "↺ Khôi phục từ khóa mặc định",
                color = Color(0xFF94A3B8),
                fontSize = 10.5.sp,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier
                    .clickable {
                        when (selectedKeywordCategory) {
                            0 -> {
                                targetKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_TARGET_KEYWORDS
                                repository.resetKeywords("goal")
                            }
                            1 -> {
                                leisureKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_LEISURE_KEYWORDS
                                repository.resetKeywords("leisure")
                            }
                            2 -> {
                                distractionKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_DISTRACTION_KEYWORDS
                                repository.resetKeywords("distraction")
                            }
                        }
                        Toast.makeText(context, "↺ Đã khôi phục từ khóa mặc định!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── SECTION 7: BEDTIME & REFLECTION SCHEDULE ───
        SettingsSectionHeader(title = "🌙 CHẾ ĐỘ GIỜ ĐI NGỦ & PHẢN TƯ TỐI")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x33A78BFA), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kích hoạt bảo vệ giấc ngủ",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tự động kích hoạt màn hình Đen Trắng trong khung giờ ngủ",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = bedtimeEnabled,
                        onCheckedChange = { bedtimeEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFA78BFA),
                            checkedTrackColor = Color(0x448B5CF6)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bedtimeStart,
                        onValueChange = { bedtimeStart = it },
                        label = { Text("Bắt đầu ngủ (HH:mm)", fontSize = 10.sp) },
                        placeholder = { Text("22:30", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bedtimeEnd,
                        onValueChange = { bedtimeEnd = it },
                        label = { Text("Thức dậy (HH:mm)", fontSize = 10.sp) },
                        placeholder = { Text("05:00", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val sleepDuration = calculateSleepDuration(bedtimeStart, bedtimeEnd)
                Text(
                    text = "💤 Thời gian nghỉ ngơi dự kiến: $sleepDuration",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reflectionReminderTime,
                    onValueChange = { reflectionReminderTime = it },
                    label = { Text("Giờ nhắc nhở phản tư cuối ngày (HH:mm)", fontSize = 10.sp) },
                    placeholder = { Text("21:30", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ─── SECTION 8: QA & TEST SIMULATION MODE ───
        SettingsSectionHeader(title = "🧪 CHẾ ĐỘ THỬ NGHIỆM & MÔ PHỎNG (QA PANEL)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1F3B82F6))
                .border(1.2.dp, Color(0x663B82F6), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bảng điều khiển QA Test Mode",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Mô phỏng tức thì các mốc M1/M2/M3, năng lượng và streak",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = isQaMode,
                        onCheckedChange = { enabled ->
                            repository.setQaMode(enabled)
                            if (enabled) {
                                Toast.makeText(context, "🧪 Đã bật Chế độ Thử nghiệm (QA Mode)", Toast.LENGTH_SHORT).show()
                            } else {
                                testEnergyInput = 100f
                                Toast.makeText(context, "🧹 Đã tắt Chế độ Thử nghiệm: Toàn bộ dữ liệu mẫu đã bị xóa và khôi phục trạng thái ban đầu!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF3B82F6),
                            checkedTrackColor = Color(0x443B82F6)
                        )
                    )
                }

                if (isQaMode) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Nút tạo dữ liệu mẫu 7 ngày
                    Button(
                        onClick = {
                            repository.generateMockHistory(7)
                            Toast.makeText(context, "🧪 Đã tạo dữ liệu mẫu 7 ngày cho Báo cáo dài hạn!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("🧪 Tạo Dữ Liệu Mẫu 7 Ngày (Báo Cáo)", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "⚡ Năng lượng thú cưng: ${petState.energy}⚡",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Presets: 0⚡, 10⚡, 50⚡, 100⚡
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 10, 50, 100).forEach { p ->
                            OutlinedButton(
                                onClick = {
                                    repository.setTestEnergy(p)
                                    testEnergyInput = p.toFloat()
                                    Toast.makeText(context, "⚡ Đã gán năng lượng = $p⚡", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                            ) {
                                Text("$p⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Energy Slider
                    Slider(
                        value = testEnergyInput,
                        onValueChange = { testEnergyInput = it },
                        onValueChangeFinished = {
                            repository.setTestEnergy(testEnergyInput.toInt())
                        },
                        valueRange = 0f..100f,
                        steps = 20,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF60A5FA)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulation Action Buttons
                    Text(text = "Kích hoạt hiệu ứng ma sát:", color = Color(0xFFCBD5E1), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                repository.penalizePet(5, "qa_test_m1")
                                Toast.makeText(context, "⚠️ Đã kích hoạt M1 (-5⚡)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                        ) {
                            Text("M1 (-5⚡)", fontSize = 9.5.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                repository.penalizePet(10, "qa_test_m2")
                                Toast.makeText(context, "🚨 Đã kích hoạt M2 (-10⚡, kích hoạt Grayscale)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                        ) {
                            Text("M2 (-10⚡)", fontSize = 9.5.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                repository.penalizePet(20, "qa_test_m3")
                                Toast.makeText(context, "🛑 Đã kích hoạt M3 (-20⚡, Box Breathing)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                        ) {
                            Text("M3 (-20⚡)", fontSize = 9.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Streak Presets
                    Text(text = "🔥 Chuỗi ngày Streak:", color = Color(0xFFCBD5E1), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 3, 7, 14).forEach { s ->
                            OutlinedButton(
                                onClick = {
                                    repository.setTestStreak(s)
                                    Toast.makeText(context, "🔥 Đã gán Streak = $s ngày", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                            ) {
                                Text("$s ngày", fontSize = 9.5.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reset test data
                    Button(
                        onClick = {
                            repository.resetTestData()
                            testEnergyInput = 100f
                            Toast.makeText(context, "🔄 Đã reset dữ liệu thử nghiệm về mặc định!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444))
                    ) {
                        Text("🔄 Reset Sạch Dữ Liệu Thử Nghiệm", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ─── SAVE CONFIG BUTTON ───
        Button(
            onClick = {
                val updatedThresholds = PlatformThresholds(
                    youtube = YoutubeThresholds(
                        shorts = ThresholdConfig(
                            m1 = ytShortsM1.toIntOrNull() ?: 15,
                            m2 = ytShortsM2.toIntOrNull() ?: 30,
                            m3 = ytShortsM3.toIntOrNull() ?: 45
                        ),
                        long = ThresholdConfig(
                            m1 = ytLongM1.toIntOrNull() ?: 3,
                            m2 = ytLongM2.toIntOrNull() ?: 5,
                            m3 = ytLongM3.toIntOrNull() ?: 8
                        )
                    ),
                    facebook = FacebookThresholds(
                        reels = ThresholdConfig(
                            m1 = fbReelsM1.toIntOrNull() ?: 15,
                            m2 = fbReelsM2.toIntOrNull() ?: 30,
                            m3 = fbReelsM3.toIntOrNull() ?: 45
                        ),
                        feeds = ThresholdConfig(
                            m1 = fbFeedsM1.toIntOrNull() ?: 20,
                            m2 = fbFeedsM2.toIntOrNull() ?: 40,
                            m3 = fbFeedsM3.toIntOrNull() ?: 60
                        ),
                        long = ThresholdConfig(
                            m1 = fbWatchM1.toIntOrNull() ?: 2,
                            m2 = fbWatchM2.toIntOrNull() ?: 4,
                            m3 = fbWatchM3.toIntOrNull() ?: 6
                        )
                    ),
                    tiktok = TiktokThresholds(
                        shorts = ThresholdConfig(
                            m1 = ttShortsM1.toIntOrNull() ?: 15,
                            m2 = ttShortsM2.toIntOrNull() ?: 30,
                            m3 = ttShortsM3.toIntOrNull() ?: 45
                        )
                    )
                )

                val newPlatformLimits = PlatformLimits(
                    youtube = YoutubeLimits(
                        maxShortSwipes = updatedThresholds.youtube.shorts.m1,
                        maxLongVideos = updatedThresholds.youtube.long.m1
                    ),
                    facebook = FacebookLimits(
                        maxFeedPosts = updatedThresholds.facebook.feeds.m1,
                        maxReelsSwipes = updatedThresholds.facebook.reels.m1,
                        maxLongVideos = updatedThresholds.facebook.long.m1
                    ),
                    tiktok = TiktokLimits(
                        maxShortSwipes = updatedThresholds.tiktok.shorts.m1
                    )
                )

                val newLeisureQuota = leisureQuotaText.toIntOrNull() ?: 45
                val cleanSyncCode = syncCodeInput.trim().uppercase().ifBlank { currentConfig.syncCode }
                val updatedConfig = currentConfig.copy(
                    syncCode = cleanSyncCode,
                    masterGoal = masterGoal.trim(),
                    emotionalAnchorImage = emotionalAnchor.trim(),
                    geminiApiKey = geminiKey.trim(),
                    leisureQuotaMinutes = newLeisureQuota,
                    thresholds = updatedThresholds,
                    platformLimits = newPlatformLimits,
                    targetKeywords = targetKeywords,
                    leisureKeywords = leisureKeywords,
                    distractionKeywords = distractionKeywords,
                    bedtime = BedtimeConfig(
                        enabled = bedtimeEnabled,
                        start = bedtimeStart.trim().ifBlank { "22:30" },
                        end = bedtimeEnd.trim().ifBlank { "05:00" }
                    ),
                    reflection = ReflectionConfig(
                        reminderTime = reflectionReminderTime.trim().ifBlank { "21:30" }
                    )
                )
                repository.updateConfig(updatedConfig)
                syncManager.updateSyncCode(cleanSyncCode)

                Toast.makeText(context, "✅ Đã lưu cấu hình, mốc ma sát & giới hạn!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Text(
                text = "💾 LƯU TOÀN BỘ CẤU HÌNH",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MilestoneTripleInput(
    label: String,
    m1Value: String,
    onM1Change: (String) -> Unit,
    m2Value: String,
    onM2Change: (String) -> Unit,
    m3Value: String,
    onM3Change: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mốc 1
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mốc 1",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = m1Value,
                    onValueChange = onM1Change,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    singleLine = true
                )
            }

            // Mốc 2
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mốc 2",
                    color = Color(0xFFF59E0B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = m2Value,
                    onValueChange = onM2Change,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    singleLine = true
                )
            }

            // Mốc 3
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mốc 3",
                    color = Color(0xFFEF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedTextField(
                    value = m3Value,
                    onValueChange = onM3Change,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun PlatformLimitCard(
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun LimitInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 9.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF8B5CF6),
            unfocusedBorderColor = Color(0x33FFFFFF)
        ),
        singleLine = true
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFA78BFA),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

private fun calculateSleepDuration(start: String, end: String): String {
    val sParts = start.split(":").mapNotNull { it.toIntOrNull() }
    val eParts = end.split(":").mapNotNull { it.toIntOrNull() }
    if (sParts.size != 2 || eParts.size != 2) return "6 giờ 30 phút"
    val startMin = sParts[0] * 60 + sParts[1]
    val endMin = eParts[0] * 60 + eParts[1]
    val totalMin = if (endMin >= startMin) endMin - startMin else (24 * 60 - startMin) + endMin
    val h = totalMin / 60
    val m = totalMin % 60
    return if (m == 0) "$h giờ" else "$h giờ $m phút"
}
