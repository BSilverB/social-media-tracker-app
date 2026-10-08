package com.example.social_media_tracker_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.social_media_tracker_app.data.repository.FirebaseSyncManager
import com.example.social_media_tracker_app.data.repository.StatsRepository
import com.example.social_media_tracker_app.domain.pomodoro.PomodoroManager
import com.example.social_media_tracker_app.ui.components.MindfulBottomBar
import com.example.social_media_tracker_app.ui.dashboard.DashboardDialog
import com.example.social_media_tracker_app.ui.navigation.AppTab
import com.example.social_media_tracker_app.ui.overlay.MindfulBreathingOverlay
import com.example.social_media_tracker_app.ui.pet.PetWardrobeModal
import com.example.social_media_tracker_app.ui.screens.BrowserWebViewScreen
import com.example.social_media_tracker_app.ui.screens.HomeScreen
import com.example.social_media_tracker_app.ui.screens.PetScreen
import com.example.social_media_tracker_app.ui.screens.ProfileScreen
import com.example.social_media_tracker_app.ui.screens.ReflectionScreen
import com.example.social_media_tracker_app.ui.screens.SettingsScreen
import com.example.social_media_tracker_app.ui.theme.SocialmediatrackerappTheme

class MainActivity : ComponentActivity() {

    private lateinit var statsRepository: StatsRepository
    private lateinit var pomodoroManager: PomodoroManager
    private lateinit var syncManager: com.example.social_media_tracker_app.data.repository.FirebaseSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        statsRepository = StatsRepository.getInstance(this)
        pomodoroManager = PomodoroManager(statsRepository)
        syncManager = com.example.social_media_tracker_app.data.repository.FirebaseSyncManager.getInstance(this, statsRepository)

        // Tự động đẩy mobile stats với debounce 2s khi có thay đổi
        statsRepository.onStatsChangedListener = { updatedStats ->
            syncManager.schedulePushMobileStats(updatedStats)
        }

        // Tự động đẩy cấu hình lên Firebase ngay tức thì khi người dùng thay đổi cài đặt
        statsRepository.onConfigChangedListener = { updatedConfig ->
            syncManager.pushConfigImmediately(updatedConfig)
        }

        setContent {
            SocialmediatrackerappTheme {
                MindfulAppRoot(
                    repository = statsRepository,
                    pomodoroManager = pomodoroManager,
                    syncManager = syncManager,
                    onCloseApp = { finish() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        syncManager.onAppExitOrBackground(statsRepository.petState.value)
    }

    override fun onStop() {
        super.onStop()
        syncManager.onAppExitOrBackground(statsRepository.petState.value)
    }
}

@Composable
fun MindfulAppRoot(
    repository: StatsRepository,
    pomodoroManager: PomodoroManager,
    syncManager: FirebaseSyncManager,
    onCloseApp: () -> Unit
) {
    val context = LocalContext.current

    // Offline Idle Recovery Check (+5⚡/30m)
    LaunchedEffect(Unit) {
        val (recovered, msg) = repository.checkIdleRecovery()
        if (recovered > 0 && msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    // Navigation State
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var activePlatformUrl by remember { mutableStateOf<String?>(null) }
    var activePlatformName by remember { mutableStateOf("Mạng Xã Hội") }

    // Dialog States
    var showWardrobeModal by remember { mutableStateOf(false) }
    var showDashboardModal by remember { mutableStateOf(false) }
    var showBreathingOverlay by remember { mutableStateOf(false) }

    // Repository States
    val stats by repository.todayStats.collectAsState()
    val appConfig by repository.appConfig.collectAsState()
    val petState by repository.petState.collectAsState()
    val pomodoroState by pomodoroManager.state.collectAsState()

    // If an active social media platform is launched, show BrowserWebView full-screen
    if (activePlatformUrl != null) {
        BrowserWebViewScreen(
            targetUrl = activePlatformUrl!!,
            platformName = activePlatformName,
            repository = repository,
            pomodoroManager = pomodoroManager,
            onCloseBrowser = { activePlatformUrl = null }
        )
        return
    }

    // Hardware Back Handler on Main Dashboard
    BackHandler(enabled = true) {
        when {
            showBreathingOverlay -> showBreathingOverlay = false
            showWardrobeModal -> showWardrobeModal = false
            showDashboardModal -> showDashboardModal = false
            isSettingsOpen -> isSettingsOpen = false
            selectedTab != AppTab.DASHBOARD -> selectedTab = AppTab.DASHBOARD
            else -> onCloseApp()
        }
    }

    // Main 4-Tab Scaffold
    Scaffold(
        bottomBar = {
            MindfulBottomBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    isSettingsOpen = false
                }
            )
        },
        containerColor = Color(0xFF090D16)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.DASHBOARD -> {
                    HomeScreen(
                        stats = stats,
                        petState = petState,
                        pomodoroState = pomodoroState,
                        appConfig = appConfig,
                        onPokePet = { repository.pokePet() },
                        onOpenWardrobe = { showWardrobeModal = true },
                        onTogglePomodoro = { pomodoroManager.toggle() },
                        onStartPomodoroSession = { cycles, focus, brk, mode ->
                            pomodoroManager.startNewSession(cycles, focus, brk, mode)
                        },
                        onTriggerBreathing = { showBreathingOverlay = true },
                        onLaunchPlatform = { url, name ->
                            activePlatformUrl = url
                            activePlatformName = name
                        },
                        onUpdateVideoCategory = { videoId, platform, newCat ->
                            repository.updateVideoCategory(videoId, platform, newCat)
                        }
                    )
                }

                AppTab.REFLECTION -> {
                    ReflectionScreen(
                        stats = stats,
                        masterGoal = appConfig.masterGoal,
                        geminiApiKey = appConfig.geminiApiKey,
                        targetKeywords = appConfig.targetKeywords,
                        onSaveReflection = { lesson1, lesson2, rating ->
                            repository.saveReflection(lesson1, lesson2, rating)
                        },
                        onApplyAiFeedback = { result ->
                            repository.applyAiClassificationAndLearnedKeywords(
                                reclassifiedMap = result.reclassifiedVideos,
                                learnedTargetKeywords = result.learnedTargetKeywords,
                                learnedLeisureKeywords = result.learnedLeisureKeywords,
                                learnedDistractionKeywords = result.learnedDistractionKeywords
                            )
                        }
                    )
                }

                AppTab.PET -> {
                    PetScreen(
                        repository = repository,
                        syncManager = syncManager,
                        onTriggerBreathing = { showBreathingOverlay = true }
                    )
                }

                AppTab.PROFILE -> {
                    if (!isSettingsOpen) {
                        ProfileScreen(
                            repository = repository,
                            onOpenSettings = { isSettingsOpen = true }
                        )
                    } else {
                        SettingsScreen(
                            repository = repository,
                            syncManager = syncManager,
                            onBack = { isSettingsOpen = false }
                        )
                    }
                }
            }
        }
    }

    // Global Modal: Pet Wardrobe
    if (showWardrobeModal) {
        PetWardrobeModal(
            petState = petState,
            onEquip = { headItem -> repository.equipAccessory(headItem) },
            onDismiss = { showWardrobeModal = false }
        )
    }

    // Global Modal: Long-term Analytics Dashboard
    if (showDashboardModal) {
        DashboardDialog(
            repository = repository,
            onDismiss = { showDashboardModal = false }
        )
    }

    // Global Modal: Box Breathing 12s
    if (showBreathingOverlay) {
        MindfulBreathingOverlay(
            masterGoal = appConfig.masterGoal,
            currentSwipes = stats.totalSwipes,
            threshold = appConfig.thresholds.m3,
            petState = petState,
            onDismiss = { showBreathingOverlay = false },
            onCloseApp = { showBreathingOverlay = false },
            onResistTemptation = { repository.recordTemptation(true) },
            onSuccumbTemptation = { repository.recordTemptation(false) }
        )
    }
}