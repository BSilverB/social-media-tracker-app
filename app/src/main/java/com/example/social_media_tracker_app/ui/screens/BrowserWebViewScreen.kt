package com.example.social_media_tracker_app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.social_media_tracker_app.data.repository.StatsRepository
import com.example.social_media_tracker_app.domain.pomodoro.PomodoroManager
import com.example.social_media_tracker_app.ui.hud.FloatingHud
import com.example.social_media_tracker_app.ui.overlay.IntentionalEntryModal
import com.example.social_media_tracker_app.ui.overlay.Milestone2Modal
import com.example.social_media_tracker_app.ui.overlay.MindfulBreathingOverlay
import com.example.social_media_tracker_app.ui.overlay.PetExtensionModal
import com.example.social_media_tracker_app.ui.overlay.StudyCheckInDialog
import com.example.social_media_tracker_app.ui.pet.PetWardrobeModal
import com.example.social_media_tracker_app.ui.reflection.DailyReflectionDialog
import com.example.social_media_tracker_app.ui.webview.MindfulWebView
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun BrowserWebViewScreen(
    targetUrl: String,
    platformName: String,
    repository: StatsRepository,
    pomodoroManager: PomodoroManager,
    onCloseBrowser: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val stats by repository.todayStats.collectAsState()
    val appConfig by repository.appConfig.collectAsState()
    val petState by repository.petState.collectAsState()
    val pomodoroState by pomodoroManager.state.collectAsState()
    val activityState by repository.currentActivity.collectAsState()
    val isNavVisible by repository.isScrollingUp.collectAsState()
    val currentVideoCategory by repository.currentVideoCategory.collectAsState()
    val distractionWarning by repository.distractionWarning.collectAsState()

    var hasDeclaredSessionIntent by remember { mutableStateOf(false) }
    var sessionTargetMinutes by remember { mutableIntStateOf(15) }
    var sessionRemainingSeconds by remember { mutableIntStateOf(15 * 60) }
    var showExtensionModal by remember { mutableStateOf(false) }

    var showBreathingOverlay by remember { mutableStateOf(false) }
    var showWardrobeModal by remember { mutableStateOf(false) }
    var showReflectionModal by remember { mutableStateOf(false) }
    var showM2Modal by remember { mutableStateOf(false) }

    // Fullscreen video state
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var hideCustomViewAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isFullscreenHudExpanded by remember { mutableStateOf(false) }

    DisposableEffect(customVideoView) {
        if (customVideoView != null) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Study Check-In state
    var pendingCheckInVideoId by remember { mutableStateOf<String?>(null) }
    var pendingCheckInTitle by remember { mutableStateOf<String?>(null) }

    // Milestone thresholds & Grace swipes tracking
    var m1Triggered by remember { mutableStateOf(false) }
    var m2Triggered by remember { mutableStateOf(false) }
    var m3Triggered by remember { mutableStateOf(false) }
    var graceSwipesRemaining by remember { mutableIntStateOf(0) }
    var lastRecordedSwipes by remember { mutableIntStateOf(stats.totalSwipes) }

    val m1Milestone = appConfig.thresholds.m1
    val m2Milestone = appConfig.thresholds.m2
    val redMilestone = appConfig.thresholds.m3

    // Check Bedtime and Grayscale Demotivation Mode (Extension alignment)
    val now = remember { Calendar.getInstance() }
    val currentMinutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val isBedtime = remember(appConfig.bedtime, currentMinutesOfDay) {
        if (!appConfig.bedtime.enabled) false
        else {
            val startParts = appConfig.bedtime.startTime.split(":").mapNotNull { it.toIntOrNull() }
            val endParts = appConfig.bedtime.endTime.split(":").mapNotNull { it.toIntOrNull() }
            val startM = if (startParts.size == 2) startParts[0] * 60 + startParts[1] else 22 * 60 + 30
            val endM = if (endParts.size == 2) endParts[0] * 60 + endParts[1] else 5 * 60
            if (startM > endM) {
                currentMinutesOfDay >= startM || currentMinutesOfDay < endM
            } else {
                currentMinutesOfDay in startM until endM
            }
        }
    }
    val isOverM2 = stats.totalSwipes >= m2Milestone
    val isLowEnergy = petState.energy < 15
    val isMusicFocus = pomodoroState.isFocusSession && pomodoroState.focusMode == "music"
    val isStudyFocus = pomodoroState.isFocusSession && pomodoroState.focusMode == "study"

    // Grayscale ACTIVE if: pet < 15⚡ OR isBedtime OR isOverM2 OR music focus
    val shouldGrayscale = when {
        pomodoroState.isBreakSession -> false
        isStudyFocus -> false
        isLowEnergy || isBedtime || isOverM2 || isMusicFocus -> true
        else -> false
    }

    LaunchedEffect(shouldGrayscale, webViewRef) {
        webViewRef?.evaluateJavascript(
            "if (typeof window.__setGrayscale === 'function') window.__setGrayscale($shouldGrayscale);",
            null
        )
    }

    // Sync Pomodoro focus state to WebView tracker script
    LaunchedEffect(pomodoroState.isFocusSession, pomodoroState.focusMode, webViewRef) {
        webViewRef?.evaluateJavascript(
            "if (typeof window.__setPomodoroFocusState === 'function') window.__setPomodoroFocusState(${pomodoroState.isFocusSession}, '${pomodoroState.focusMode}');",
            null
        )
    }

    // Milestone Penalties and Grace Swipes Handling
    LaunchedEffect(stats.totalSwipes) {
        val currentSwipes = stats.totalSwipes

        // M1 (-5⚡)
        if (currentSwipes >= m1Milestone && !m1Triggered) {
            m1Triggered = true
            repository.penalizePet(5, "milestone_1")
            Toast.makeText(
                context,
                "⚠️ Mốc 1 ($m1Milestone lượt): Năng lượng thú cưng giảm -5⚡!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // M2 (-10⚡ + Modal + Grayscale)
        if (currentSwipes >= m2Milestone && !m2Triggered) {
            m2Triggered = true
            repository.penalizePet(10, "milestone_2")
            showM2Modal = true
        }

        // M3 (-20⚡ + Box Breathing + 5 Grace Swipes)
        if (currentSwipes >= redMilestone && !m3Triggered && !pomodoroState.isBreakSession) {
            m3Triggered = true
            repository.penalizePet(20, "milestone_3")
            showBreathingOverlay = true
            graceSwipesRemaining = 5
        } else if (m3Triggered && currentSwipes > lastRecordedSwipes) {
            val diff = currentSwipes - lastRecordedSwipes
            if (graceSwipesRemaining > 0) {
                graceSwipesRemaining = (graceSwipesRemaining - diff).coerceAtLeast(0)
            } else {
                repository.penalizePet(5 * diff, "post_grace_swipe")
                Toast.makeText(
                    context,
                    "🚨 Vượt mốc M3: Trừ -${5 * diff}⚡ do lướt vô thức!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        lastRecordedSwipes = currentSwipes
    }

    // Session Countdown Timer
    LaunchedEffect(hasDeclaredSessionIntent, showExtensionModal) {
        if (hasDeclaredSessionIntent && !showExtensionModal) {
            while (sessionRemainingSeconds > 0) {
                delay(1000L)
                sessionRemainingSeconds--
            }
            if (sessionRemainingSeconds <= 0 && !showExtensionModal) {
                showExtensionModal = true
            }
        }
    }

    // Hardware / Phone Gesture Back Navigation
    BackHandler(enabled = true) {
        when {
            customVideoView != null -> {
                hideCustomViewAction?.invoke()
                customVideoView = null
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            pendingCheckInVideoId != null -> { /* Force user action in study check in */ }
            showM2Modal -> showM2Modal = false
            showExtensionModal -> showExtensionModal = false
            showBreathingOverlay -> showBreathingOverlay = false
            showWardrobeModal -> showWardrobeModal = false
            showReflectionModal -> showReflectionModal = false
            webViewRef?.canGoBack() == true -> webViewRef?.goBack()
            else -> onCloseBrowser()
        }
    }

    // Formatted session remaining time
    val sessionMinutes = sessionRemainingSeconds / 60
    val sessionSecs = sessionRemainingSeconds % 60
    val sessionTimerFormatted = String.format("%02d:%02d", sessionMinutes, sessionSecs)
    val hudTimerLabel = when {
        pomodoroState.statusLabel.isNotBlank() && hasDeclaredSessionIntent ->
            "${pomodoroState.statusLabel} • ⏳ $sessionTimerFormatted"
        pomodoroState.statusLabel.isNotBlank() -> pomodoroState.statusLabel
        hasDeclaredSessionIntent -> "⏳ $sessionTimerFormatted"
        else -> ""
    }

    // ─── FULLSCREEN VIDEO CONTAINER ───
    if (customVideoView != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { customVideoView!! },
                modifier = Modifier.fillMaxSize()
            )

            // Sleek Discreet Mini Badge / Side-tab on edge
            if (!isFullscreenHudExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC0F172A))
                        .border(1.dp, Color(0x668B5CF6), RoundedCornerShape(20.dp))
                        .clickable { isFullscreenHudExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🐾 ${petState.energy}⚡", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "⏳ $sessionTimerFormatted", color = Color.White, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "◀", color = Color(0xFFA78BFA), fontSize = 10.sp)
                    }
                }
            } else {
                // Expanded HUD Panel in Fullscreen
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF00F172A))
                        .border(1.2.dp, Color(0xFF8B5CF6), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Mindful HUD", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "▶ Ẩn vào cạnh",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { isFullscreenHudExpanded = false }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Năng lượng: ${petState.energy}/100⚡ • ⏳ $sessionTimerFormatted", color = Color.White, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    hideCustomViewAction?.invoke()
                                    customVideoView = null
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "⤓ Thu Nhỏ", fontSize = 10.sp, color = Color(0xFF38BDF8))
                            }
                            Button(
                                onClick = {
                                    hideCustomViewAction?.invoke()
                                    customVideoView = null
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    onCloseBrowser()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Thoát MXH", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Main Edge-Aware Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Layer 1: Mindful WebView Container
        MindfulWebView(
            repository = repository,
            targetUrl = targetUrl,
            modifier = Modifier.fillMaxSize(),
            onStudyCheckInNeeded = { videoId, title ->
                pendingCheckInVideoId = videoId
                pendingCheckInTitle = title
            },
            onCustomViewShown = { view, hideAction ->
                customVideoView = view
                hideCustomViewAction = hideAction
            },
            onCustomViewHidden = {
                customVideoView = null
                hideCustomViewAction = null
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            },
            onWebViewCreated = { webViewRef = it }
        )

        // Layer 2: Left-edge subtle horizontal swipe gesture to exit/go back
        var totalHorizontalDrag by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(24.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalHorizontalDrag = 0f },
                        onDragEnd = {
                            if (totalHorizontalDrag > 120f) {
                                if (webViewRef?.canGoBack() == true) {
                                    webViewRef?.goBack()
                                } else {
                                    onCloseBrowser()
                                }
                            }
                            totalHorizontalDrag = 0f
                        },
                        onDragCancel = { totalHorizontalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalHorizontalDrag += dragAmount
                        }
                    )
                }
        )

        // Layer 3: Floating Realtime HUD Badge
        FloatingHud(
            stats = stats,
            petState = petState,
            activityState = activityState,
            pomodoroLabel = hudTimerLabel,
            isBreakSession = pomodoroState.isBreakSession,
            isNavVisible = isNavVisible,
            currentVideoCategory = currentVideoCategory,
            distractionWarning = distractionWarning,
            onPokePet = { repository.pokePet() },
            onTogglePomodoro = { pomodoroManager.toggle() },
            onOpenWardrobe = { showWardrobeModal = true },
            onOpenReflection = { showReflectionModal = true },
            onOpenDashboard = { /* Dashboard accessible via Profile tab */ },
            onTriggerBreathing = { showBreathingOverlay = true },
            onExitBrowser = onCloseBrowser
        )

        // Layer 4: Intentional Entry Modal with Emotional Anchor & Time Config
        if (!hasDeclaredSessionIntent) {
            IntentionalEntryModal(
                masterGoal = appConfig.masterGoal,
                emotionalAnchorImage = appConfig.emotionalAnchorImage,
                platformName = platformName,
                onSubmitIntent = { intentText, minutes ->
                    hasDeclaredSessionIntent = true
                    sessionTargetMinutes = minutes
                    sessionRemainingSeconds = minutes * 60
                    repository.setSessionIntent(intentText)
                    Toast.makeText(
                        context,
                        "🎯 Phiên $platformName ($minutes phút): $intentText",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        // Layer 5: Pet Session Extension Modal
        if (showExtensionModal) {
            PetExtensionModal(
                petState = petState,
                sessionMinutes = sessionTargetMinutes,
                onExtend = { extraMinutes, energyCost ->
                    repository.penalizePet(energyCost, "session_extension")
                    sessionTargetMinutes += extraMinutes
                    sessionRemainingSeconds = extraMinutes * 60
                    showExtensionModal = false
                    Toast.makeText(
                        context,
                        "⏱ Đã gia hạn +$extraMinutes phút (-$energyCost⚡ cho linh vật)",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onStopAndExit = {
                    repository.rewardPet(10, "session_stopped_on_time")
                    showExtensionModal = false
                    Toast.makeText(
                        context,
                        "🌟 Tuyệt vời! Bạn dừng lướt đúng hẹn (+10⚡ thưởng cho linh vật)",
                        Toast.LENGTH_SHORT
                    ).show()
                    onCloseBrowser()
                }
            )
        }

        // Layer 6: Box Breathing 12s Overlay
        if (showBreathingOverlay) {
            MindfulBreathingOverlay(
                masterGoal = appConfig.masterGoal,
                currentSwipes = stats.totalSwipes,
                threshold = redMilestone,
                onDismiss = {
                    showBreathingOverlay = false
                    graceSwipesRemaining = 5
                    Toast.makeText(context, "🔑 Bạn đã bình tâm và nhận 5 lượt hoãn!", Toast.LENGTH_SHORT).show()
                },
                onCloseApp = onCloseBrowser,
                onResistTemptation = { repository.recordTemptation(true) },
                onSuccumbTemptation = { repository.recordTemptation(false) }
            )
        }

        // Layer 7: Pet Wardrobe Modal
        if (showWardrobeModal) {
            PetWardrobeModal(
                petState = petState,
                onEquip = { headItem -> repository.equipAccessory(headItem) },
                onDismiss = { showWardrobeModal = false }
            )
        }

        // Layer 8: Daily Reflection Modal
        if (showReflectionModal) {
            DailyReflectionDialog(
                stats = stats,
                onSaveReflection = { lesson1, lesson2, rating ->
                    repository.saveReflection(lesson1, lesson2, rating)
                    Toast.makeText(context, "🌟 Đã lưu phản tư và thưởng +15⚡ cho linh vật!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showReflectionModal = false }
            )
        }

        // Layer 9: Milestone 2 Center Friction Modal
        if (showM2Modal) {
            Milestone2Modal(
                currentCount = stats.totalSwipes,
                unitLabel = "lượt lướt",
                petState = petState,
                onCloseBrowser = onCloseBrowser,
                onDismiss = { showM2Modal = false }
            )
        }

        // Layer 10: Pomodoro Study Check-In Modal
        if (pendingCheckInVideoId != null) {
            StudyCheckInDialog(
                videoTitle = pendingCheckInTitle ?: "Video học tập",
                petState = petState,
                onConfirmGoal = { intentText ->
                    repository.recordStudyCheckIn(pendingCheckInVideoId!!, pendingCheckInTitle ?: "", intentText, true)
                    pendingCheckInVideoId = null
                    pendingCheckInTitle = null
                    Toast.makeText(context, "✅ Đã xác nhận video phục vụ mục tiêu học tập!", Toast.LENGTH_SHORT).show()
                },
                onRejectDistraction = {
                    repository.recordStudyCheckIn(pendingCheckInVideoId!!, pendingCheckInTitle ?: "", "Lạc lối trong giờ học", false)
                    repository.penalizePet(10, "study_distraction")
                    pendingCheckInVideoId = null
                    pendingCheckInTitle = null
                    Toast.makeText(context, "⚠️ Cảnh báo: Trừ -10⚡ do xem video giải trí trong giờ học!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
