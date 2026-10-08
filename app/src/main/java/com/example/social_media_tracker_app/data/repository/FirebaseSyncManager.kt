package com.example.social_media_tracker_app.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.social_media_tracker_app.data.model.AppConfig
import com.example.social_media_tracker_app.data.model.BedtimeConfig
import com.example.social_media_tracker_app.data.model.DailyStats
import com.example.social_media_tracker_app.data.model.FacebookThresholds
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.data.model.PlatformThresholds
import com.example.social_media_tracker_app.data.model.PomodoroConfig
import com.example.social_media_tracker_app.data.model.ReflectionConfig
import com.example.social_media_tracker_app.data.model.ThresholdConfig
import com.example.social_media_tracker_app.data.model.TiktokThresholds
import com.example.social_media_tracker_app.data.model.YoutubeThresholds
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudSyncDeviceData(
    val swipes: Int = 0,
    val validViews: Int = 0,
    val activeSeconds: Long = 0L,
    val lastUpdated: Long = 0L
)

data class CloudSharedState(
    val streak: Int = 0,
    val petEnergy: Int = 100,
    val petMood: String = "happy",
    val lastActiveDevice: String = "mobile",
    val lastExitTimestamp: Long = 0L
)

class FirebaseSyncManager(
    private val context: Context,
    private val statsRepository: StatsRepository
) {
    companion object {
        private const val TAG = "FirebaseSyncManager"

        @Volatile
        private var INSTANCE: FirebaseSyncManager? = null

        fun getInstance(context: Context, statsRepository: StatsRepository): FirebaseSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseSyncManager(context.applicationContext, statsRepository).also { INSTANCE = it }
            }
        }
    }

    private val db: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining FirebaseDatabase instance", e)
            FirebaseDatabase.getInstance("https://mindful-tracker-bc869-default-rtdb.asia-southeast1.firebasedatabase.app")
        }
    }

    private var currentSyncCode: String = statsRepository.appConfig.value.syncCode
    private var syncRef: DatabaseReference? = null
    private var syncListener: ValueEventListener? = null

    // State flows
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _desktopStats = MutableStateFlow(CloudSyncDeviceData())
    val desktopStats: StateFlow<CloudSyncDeviceData> = _desktopStats.asStateFlow()

    private val _sharedState = MutableStateFlow(CloudSharedState())
    val sharedState: StateFlow<CloudSharedState> = _sharedState.asStateFlow()

    // Debounce handler (2 seconds)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingMobileSyncRunnable: Runnable? = null

    init {
        startSync(currentSyncCode)
    }

    fun updateSyncCode(newCode: String) {
        val clean = newCode.trim().uppercase()
        if (clean.isNotBlank() && clean != currentSyncCode) {
            currentSyncCode = clean
            val currentConfig = statsRepository.appConfig.value
            statsRepository.updateConfig(currentConfig.copy(syncCode = clean))
            startSync(clean)
        }
    }

    @Synchronized
    fun startSync(syncCode: String) {
        stopSync()
        currentSyncCode = syncCode.trim().uppercase()
        if (currentSyncCode.isBlank()) return

        try {
            val ref = db.getReference("users/$currentSyncCode")
            syncRef = ref

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _isConnected.value = true
                    val today = DailyStats.getTodayDateString()

                    // 1. Đọc số liệu Desktop hôm nay
                    val historySnap = snapshot.child("history")
                    val todaySnap = if (historySnap.hasChild(today)) historySnap.child(today) else snapshot.child(today)

                    if (todaySnap.hasChild("desktop")) {
                        val dSnap = todaySnap.child("desktop")
                        val dUpdated = dSnap.child("lastUpdated").getValue(Long::class.java) ?: 0L

                        val ytSwipes = dSnap.child("youtube/shorts/swipes").getValue(Int::class.java) ?: 0
                        val fbReels = dSnap.child("facebook/reels/swipes").getValue(Int::class.java) ?: 0
                        val fbFeed = dSnap.child("facebook/feed/postsScrolled").getValue(Int::class.java) ?: 0
                        val ttSwipes = dSnap.child("tiktok/shorts/swipes").getValue(Int::class.java) ?: 0
                        val calcSwipes = ytSwipes + fbReels + fbFeed + ttSwipes
                        val dSwipes = dSnap.child("swipes").getValue(Int::class.java) ?: calcSwipes

                        val ytValids = dSnap.child("youtube/shorts/validViews").getValue(Int::class.java) ?: 0
                        val fbValids = dSnap.child("facebook/reels/validViews").getValue(Int::class.java) ?: 0
                        val fbFeedRead = dSnap.child("facebook/feed/postsRead").getValue(Int::class.java) ?: 0
                        val ttValids = dSnap.child("tiktok/shorts/validViews").getValue(Int::class.java) ?: 0
                        val calcValids = ytValids + fbValids + fbFeedRead + ttValids
                        val dValids = dSnap.child("validViews").getValue(Int::class.java) ?: calcValids

                        val ytSec = dSnap.child("youtube/activeSeconds").getValue(Long::class.java) ?: 0L
                        val fbSec = dSnap.child("facebook/activeSeconds").getValue(Long::class.java) ?: 0L
                        val ttSec = dSnap.child("tiktok/activeSeconds").getValue(Long::class.java) ?: 0L
                        val calcSec = ytSec + fbSec + ttSec
                        val dSec = dSnap.child("activeSeconds").getValue(Long::class.java) ?: calcSec

                        _desktopStats.value = CloudSyncDeviceData(
                            swipes = dSwipes,
                            validViews = dValids,
                            activeSeconds = dSec,
                            lastUpdated = dUpdated
                        )
                        statsRepository.updateDesktopSwipes(dSwipes)
                    }

                    // 2. Đọc sharedState
                    if (snapshot.hasChild("sharedState")) {
                        val sSnap = snapshot.child("sharedState")
                        val streak = sSnap.child("streak").getValue(Int::class.java) ?: 0
                        val petEnergy = sSnap.child("petEnergy").getValue(Int::class.java) ?: 100
                        val petMood = sSnap.child("petMood").getValue(String::class.java) ?: "happy"
                        val lastDevice = sSnap.child("lastActiveDevice").getValue(String::class.java) ?: "mobile"
                        val lastExit = sSnap.child("lastExitTimestamp").getValue(Long::class.java) ?: 0L

                        val newShared = CloudSharedState(
                            streak = streak,
                            petEnergy = petEnergy,
                            petMood = petMood,
                            lastActiveDevice = lastDevice,
                            lastExitTimestamp = lastExit
                        )
                        _sharedState.value = newShared

                        // Conflict resolution cho Pet: nếu trên cloud thấp hơn điểm hiện tại hoặc streak cao hơn
                        val currentPet = statsRepository.petState.value
                        if (petEnergy < currentPet.energy) {
                            statsRepository.penalizePet(currentPet.energy - petEnergy, "cloud_sync_penalize")
                        }
                        if (streak > currentPet.streakDays) {
                            statsRepository.setTestStreak(streak)
                        }
                    }

                    // 3. Đọc config từ cloud nếu có cập nhật mới
                    if (snapshot.hasChild("config")) {
                        val cSnap = snapshot.child("config")
                        val currentCfg = statsRepository.appConfig.value
                        var changed = false
                        var updatedCfg = currentCfg

                        val masterGoal = cSnap.child("masterGoal").getValue(String::class.java)
                        if (!masterGoal.isNullOrBlank() && masterGoal != currentCfg.masterGoal) {
                            updatedCfg = updatedCfg.copy(masterGoal = masterGoal)
                            changed = true
                        }

                        val leisureQuota = cSnap.child("leisureQuotaMinutes").getValue(Int::class.java)
                        if (leisureQuota != null && leisureQuota > 0 && leisureQuota != currentCfg.leisureQuotaMinutes) {
                            updatedCfg = updatedCfg.copy(leisureQuotaMinutes = leisureQuota)
                            changed = true
                        }

                        // Thresholds parser
                        if (cSnap.hasChild("thresholds")) {
                            val tSnap = cSnap.child("thresholds")
                            val yShortsM1 = tSnap.child("youtube/shorts/m1").getValue(Int::class.java) ?: currentCfg.thresholds.youtube.shorts.m1
                            val yShortsM2 = tSnap.child("youtube/shorts/m2").getValue(Int::class.java) ?: currentCfg.thresholds.youtube.shorts.m2
                            val yShortsM3 = tSnap.child("youtube/shorts/m3").getValue(Int::class.java) ?: currentCfg.thresholds.youtube.shorts.m3
                            val yLongM1 = tSnap.child("youtube/long/m1").getValue(Int::class.java) ?: currentCfg.thresholds.youtube.long.m1
                            val yLongM2 = tSnap.child("youtube/long/m2").getValue(Int::class.java) ?: currentCfg.thresholds.youtube.long.m2
                            val yLongM3 = tSnap.child("youtube/long/m3").getValue(Int::class.java) ?: currentCfg.thresholds.youtube.long.m3

                            val fbReelsM1 = tSnap.child("facebook/reels/m1").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.reels.m1
                            val fbReelsM2 = tSnap.child("facebook/reels/m2").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.reels.m2
                            val fbReelsM3 = tSnap.child("facebook/reels/m3").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.reels.m3
                            val fbFeedsM1 = tSnap.child("facebook/feeds/m1").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.feeds.m1
                            val fbFeedsM2 = tSnap.child("facebook/feeds/m2").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.feeds.m2
                            val fbFeedsM3 = tSnap.child("facebook/feeds/m3").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.feeds.m3
                            val fbLongM1 = tSnap.child("facebook/long/m1").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.long.m1
                            val fbLongM2 = tSnap.child("facebook/long/m2").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.long.m2
                            val fbLongM3 = tSnap.child("facebook/long/m3").getValue(Int::class.java) ?: currentCfg.thresholds.facebook.long.m3

                            val ttShortsM1 = tSnap.child("tiktok/shorts/m1").getValue(Int::class.java) ?: currentCfg.thresholds.tiktok.shorts.m1
                            val ttShortsM2 = tSnap.child("tiktok/shorts/m2").getValue(Int::class.java) ?: currentCfg.thresholds.tiktok.shorts.m2
                            val ttShortsM3 = tSnap.child("tiktok/shorts/m3").getValue(Int::class.java) ?: currentCfg.thresholds.tiktok.shorts.m3

                            val newThresholds = PlatformThresholds(
                                youtube = YoutubeThresholds(
                                    shorts = ThresholdConfig(yShortsM1, yShortsM2, yShortsM3),
                                    long = ThresholdConfig(yLongM1, yLongM2, yLongM3)
                                ),
                                facebook = FacebookThresholds(
                                    reels = ThresholdConfig(fbReelsM1, fbReelsM2, fbReelsM3),
                                    feeds = ThresholdConfig(fbFeedsM1, fbFeedsM2, fbFeedsM3),
                                    long = ThresholdConfig(fbLongM1, fbLongM2, fbLongM3)
                                ),
                                tiktok = TiktokThresholds(
                                    shorts = ThresholdConfig(ttShortsM1, ttShortsM2, ttShortsM3)
                                )
                            )

                            if (newThresholds != currentCfg.thresholds) {
                                updatedCfg = updatedCfg.copy(thresholds = newThresholds)
                                changed = true
                            }
                        } else {
                            val t1 = cSnap.child("threshold1").getValue(Int::class.java)
                            val t2 = cSnap.child("threshold2").getValue(Int::class.java)
                            val t3 = cSnap.child("threshold3").getValue(Int::class.java)
                            if (t1 != null && t2 != null && t3 != null) {
                                if (t1 != currentCfg.thresholds.m1 || t2 != currentCfg.thresholds.m2 || t3 != currentCfg.thresholds.m3) {
                                    updatedCfg = updatedCfg.copy(
                                        thresholds = currentCfg.thresholds.withGlobalShortsThreshold(t1, t2, t3)
                                    )
                                    changed = true
                                }
                            }
                        }

                        // Pomodoro parser
                        if (cSnap.hasChild("pomodoro")) {
                            val pSnap = cSnap.child("pomodoro")
                            val enabled = pSnap.child("enabled").getValue(Boolean::class.java) ?: currentCfg.pomodoro.enabled
                            val focus = pSnap.child("focusMinutes").getValue(Int::class.java) ?: currentCfg.pomodoro.focusMinutes
                            val brk = pSnap.child("breakMinutes").getValue(Int::class.java) ?: currentCfg.pomodoro.breakMinutes
                            val newPomo = PomodoroConfig(enabled, focus, brk)
                            if (newPomo != currentCfg.pomodoro) {
                                updatedCfg = updatedCfg.copy(pomodoro = newPomo)
                                changed = true
                            }
                        }

                        // Bedtime parser
                        if (cSnap.hasChild("bedtime")) {
                            val bSnap = cSnap.child("bedtime")
                            val enabled = bSnap.child("enabled").getValue(Boolean::class.java) ?: currentCfg.bedtime.enabled
                            val start = bSnap.child("start").getValue(String::class.java) ?: currentCfg.bedtime.start
                            val end = bSnap.child("end").getValue(String::class.java) ?: currentCfg.bedtime.end
                            val newBed = BedtimeConfig(enabled, start, end)
                            if (newBed != currentCfg.bedtime) {
                                updatedCfg = updatedCfg.copy(bedtime = newBed)
                                changed = true
                            }
                        }

                        // Reflection reminder parser
                        if (cSnap.hasChild("reflection")) {
                            val rSnap = cSnap.child("reflection")
                            val reminderTime = rSnap.child("reminderTime").getValue(String::class.java) ?: currentCfg.reflection.reminderTime
                            val newRef = ReflectionConfig(reminderTime)
                            if (newRef != currentCfg.reflection) {
                                updatedCfg = updatedCfg.copy(reflection = newRef)
                                changed = true
                            }
                        }

                        // Keywords parser
                        if (cSnap.hasChild("keywords")) {
                            val kSnap = cSnap.child("keywords")
                            val targetKws = kSnap.child("target").children.mapNotNull { it.getValue(String::class.java) }
                            val leisureKws = kSnap.child("leisure").children.mapNotNull { it.getValue(String::class.java) }
                            val distractionKws = kSnap.child("distraction").children.mapNotNull { it.getValue(String::class.java) }

                            var kwChanged = false
                            var targetFinal = currentCfg.targetKeywords
                            var leisureFinal = currentCfg.leisureKeywords
                            var distractionFinal = currentCfg.distractionKeywords

                            if (targetKws.isNotEmpty() && targetKws != currentCfg.targetKeywords) {
                                targetFinal = targetKws
                                kwChanged = true
                            }
                            if (leisureKws.isNotEmpty() && leisureKws != currentCfg.leisureKeywords) {
                                leisureFinal = leisureKws
                                kwChanged = true
                            }
                            if (distractionKws.isNotEmpty() && distractionKws != currentCfg.distractionKeywords) {
                                distractionFinal = distractionKws
                                kwChanged = true
                            }
                            if (kwChanged) {
                                updatedCfg = updatedCfg.copy(
                                    targetKeywords = targetFinal,
                                    leisureKeywords = leisureFinal,
                                    distractionKeywords = distractionFinal
                                )
                                changed = true
                            }
                        }

                        if (changed) {
                            Log.d(TAG, "🟢 Nhận được cập nhật config mới từ Cloud, áp dụng vào App...")
                            statsRepository.updateConfig(updatedCfg, fromRemote = true)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _isConnected.value = false
                    Log.w(TAG, "Firebase sync onCancelled: ${error.message}")
                }
            }

            syncListener = listener
            ref.addValueEventListener(listener)
            Log.d(TAG, "🟢 Đã kết nối Firebase Realtime Sync tại users/$currentSyncCode")
        } catch (e: Exception) {
            _isConnected.value = false
            Log.e(TAG, "Lỗi khi khởi tạo Firebase sync listener", e)
        }
    }

    fun stopSync() {
        try {
            syncListener?.let { syncRef?.removeEventListener(it) }
            syncListener = null
            syncRef = null
            _isConnected.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi stopSync", e)
        }
    }

    /**
     * Đẩy số liệu Mobile lên Firebase với Debounce 2 giây
     */
    fun schedulePushMobileStats(stats: DailyStats) {
        pendingMobileSyncRunnable?.let { mainHandler.removeCallbacks(it) }

        val runnable = Runnable {
            pushMobileStatsImmediately(stats)
        }
        pendingMobileSyncRunnable = runnable
        mainHandler.postDelayed(runnable, 2000L)
    }

    fun pushMobileStatsImmediately(stats: DailyStats) {
        if (currentSyncCode.isBlank()) return
        try {
            val today = stats.date
            val mobileRef = db.getReference("users/$currentSyncCode/history/$today/mobile")

            val payload = mapOf(
                "lastUpdated" to System.currentTimeMillis(),
                "swipes" to stats.mobileSwipes,
                "validViews" to stats.validViews,
                "activeSeconds" to stats.activeSeconds,
                "youtube" to mapOf(
                    "activeSeconds" to stats.youtube.summary.activeSeconds,
                    "passiveSeconds" to stats.youtube.summary.passiveSeconds,
                    "reloadCount" to stats.youtube.summary.reloadCount,
                    "shorts" to mapOf(
                        "swipes" to stats.youtube.shorts.totalSwipes,
                        "validViews" to stats.youtube.shorts.validViews,
                        "impulsiveCount" to stats.youtube.shorts.impulsiveCount,
                        "loopViews" to stats.youtube.shorts.loopViews
                    ),
                    "longVideos" to mapOf(
                        "watched" to stats.youtube.longVideos.totalWatched,
                        "usefulCount" to stats.youtube.longVideos.usefulCount,
                        "impulsiveCount" to stats.youtube.longVideos.impulsiveCount
                    ),
                    "musicVideos" to mapOf(
                        "watched" to stats.youtube.musicVideos.totalWatched,
                        "durationSeconds" to stats.youtube.musicVideos.durationSeconds
                    )
                ),
                "facebook" to mapOf(
                    "activeSeconds" to stats.facebook.summary.activeSeconds,
                    "passiveSeconds" to stats.facebook.summary.passiveSeconds,
                    "reloadCount" to stats.facebook.summary.reloadCount,
                    "feed" to mapOf(
                        "postsScrolled" to stats.facebook.feed.feedPostsScrolled,
                        "postsRead" to stats.facebook.feed.feedPostsRead
                    ),
                    "reels" to mapOf(
                        "swipes" to stats.facebook.reels.totalSwipes,
                        "validViews" to stats.facebook.reels.validViews,
                        "impulsiveCount" to stats.facebook.reels.impulsiveCount,
                        "loopViews" to stats.facebook.reels.loopViews
                    ),
                    "longVideos" to mapOf(
                        "watched" to stats.facebook.longVideos.totalWatched,
                        "usefulCount" to stats.facebook.longVideos.usefulCount,
                        "impulsiveCount" to stats.facebook.longVideos.impulsiveCount
                    )
                ),
                "tiktok" to mapOf(
                    "activeSeconds" to stats.tiktok.summary.activeSeconds,
                    "passiveSeconds" to stats.tiktok.summary.passiveSeconds,
                    "reloadCount" to stats.tiktok.summary.reloadCount,
                    "shorts" to mapOf(
                        "swipes" to stats.tiktok.shorts.totalSwipes,
                        "validViews" to stats.tiktok.shorts.validViews,
                        "impulsiveCount" to stats.tiktok.shorts.impulsiveCount,
                        "loopViews" to stats.tiktok.shorts.loopViews
                    )
                )
            )
            mobileRef.setValue(payload)

            // Đẩy kèm config an toàn (KHÔNG API KEY)
            pushConfigImmediately(statsRepository.appConfig.value)
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi đẩy mobile stats lên cloud: ${e.message}")
        }
    }

    /**
     * Đẩy cấu hình lên Firebase ngay lập tức khi người dùng lưu cài đặt trong App
     */
    fun pushConfigImmediately(appConfig: AppConfig = statsRepository.appConfig.value) {
        if (currentSyncCode.isBlank()) return
        try {
            val configRef = db.getReference("users/$currentSyncCode/config")
            val safeConfig = mapOf(
                "syncCode" to appConfig.syncCode,
                "masterGoal" to appConfig.masterGoal,
                "leisureQuotaMinutes" to appConfig.leisureQuotaMinutes,
                "thresholds" to mapOf(
                    "youtube" to mapOf(
                        "shorts" to mapOf("m1" to appConfig.thresholds.youtube.shorts.m1, "m2" to appConfig.thresholds.youtube.shorts.m2, "m3" to appConfig.thresholds.youtube.shorts.m3),
                        "long" to mapOf("m1" to appConfig.thresholds.youtube.long.m1, "m2" to appConfig.thresholds.youtube.long.m2, "m3" to appConfig.thresholds.youtube.long.m3)
                    ),
                    "facebook" to mapOf(
                        "reels" to mapOf("m1" to appConfig.thresholds.facebook.reels.m1, "m2" to appConfig.thresholds.facebook.reels.m2, "m3" to appConfig.thresholds.facebook.reels.m3),
                        "feeds" to mapOf("m1" to appConfig.thresholds.facebook.feeds.m1, "m2" to appConfig.thresholds.facebook.feeds.m2, "m3" to appConfig.thresholds.facebook.feeds.m3),
                        "long" to mapOf("m1" to appConfig.thresholds.facebook.long.m1, "m2" to appConfig.thresholds.facebook.long.m2, "m3" to appConfig.thresholds.facebook.long.m3)
                    ),
                    "tiktok" to mapOf(
                        "shorts" to mapOf("m1" to appConfig.thresholds.tiktok.shorts.m1, "m2" to appConfig.thresholds.tiktok.shorts.m2, "m3" to appConfig.thresholds.tiktok.shorts.m3)
                    )
                ),
                "pomodoro" to mapOf(
                    "enabled" to appConfig.pomodoro.enabled,
                    "focusMinutes" to appConfig.pomodoro.focusMinutes,
                    "breakMinutes" to appConfig.pomodoro.breakMinutes
                ),
                "bedtime" to mapOf(
                    "enabled" to appConfig.bedtime.enabled,
                    "start" to appConfig.bedtime.start,
                    "end" to appConfig.bedtime.end
                ),
                "reflection" to mapOf(
                    "reminderTime" to appConfig.reflection.reminderTime
                ),
                "keywords" to mapOf(
                    "target" to appConfig.targetKeywords,
                    "leisure" to appConfig.leisureKeywords,
                    "distraction" to appConfig.distractionKeywords
                ),
                "lastUpdated" to System.currentTimeMillis()
            )
            configRef.setValue(safeConfig)
            Log.d(TAG, "🟢 Đã đẩy cấu hình cập nhật tức thì lên cloud: users/$currentSyncCode/config")
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi đẩy config tức thì lên cloud: ${e.message}")
        }
    }

    /**
     * Khi App bị pause hoặc đưa xuống nền (onStop / onPause)
     */
    fun onAppExitOrBackground(petState: PetState) {
        if (currentSyncCode.isBlank()) return
        try {
            val now = System.currentTimeMillis()
            val sharedRef = db.getReference("users/$currentSyncCode/sharedState")
            val payload = mapOf(
                "streak" to petState.streakDays,
                "petEnergy" to petState.energy,
                "petMood" to petState.mood,
                "lastActiveDevice" to "mobile",
                "lastExitTimestamp" to now
            )
            sharedRef.updateChildren(payload)

            val petRef = db.getReference("users/$currentSyncCode/petState")
            val petPayload = mapOf(
                "energy" to petState.energy,
                "mood" to petState.mood,
                "currentStreak" to petState.currentStreak,
                "streakDays" to petState.streakDays,
                "lastPokeEnergyTime" to petState.lastPokeEnergyTime,
                "mode" to petState.mode,
                "accessories" to mapOf(
                    "unlockedItems" to petState.accessories.unlockedItems,
                    "equippedHead" to petState.accessories.equippedHead
                )
            )
            petRef.updateChildren(petPayload)

            val handoffRef = db.getReference("users/$currentSyncCode/deviceHandoff")
            val currentActivity = statsRepository.currentActivity.value
            val todayStats = statsRepository.todayStats.value
            val handoffPayload = mapOf(
                "lastActiveDevice" to "mobile",
                "lastExitTimestamp" to now,
                "exitContext" to mapOf(
                    "platform" to currentActivity.platform,
                    "contentType" to currentActivity.activityType,
                    "totalSwipesToday" to todayStats.totalSwipes,
                    "totalLongToday" to todayStats.totalLongVideosWatched,
                    "lastVideoTitle" to (statsRepository.currentVideoTitle.value ?: ""),
                    "isUnfinished" to false
                )
            )
            handoffRef.updateChildren(handoffPayload)

            Log.d(TAG, "Đã cập nhật exit timestamp và state của mobile lên cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi cập nhật exit timestamp: ${e.message}")
        }
    }
}
