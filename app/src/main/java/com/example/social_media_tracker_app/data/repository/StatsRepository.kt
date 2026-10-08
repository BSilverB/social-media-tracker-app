package com.example.social_media_tracker_app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.social_media_tracker_app.data.model.ActivityState
import com.example.social_media_tracker_app.data.model.AppConfig
import com.example.social_media_tracker_app.data.model.DailyStats
import com.example.social_media_tracker_app.data.model.FacebookStats
import com.example.social_media_tracker_app.data.model.FeedStats
import com.example.social_media_tracker_app.data.model.HourlyStatsItem
import com.example.social_media_tracker_app.data.model.LongVideoStats
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.data.model.ReflectionData
import com.example.social_media_tracker_app.data.model.ShortVideoStats
import com.example.social_media_tracker_app.data.model.SummaryStats
import com.example.social_media_tracker_app.data.model.TemptationStats
import com.example.social_media_tracker_app.data.model.TiktokStats
import com.example.social_media_tracker_app.data.model.WatchedVideoItem
import com.example.social_media_tracker_app.data.model.YoutubeStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class StatsRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Today's Stats
    private val _todayStats = MutableStateFlow(loadStats(DailyStats.getTodayDateString()))
    val todayStats: StateFlow<DailyStats> = _todayStats.asStateFlow()

    // App Configuration
    private val _appConfig = MutableStateFlow(loadConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    // Pet Companion State
    private val _petState = MutableStateFlow(loadPetState())
    val petState: StateFlow<PetState> = _petState.asStateFlow()

    // Real-time Current Activity State (Platform + Mode: shorts, reels, tiktok, long_video, feed, browse)
    private val _currentActivity = MutableStateFlow(ActivityState())
    val currentActivity: StateFlow<ActivityState> = _currentActivity.asStateFlow()

    // QA / Test Simulation Mode
    private val _isQaMode = MutableStateFlow(prefs.getBoolean(KEY_QA_MODE, false))
    val isQaMode: StateFlow<Boolean> = _isQaMode.asStateFlow()

    // Web Scroll Direction (true = scrolling up/visible, false = scrolling down/hidden)
    private val _isScrollingUp = MutableStateFlow(true)
    val isScrollingUp: StateFlow<Boolean> = _isScrollingUp.asStateFlow()

    // Current Session Intent declared by user when entering browser
    private val _sessionIntent = MutableStateFlow("")
    val sessionIntent: StateFlow<String> = _sessionIntent.asStateFlow()

    var onStatsChangedListener: ((DailyStats) -> Unit)? = null
    var onConfigChangedListener: ((AppConfig) -> Unit)? = null

    @Synchronized
    fun updateDesktopSwipes(desktopSwipes: Int) {
        val current = _todayStats.value
        if (current.desktopSwipes != desktopSwipes) {
            val updated = current.copy(desktopSwipes = desktopSwipes)
            _todayStats.value = updated
            try {
                prefs.edit().putString(KEY_PREFIX + updated.date, updated.toJson().toString()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving desktop swipes to prefs", e)
            }
        }
    }

    fun setSessionIntent(intent: String) {
        _sessionIntent.value = intent.trim()
    }

    // Real-time classification of currently playing video
    private val _currentVideoCategory = MutableStateFlow<String?>("goal")
    val currentVideoCategory: StateFlow<String?> = _currentVideoCategory.asStateFlow()

    private val _currentVideoTitle = MutableStateFlow<String?>("")
    val currentVideoTitle: StateFlow<String?> = _currentVideoTitle.asStateFlow()

    // Gentle warning notification for consecutive distractions
    private val _distractionWarning = MutableStateFlow<String?>(null)
    val distractionWarning: StateFlow<String?> = _distractionWarning.asStateFlow()

    private var consecutiveDistractionCount: Int = 0
    private val recentReloadTimestamps = mutableListOf<Long>()
    private val videoClassificationCache = mutableMapOf<String, String>()

    fun clearDistractionWarning() {
        _distractionWarning.value = null
    }

    /**
     * Digital Detox: Tự động hồi phục +5⚡ mỗi 30 phút rời xa MXH/App
     */
    @Synchronized
    fun checkIdleRecovery(): Pair<Int, String?> {
        val now = System.currentTimeMillis()
        val current = _petState.value
        val lastActive = current.lastActiveTimestamp

        if (lastActive <= 0L) {
            val updated = current.copy(lastActiveTimestamp = now)
            savePetState(updated)
            return Pair(0, null)
        }

        val elapsedMinutes = (now - lastActive) / (60 * 1000L)
        if (elapsedMinutes >= 30) {
            val steps = (elapsedMinutes / 30).toInt()
            val bonus = steps * 5
            rewardPet(bonus, "idle_recovery")
            val updated = _petState.value.copy(lastActiveTimestamp = now)
            savePetState(updated)

            val hrs = elapsedMinutes / 60
            val mins = elapsedMinutes % 60
            val timeDesc = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
            val msg = "🌱 Chào mừng bạn quay lại! Bạn đã rời xa MXH $timeDesc (+${bonus}⚡ hồi phục linh vật)."
            return Pair(bonus, msg)
        } else {
            val updated = current.copy(lastActiveTimestamp = now)
            savePetState(updated)
            return Pair(0, null)
        }
    }

    fun updateActivity(platform: String, activityType: String, detail: String = "") {
        _currentActivity.value = ActivityState(
            platform = platform.lowercase(),
            activityType = activityType.lowercase(),
            detail = detail
        )
    }

    fun setScrollingUp(isUp: Boolean) {
        if (_isScrollingUp.value != isUp) {
            _isScrollingUp.value = isUp
        }
    }

    companion object {
        private const val TAG = "StatsRepository"
        private const val PREFS_NAME = "mindful_hub_daily_stats"
        private const val KEY_PREFIX = "stats_"
        private const val KEY_CONFIG = "app_config"
        private const val KEY_PET_STATE = "pet_state"
        private const val KEY_QA_MODE = "qa_test_mode_enabled"

        @Volatile
        private var INSTANCE: StatsRepository? = null

        fun getInstance(context: Context): StatsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StatsRepository(context).also { INSTANCE = it }
            }
        }
    }

    fun checkDateRollOver() {
        val today = DailyStats.getTodayDateString()
        if (_todayStats.value.date != today) {
            val oldStats = _todayStats.value
            // Compress previous day: purge raw video log to save storage and protect privacy
            if (oldStats.watchedVideos.isNotEmpty()) {
                saveStats(oldStats.copy(watchedVideos = emptyList()))
            }
            _todayStats.value = loadStats(today)
            updateStreakOnNewDay()
        }
    }

    private fun updateStreakOnNewDay() {
        val current = _petState.value
        val newStreak = current.streakDays + 1
        val updated = current.copy(
            currentStreak = newStreak,
            streakDays = newStreak
        )
        savePetState(updated)
        checkStreakUnlocks()
    }

    // ─── Per-Platform Tracking Events ───────────────────────────────────────

    private fun updateHourlyBucket(
        currentHourly: List<HourlyStatsItem>,
        swipesDelta: Int = 0,
        longVideosDelta: Int = 0,
        feedScrolledDelta: Int = 0,
        reloadsDelta: Int = 0,
        activeSecDelta: Long = 0L
    ): List<HourlyStatsItem> {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val hourlyMap = currentHourly.associateBy { it.hour }.toMutableMap()
        val existing = hourlyMap[currentHour] ?: HourlyStatsItem(hour = currentHour)
        hourlyMap[currentHour] = existing.copy(
            swipes = existing.swipes + swipesDelta,
            longVideos = existing.longVideos + longVideosDelta,
            feedScrolled = existing.feedScrolled + feedScrolledDelta,
            reloads = existing.reloads + reloadsDelta,
            activeSeconds = existing.activeSeconds + activeSecDelta
        )
        return (0..23).map { h -> hourlyMap[h] ?: HourlyStatsItem(hour = h) }
    }

    @Synchronized
    fun recordSwipe(platform: String = "youtube", shortId: String = "", reportedSwipes: Int? = null) {
        checkDateRollOver()
        val current = _todayStats.value
        val p = platform.lowercase()
        val updatedHourly = updateHourlyBucket(current.hourly, swipesDelta = 1)

        val updated = when (p) {
            "tiktok" -> {
                val ttShorts = current.tiktok.shorts
                val newSwipes = reportedSwipes ?: (ttShorts.totalSwipes + 1)
                // Any swipe before reaching valid count adds to impulsiveCount
                val impulsive = if (newSwipes > ttShorts.validViews) newSwipes - ttShorts.validViews else ttShorts.impulsiveCount
                current.copy(
                    tiktok = current.tiktok.copy(
                        shorts = ttShorts.copy(totalSwipes = newSwipes, impulsiveCount = impulsive)
                    ),
                    currentShortId = shortId,
                    hourly = updatedHourly
                )
            }
            "facebook" -> {
                val fbReels = current.facebook.reels
                val newSwipes = reportedSwipes ?: (fbReels.totalSwipes + 1)
                val impulsive = if (newSwipes > fbReels.validViews) newSwipes - fbReels.validViews else fbReels.impulsiveCount
                current.copy(
                    facebook = current.facebook.copy(
                        reels = fbReels.copy(totalSwipes = newSwipes, impulsiveCount = impulsive)
                    ),
                    currentShortId = shortId,
                    hourly = updatedHourly
                )
            }
            else -> { // youtube
                val ytShorts = current.youtube.shorts
                val newSwipes = reportedSwipes ?: (ytShorts.totalSwipes + 1)
                val impulsive = if (newSwipes > ytShorts.validViews) newSwipes - ytShorts.validViews else ytShorts.impulsiveCount
                current.copy(
                    youtube = current.youtube.copy(
                        shorts = ytShorts.copy(totalSwipes = newSwipes, impulsiveCount = impulsive)
                    ),
                    currentShortId = shortId,
                    hourly = updatedHourly
                )
            }
        }
        saveStats(updated)

        // Penalty check if exceeding M3
        if (updated.totalSwipes >= _appConfig.value.thresholds.m3) {
            penalizePet(15, "milestone_3")
        }
    }

    @Synchronized
    fun recordValidView(platform: String = "youtube", shortId: String = "", reportedValidViews: Int? = null) {
        checkDateRollOver()
        val current = _todayStats.value
        val p = platform.lowercase()

        val updated = when (p) {
            "tiktok" -> {
                val ttShorts = current.tiktok.shorts
                val newValid = reportedValidViews ?: (ttShorts.validViews + 1)
                val impulsive = maxOf(0, ttShorts.totalSwipes - newValid)
                current.copy(
                    tiktok = current.tiktok.copy(
                        shorts = ttShorts.copy(validViews = newValid, impulsiveCount = impulsive)
                    ),
                    currentShortId = shortId
                )
            }
            "facebook" -> {
                val fbReels = current.facebook.reels
                val newValid = reportedValidViews ?: (fbReels.validViews + 1)
                val impulsive = maxOf(0, fbReels.totalSwipes - newValid)
                current.copy(
                    facebook = current.facebook.copy(
                        reels = fbReels.copy(validViews = newValid, impulsiveCount = impulsive)
                    ),
                    currentShortId = shortId
                )
            }
            else -> { // youtube
                val ytShorts = current.youtube.shorts
                val newValid = reportedValidViews ?: (ytShorts.validViews + 1)
                val impulsive = maxOf(0, ytShorts.totalSwipes - newValid)
                current.copy(
                    youtube = current.youtube.copy(
                        shorts = ytShorts.copy(validViews = newValid, impulsiveCount = impulsive)
                    ),
                    currentShortId = shortId
                )
            }
        }
        saveStats(updated)
    }

    @Synchronized
    fun recordLoopView(platform: String = "youtube", shortId: String = "", reportedLoopViews: Int? = null) {
        checkDateRollOver()
        val current = _todayStats.value
        val p = platform.lowercase()

        val updated = when (p) {
            "tiktok" -> {
                val ttShorts = current.tiktok.shorts
                val newLoops = reportedLoopViews ?: (ttShorts.loopViews + 1)
                current.copy(tiktok = current.tiktok.copy(shorts = ttShorts.copy(loopViews = newLoops)))
            }
            "facebook" -> {
                val fbReels = current.facebook.reels
                val newLoops = reportedLoopViews ?: (fbReels.loopViews + 1)
                current.copy(facebook = current.facebook.copy(reels = fbReels.copy(loopViews = newLoops)))
            }
            else -> {
                val ytShorts = current.youtube.shorts
                val newLoops = reportedLoopViews ?: (ytShorts.loopViews + 1)
                current.copy(youtube = current.youtube.copy(shorts = ytShorts.copy(loopViews = newLoops)))
            }
        }
        saveStats(updated)
    }

    @Synchronized
    fun recordReload(platform: String = "youtube") {
        checkDateRollOver()
        val current = _todayStats.value
        val p = platform.lowercase()
        val updatedHourly = updateHourlyBucket(current.hourly, reloadsDelta = 1)

        // Bắt F5 / Reload spam (>= 3 lần trong 2 phút)
        val now = System.currentTimeMillis()
        recentReloadTimestamps.add(now)
        recentReloadTimestamps.removeAll { now - it > 120000L }
        if (recentReloadTimestamps.size >= 3) {
            _distractionWarning.value = "🧘‍♂️ Bảng tin chưa có gì mới đâu, hãy hít thở sâu nào! (-5⚡)"
            penalizePet(5, "reload_spam")
        }

        val updated = when (p) {
            "tiktok" -> current.copy(
                tiktok = current.tiktok.copy(
                    summary = current.tiktok.summary.copy(reloadCount = current.tiktok.summary.reloadCount + 1)
                ),
                hourly = updatedHourly
            )
            "facebook" -> current.copy(
                facebook = current.facebook.copy(
                    summary = current.facebook.summary.copy(reloadCount = current.facebook.summary.reloadCount + 1)
                ),
                hourly = updatedHourly
            )
            else -> current.copy(
                youtube = current.youtube.copy(
                    summary = current.youtube.summary.copy(reloadCount = current.youtube.summary.reloadCount + 1)
                ),
                hourly = updatedHourly
            )
        }
        saveStats(updated)
    }

    @Synchronized
    fun recordTemptation(isResisted: Boolean) {
        checkDateRollOver()
        val current = _todayStats.value
        val t = current.temptation
        val newTemptation = if (isResisted) {
            rewardPet(10, "temptation_resisted")
            t.copy(resistedCount = t.resistedCount + 1)
        } else {
            rewardPet(5, "box_breathing_completed")
            t.copy(succumbedCount = t.succumbedCount + 1)
        }
        saveStats(current.copy(temptation = newTemptation))
    }

    @Synchronized
    fun recordLongVideo(platform: String = "youtube", isUseful: Boolean, isImpulsive: Boolean) {
        checkDateRollOver()
        val current = _todayStats.value
        val p = platform.lowercase()

        if (isUseful) {
            rewardPet(10, "useful_video")
        } else if (isImpulsive) {
            penalizePet(20, "impulsive_video")
        }

        val updated = when (p) {
            "facebook" -> {
                val lv = current.facebook.longVideos
                current.copy(
                    facebook = current.facebook.copy(
                        longVideos = lv.copy(
                            totalWatched = lv.totalWatched + 1,
                            usefulCount = if (isUseful) lv.usefulCount + 1 else lv.usefulCount,
                            impulsiveCount = if (isImpulsive) lv.impulsiveCount + 1 else lv.impulsiveCount
                        )
                    )
                )
            }
            else -> { // youtube
                val lv = current.youtube.longVideos
                current.copy(
                    youtube = current.youtube.copy(
                        longVideos = lv.copy(
                            totalWatched = lv.totalWatched + 1,
                            usefulCount = if (isUseful) lv.usefulCount + 1 else lv.usefulCount,
                            impulsiveCount = if (isImpulsive) lv.impulsiveCount + 1 else lv.impulsiveCount
                        )
                    )
                )
            }
        }
        saveStats(updated)
    }

    @Synchronized
    fun recordMusicVideoWatched(platform: String, videoId: String, title: String, durationSec: Int) {
        checkDateRollOver()
        val current = _todayStats.value
        if (durationSec >= 5) {
            val mv = current.youtube.musicVideos
            val updated = current.copy(
                youtube = current.youtube.copy(
                    musicVideos = mv.copy(
                        totalWatched = mv.totalWatched + 1,
                        durationSeconds = mv.durationSeconds + durationSec
                    )
                )
            )
            saveStats(updated)
        }
    }

    @Synchronized
    fun recordStudyCheckIn(videoId: String, title: String, userIntent: String, isGoal: Boolean) {
        checkDateRollOver()
        val current = _todayStats.value
        val newLog = com.example.social_media_tracker_app.data.model.StudyVideoLogItem(
            videoId = videoId,
            title = title,
            userIntent = userIntent,
            isGoal = isGoal,
            timestamp = System.currentTimeMillis()
        )
        val updatedLogs = current.studyVideoLogs + newLog
        val updated = current.copy(studyVideoLogs = updatedLogs)
        saveStats(updated)

        if (isGoal) {
            rewardPet(5, "study_verified_goal")
        } else {
            penalizePet(10, "study_distraction")
        }
    }

    @Synchronized
    fun recordFeedScroll(isRead: Boolean) {
        checkDateRollOver()
        val current = _todayStats.value
        val feed = current.facebook.feed
        val updated = current.copy(
            facebook = current.facebook.copy(
                feed = feed.copy(
                    feedPostsScrolled = feed.feedPostsScrolled + 1,
                    feedPostsRead = if (isRead) feed.feedPostsRead + 1 else feed.feedPostsRead
                )
            )
        )
        saveStats(updated)
    }

    @Synchronized
    fun updatePlatformStats(platform: String, jsonPayload: String) {
        try {
            checkDateRollOver()
            val json = JSONObject(jsonPayload)
            val current = _todayStats.value
            val p = platform.lowercase()

            val activeSec = json.optLong("activeSeconds", 0L)
            val passiveSec = json.optLong("passiveSeconds", 0L)
            val reloads = json.optInt("reloadCount", 0)
            val swipes = json.optInt("totalSwipes", 0)
            val valids = json.optInt("validViews", 0)
            val loops = json.optInt("loopViews", 0)
            val impulsive = json.optInt("impulsiveCount", maxOf(0, swipes - valids))
            val shortId = json.optString("currentShortId", "")

            val updated = when (p) {
                "tiktok" -> {
                    val s = current.tiktok.summary
                    val sh = current.tiktok.shorts
                    current.copy(
                        tiktok = TiktokStats(
                            summary = s.copy(
                                activeSeconds = maxOf(s.activeSeconds, activeSec),
                                passiveSeconds = maxOf(s.passiveSeconds, passiveSec),
                                reloadCount = maxOf(s.reloadCount, reloads)
                            ),
                            shorts = sh.copy(
                                totalSwipes = maxOf(sh.totalSwipes, swipes),
                                validViews = maxOf(sh.validViews, valids),
                                loopViews = maxOf(sh.loopViews, loops),
                                impulsiveCount = maxOf(sh.impulsiveCount, impulsive)
                            )
                        ),
                        currentShortId = shortId
                    )
                }
                "facebook" -> {
                    val s = current.facebook.summary
                    val r = current.facebook.reels
                    current.copy(
                        facebook = current.facebook.copy(
                            summary = s.copy(
                                activeSeconds = maxOf(s.activeSeconds, activeSec),
                                passiveSeconds = maxOf(s.passiveSeconds, passiveSec),
                                reloadCount = maxOf(s.reloadCount, reloads)
                            ),
                            reels = r.copy(
                                totalSwipes = maxOf(r.totalSwipes, swipes),
                                validViews = maxOf(r.validViews, valids),
                                loopViews = maxOf(r.loopViews, loops),
                                impulsiveCount = maxOf(r.impulsiveCount, impulsive)
                            )
                        ),
                        currentShortId = shortId
                    )
                }
                else -> { // youtube
                    val s = current.youtube.summary
                    val sh = current.youtube.shorts
                    current.copy(
                        youtube = current.youtube.copy(
                            summary = s.copy(
                                activeSeconds = maxOf(s.activeSeconds, activeSec),
                                passiveSeconds = maxOf(s.passiveSeconds, passiveSec),
                                reloadCount = maxOf(s.reloadCount, reloads)
                            ),
                            shorts = sh.copy(
                                totalSwipes = maxOf(sh.totalSwipes, swipes),
                                validViews = maxOf(sh.validViews, valids),
                                loopViews = maxOf(sh.loopViews, loops),
                                impulsiveCount = maxOf(sh.impulsiveCount, impulsive)
                            )
                        ),
                        currentShortId = shortId
                    )
                }
            }
            saveStats(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse platform stats json: $jsonPayload", e)
        }
    }

    @Synchronized
    fun updateFromJson(jsonPayload: String) {
        try {
            val json = JSONObject(jsonPayload)
            val p = json.optString("platform", "youtube")
            updatePlatformStats(p, jsonPayload)
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateFromJson", e)
        }
    }

    @Synchronized
    fun saveReflection(lesson1: String, lesson2: String, rating: Int) {
        val current = _todayStats.value
        val ref = ReflectionData(
            lesson1 = lesson1,
            lesson2 = lesson2,
            rating = rating,
            submittedAt = System.currentTimeMillis()
        )
        // Auto-cleanup: After reflecting on today's content, summary stats are sealed.
        // Purge raw watched videos list to save storage and protect privacy.
        val updated = current.copy(reflection = ref, watchedVideos = emptyList())
        saveStats(updated)
        rewardPet(15, "daily_reflection")
    }

    // ─── Video Categorization & Alignment Engine ───────────────────────────

    fun classifyVideo(title: String, channel: String, platform: String): String {
        val config = _appConfig.value
        val normTitle = title.lowercase().trim()
        val normChannel = channel.lowercase().trim()
        val combinedText = "$normTitle $normChannel"

        // If title is generic or blank, mark as unclassified
        val isGenericTitle = normTitle.isBlank() ||
                normTitle in listOf("youtube", "tiktok", "facebook", "shorts", "reels", "video", "trang chủ", "bảng tin") ||
                normTitle.startsWith("video ")
        if (isGenericTitle) {
            return "unclassified"
        }

        // 1. Check Distraction / Dopamine Trap Triggers first
        val distractionKeywords = config.distractionKeywords.ifEmpty { com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_DISTRACTION_KEYWORDS }
        for (kw in distractionKeywords) {
            val cleanKw = kw.lowercase().trim()
            if (cleanKw.isNotBlank() && combinedText.contains(cleanKw)) {
                return "distraction"
            }
        }

        // 2. Check Goal Alignment (Master Goal keywords, Target Keywords, Session Intent)
        val goalKeywords = mutableListOf<String>()
        goalKeywords.addAll(config.targetKeywords.map { it.lowercase().trim() }.filter { it.isNotBlank() })

        val masterGoalTokens = config.masterGoal.lowercase()
            .split(" ", ",", ".", ";", "-")
            .map { it.trim() }
            .filter { it.length >= 3 && it !in listOf("muốn", "trở", "thành", "phiên", "bản", "hơn", "trong", "ngày") }
        goalKeywords.addAll(masterGoalTokens)

        val intentTokens = _sessionIntent.value.lowercase()
            .split(" ", ",", ".", ";", "-")
            .map { it.trim() }
            .filter { it.length >= 3 && it !in listOf("xem", "lướt", "nhanh", "chút", "thư", "giãn") }
        goalKeywords.addAll(intentTokens)

        val matchesGoal = goalKeywords.any { kw ->
            combinedText.contains(kw)
        }
        if (matchesGoal) {
            return "goal"
        }

        // 3. Check Explicit Leisure / Entertainment Signals
        val leisureKeywords = config.leisureKeywords.ifEmpty { com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_LEISURE_KEYWORDS }
        val isLeisureIntent = _sessionIntent.value.lowercase().let {
            it.contains("thư giãn") || it.contains("giải trí") || it.contains("nghe nhạc") || it.contains("nghỉ ngơi")
        }
        val matchesLeisure = isLeisureIntent || leisureKeywords.any { kw ->
            val cleanKw = kw.lowercase().trim()
            cleanKw.isNotBlank() && combinedText.contains(cleanKw)
        }

        if (matchesLeisure) {
            val dailyStats = _todayStats.value
            val usedLeisureMinutes = dailyStats.totalLeisureWatchedSeconds / 60
            val quotaMinutes = config.leisureQuotaMinutes

            return if (usedLeisureMinutes >= quotaMinutes) {
                "distraction" // Exceeded daily leisure quota
            } else {
                "leisure" // Within healthy leisure quota
            }
        }

        // 4. If neither clear goal nor clear leisure nor distraction -> Unclassified for user review
        return "unclassified"
    }

    @Synchronized
    fun recordVideoWatched(
        platform: String,
        videoId: String,
        title: String,
        channel: String,
        duration: Int,
        watchedSeconds: Int
    ) {
        checkDateRollOver()

        val current = _todayStats.value
        val cleanPlatform = platform.lowercase()
        val cleanTitle = title.trim().ifBlank { "Video $videoId" }
        val cleanChannel = channel.trim().ifBlank {
            when (cleanPlatform) {
                "youtube" -> "YouTube"
                "facebook" -> "Facebook"
                "tiktok" -> "TikTok"
                else -> "Mạng xã hội"
            }
        }

        // Ensure robust unique ID so videos don't accidentally merge
        val cleanId = if (videoId.isNotBlank() && videoId != "null" && !videoId.startsWith("fb_") && !videoId.startsWith("tiktok_/")) {
            videoId.trim()
        } else {
            val titlePart = if (cleanTitle.length > 5 && !cleanTitle.startsWith("Video ")) cleanTitle.hashCode().toString() else "${System.currentTimeMillis()}"
            "${cleanPlatform}_$titlePart"
        }

        val existingIndex = current.watchedVideos.indexOfFirst {
            if (cleanId.isNotBlank() && it.id == cleanId && it.platform == cleanPlatform) {
                true
            } else if (cleanTitle.length > 10 && !cleanTitle.startsWith("Video ") && it.title == cleanTitle && it.platform == cleanPlatform) {
                (System.currentTimeMillis() - it.timestamp) < 15 * 60 * 1000L
            } else {
                false
            }
        }

        val updatedCategory: String
        val updatedVideos = current.watchedVideos.toMutableList()

        if (existingIndex >= 0) {
            val existing = updatedVideos[existingIndex]
            updatedCategory = existing.category
            val newWatchedSec = maxOf(existing.watchedSeconds, watchedSeconds)
            updatedVideos[existingIndex] = existing.copy(
                watchedSeconds = newWatchedSec,
                title = if (existing.title.startsWith("Video ") && cleanTitle.length > 5) cleanTitle else existing.title,
                channel = if (existing.channel.isBlank()) cleanChannel else existing.channel
            )
        } else {
            val computedCategory = classifyVideo(cleanTitle, cleanChannel, cleanPlatform)
            updatedCategory = computedCategory

            val newItem = com.example.social_media_tracker_app.data.model.WatchedVideoItem(
                id = cleanId,
                title = cleanTitle,
                channel = cleanChannel,
                platform = cleanPlatform,
                category = computedCategory,
                watchedSeconds = maxOf(3, watchedSeconds),
                timestamp = System.currentTimeMillis(),
                userOverridden = false
            )
            updatedVideos.add(0, newItem) // most recent first

            // Consecutive distraction intervention
            if (computedCategory == "distraction") {
                consecutiveDistractionCount++
                if (consecutiveDistractionCount >= 2) {
                    _distractionWarning.value = "⚠️ Pet nhắc nhẹ: Bạn đang xem liên tiếp 2 nội dung lạc lối. Hãy quay lại mục tiêu nào!"
                    penalizePet(3, "consecutive_distraction")
                }
            } else if (computedCategory == "goal" || computedCategory == "leisure") {
                consecutiveDistractionCount = 0
                _distractionWarning.value = null
            }
        }

        _currentVideoCategory.value = updatedCategory
        _currentVideoTitle.value = cleanTitle

        val updated = current.copy(watchedVideos = updatedVideos)
        saveStats(updated)
    }

    @Synchronized
    fun updateVideoCategory(videoId: String, platform: String, newCategory: String) {
        checkDateRollOver()
        val current = _todayStats.value
        val updatedVideos = current.watchedVideos.map { item ->
            if ((item.id == videoId && videoId.isNotBlank()) || (item.platform == platform && item.id == videoId)) {
                item.copy(category = newCategory, userOverridden = true)
            } else {
                item
            }
        }
        val updated = current.copy(watchedVideos = updatedVideos)
        saveStats(updated)
    }

    @Synchronized
    fun applyAiClassificationAndLearnedKeywords(
        reclassifiedMap: Map<String, String>,
        learnedTargetKeywords: List<String>,
        learnedLeisureKeywords: List<String>,
        learnedDistractionKeywords: List<String>
    ) {
        checkDateRollOver()
        val current = _todayStats.value

        // 1. Update categories of matched videos
        if (reclassifiedMap.isNotEmpty()) {
            val updatedVideos = current.watchedVideos.map { item ->
                val newCat = reclassifiedMap[item.id] ?: reclassifiedMap[item.title]
                if (newCat != null && newCat in listOf("goal", "leisure", "distraction")) {
                    item.copy(category = newCat, userOverridden = true)
                } else {
                    item
                }
            }
            saveStats(current.copy(watchedVideos = updatedVideos))
        }

        // 2. Merge newly learned keywords into AppConfig without case-insensitive duplicates
        val currentConfig = _appConfig.value
        val mergedTarget = currentConfig.targetKeywords.toMutableList()
        learnedTargetKeywords.forEach { kw ->
            val clean = kw.lowercase().trim()
            if (clean.isNotBlank() && mergedTarget.none { it.equals(clean, ignoreCase = true) }) {
                mergedTarget.add(clean)
            }
        }

        val mergedLeisure = currentConfig.leisureKeywords.toMutableList()
        learnedLeisureKeywords.forEach { kw ->
            val clean = kw.lowercase().trim()
            if (clean.isNotBlank() && mergedLeisure.none { it.equals(clean, ignoreCase = true) }) {
                mergedLeisure.add(clean)
            }
        }

        val mergedDistraction = currentConfig.distractionKeywords.toMutableList()
        learnedDistractionKeywords.forEach { kw ->
            val clean = kw.lowercase().trim()
            if (clean.isNotBlank() && mergedDistraction.none { it.equals(clean, ignoreCase = true) }) {
                mergedDistraction.add(clean)
            }
        }

        if (mergedTarget != currentConfig.targetKeywords ||
            mergedLeisure != currentConfig.leisureKeywords ||
            mergedDistraction != currentConfig.distractionKeywords
        ) {
            val updatedConfig = currentConfig.copy(
                targetKeywords = mergedTarget,
                leisureKeywords = mergedLeisure,
                distractionKeywords = mergedDistraction
            )
            updateConfig(updatedConfig)
        }
    }

    fun addKeyword(category: String, keyword: String): Boolean {
        val clean = keyword.lowercase().trim()
        if (clean.isBlank()) return false
        val currentConfig = _appConfig.value
        val updatedConfig = when (category.lowercase()) {
            "goal", "target" -> {
                if (currentConfig.targetKeywords.any { it.equals(clean, ignoreCase = true) }) return false
                currentConfig.copy(targetKeywords = currentConfig.targetKeywords + clean)
            }
            "leisure" -> {
                if (currentConfig.leisureKeywords.any { it.equals(clean, ignoreCase = true) }) return false
                currentConfig.copy(leisureKeywords = currentConfig.leisureKeywords + clean)
            }
            "distraction" -> {
                if (currentConfig.distractionKeywords.any { it.equals(clean, ignoreCase = true) }) return false
                currentConfig.copy(distractionKeywords = currentConfig.distractionKeywords + clean)
            }
            else -> return false
        }
        updateConfig(updatedConfig)
        return true
    }

    fun removeKeyword(category: String, keyword: String) {
        val clean = keyword.lowercase().trim()
        val currentConfig = _appConfig.value
        val updatedConfig = when (category.lowercase()) {
            "goal", "target" -> currentConfig.copy(targetKeywords = currentConfig.targetKeywords.filterNot { it.equals(clean, ignoreCase = true) })
            "leisure" -> currentConfig.copy(leisureKeywords = currentConfig.leisureKeywords.filterNot { it.equals(clean, ignoreCase = true) })
            "distraction" -> currentConfig.copy(distractionKeywords = currentConfig.distractionKeywords.filterNot { it.equals(clean, ignoreCase = true) })
            else -> return
        }
        updateConfig(updatedConfig)
    }

    fun resetKeywords(category: String) {
        val currentConfig = _appConfig.value
        val updatedConfig = when (category.lowercase()) {
            "goal", "target" -> currentConfig.copy(targetKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_TARGET_KEYWORDS)
            "leisure" -> currentConfig.copy(leisureKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_LEISURE_KEYWORDS)
            "distraction" -> currentConfig.copy(distractionKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_DISTRACTION_KEYWORDS)
            else -> currentConfig.copy(
                targetKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_TARGET_KEYWORDS,
                leisureKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_LEISURE_KEYWORDS,
                distractionKeywords = com.example.social_media_tracker_app.data.model.AppConfig.DEFAULT_DISTRACTION_KEYWORDS
            )
        }
        updateConfig(updatedConfig)
    }

    // ─── Pet Engine & Wardrobe Management ───────────────────────────────────

    fun penalizePet(amount: Int, reason: String) {
        val current = _petState.value
        val newEnergy = (current.energy - amount).coerceAtLeast(0)
        val updated = current.copy(
            energy = newEnergy,
            mood = PetState.calculateMood(newEnergy)
        )
        savePetState(updated)
    }

    fun rewardPet(amount: Int, reason: String) {
        val current = _petState.value
        val newEnergy = (current.energy + amount).coerceAtMost(100)
        val updated = current.copy(
            energy = newEnergy,
            mood = PetState.calculateMood(newEnergy)
        )
        savePetState(updated)
    }

    fun pokePet(): Pair<String, Boolean> {
        val current = _petState.value
        val prompts = listOf(
            "💧 Uống một ngụm nước ấm nhé!",
            "🌬️ Hít sâu 3 nhịp và thả lỏng vai nào!",
            "🎯 Giữ vững mục tiêu hôm nay nhé!",
            "👀 Nhìn xa thư giãn mắt một chút nào!",
            "🧘 Ngồi thẳng lưng lên bạn nhé!"
        )
        val message = when {
            current.isWilted -> "Mầm xanh đang bị úa do đứt chuỗi... Hãy hoàn thành 1 phiên Pomodoro hoặc cho tớ 1 hạt mầm để hồi sinh nhé! 🍂"
            current.customQuotes.isNotEmpty() && (current.mood == "sad" || Math.random() < 0.7) -> current.customQuotes.random()
            else -> prompts.random()
        }
        val now = System.currentTimeMillis()
        val cooldownMs = 30 * 60 * 1000L

        return if (now - current.lastPokeEnergyTime >= cooldownMs) {
            val newEnergy = (current.energy + 3).coerceAtMost(100)
            val updated = current.copy(
                energy = newEnergy,
                mood = PetState.calculateMood(newEnergy),
                lastPokeEnergyTime = now
            )
            savePetState(updated)
            Pair(message, true)
        } else {
            Pair(message, false)
        }
    }

    fun updateFullPetState(newState: PetState) {
        savePetState(newState)
        checkStreakUnlocks()
    }

    fun addKnowledgeSeed() {
        val current = _petState.value
        val updated = current.copy(knowledgeSeeds = current.knowledgeSeeds + 1)
        savePetState(updated)
    }

    fun feedPetSeed(): Boolean {
        val current = _petState.value
        if (current.knowledgeSeeds <= 0) return false
        val newEnergy = minOf(100, current.energy + 15)
        val updated = current.copy(
            knowledgeSeeds = current.knowledgeSeeds - 1,
            energy = newEnergy,
            mood = PetState.calculateMood(newEnergy),
            isWilted = false
        )
        savePetState(updated)
        return true
    }

    fun updateCustomQuotes(quotes: List<String>) {
        val current = _petState.value
        val updated = current.copy(customQuotes = quotes)
        savePetState(updated)
    }

    fun revivePetFromWilt() {
        val current = _petState.value
        if (current.isWilted) {
            savePetState(current.copy(isWilted = false))
        }
    }

    fun checkStreakUnlocks() {
        val current = _petState.value
        val unlocked = current.accessories.unlockedItems.toMutableSet()
        var changed = false

        if (current.streakDays >= 3 && !unlocked.contains("sunglasses")) {
            unlocked.add("sunglasses")
            changed = true
        }
        if (current.streakDays >= 7 && !unlocked.contains("laurel")) {
            unlocked.add("laurel")
            changed = true
        }

        if (changed) {
            val updatedAcc = current.accessories.copy(unlockedItems = unlocked.toList())
            savePetState(current.copy(accessories = updatedAcc))
        }
    }

    fun equipAccessory(headItem: String?) {
        val current = _petState.value
        val updatedAcc = current.accessories.copy(equippedHead = headItem)
        savePetState(current.copy(accessories = updatedAcc))
    }

    fun updatePetMode(mode: String, puppetPhotos: com.example.social_media_tracker_app.data.model.PuppetPhotos? = null) {
        val current = _petState.value
        val updated = current.copy(
            mode = mode,
            puppetPhotos = puppetPhotos ?: current.puppetPhotos
        )
        savePetState(updated)
    }

    fun updatePetPuppetPhotos(puppetPhotos: com.example.social_media_tracker_app.data.model.PuppetPhotos) {
        val current = _petState.value
        val updated = current.copy(puppetPhotos = puppetPhotos)
        savePetState(updated)
    }

    fun updatePetAiSprites(aiSprites: com.example.social_media_tracker_app.data.model.AiSprites) {
        val current = _petState.value
        val updated = current.copy(mode = "ai_generated", aiSprites = aiSprites)
        savePetState(updated)
    }

    fun updateUserPersona(dayDigest: com.example.social_media_tracker_app.data.model.PersonaDayHistory) {
        val current = _petState.value
        val persona = current.userPersona
        val newTracked = persona.totalTrackedDays + 1
        val newAvg = if (newTracked > 0) ((persona.avgUsefulPct * (newTracked - 1)) + dayDigest.usefulPct) / newTracked else dayDigest.usefulPct
        val newRecord = maxOf(persona.streakRecord, current.streakDays)
        val newHistory = (listOf(dayDigest) + persona.history).take(30)

        val updatedPersona = persona.copy(
            totalTrackedDays = newTracked,
            streakRecord = newRecord,
            avgUsefulPct = newAvg,
            history = newHistory
        )
        savePetState(current.copy(userPersona = updatedPersona))
    }

    fun evaluateDailyStreak(todayKey: String) {
        val current = _petState.value
        val stats = _todayStats.value

        val totalLong = stats.youtube.longVideos.totalWatched + stats.facebook.longVideos.totalWatched
        val totalShorts = stats.youtube.shorts.totalSwipes + stats.facebook.reels.totalSwipes

        // Kỷ luật: Nếu Pet cạn kiệt 0 năng lượng lúc 22h00 -> Mất toàn bộ Streak
        if (current.energy <= 0) {
            val updated = current.copy(currentStreak = 0, streakDays = 0, mood = "sad")
            savePetState(updated)
            return
        }

        // Tiêu chí có kỷ luật: không vi phạm quá đà (<= 10 video dài và <= 50 shorts/reels)
        val isDisciplined = totalLong <= 10 && totalShorts <= 50
        val updated = if (isDisciplined) {
            val newStreak = current.streakDays + 1
            current.copy(
                streakDays = newStreak,
                currentStreak = newStreak,
                energy = minOf(100, current.energy + 15),
                mood = "happy"
            )
        } else {
            val newStreak = maxOf(0, current.streakDays - 1)
            val newEnergy = maxOf(0, current.energy - 20)
            current.copy(
                streakDays = newStreak,
                currentStreak = newStreak,
                energy = newEnergy,
                mood = PetState.calculateMood(newEnergy)
            )
        }
        savePetState(updated)
        checkStreakUnlocks()
    }

    // ─── QA / Test Mode Simulation Helpers ─────────────────────────────────

    fun setTestEnergy(amount: Int) {
        val current = _petState.value
        val clamped = amount.coerceIn(0, 100)
        val updated = current.copy(
            energy = clamped,
            mood = PetState.calculateMood(clamped)
        )
        savePetState(updated)
    }

    fun setTestStreak(days: Int) {
        val current = _petState.value
        val updated = current.copy(
            currentStreak = days,
            streakDays = days
        )
        savePetState(updated)
        checkStreakUnlocks()
    }

    fun setQaMode(enabled: Boolean) {
        _isQaMode.value = enabled
        prefs.edit().putBoolean(KEY_QA_MODE, enabled).apply()
        if (!enabled) {
            clearMockDataAndReset()
        }
    }

    /**
     * Khôi phục toàn bộ trạng thái app về ban đầu khi tắt QA Mode:
     * - Xóa sạch các bản ghi ngày cũ được tạo bởi chế độ Test (mock data).
     * - Đặt lại số liệu ngày hôm nay về trắng (0 swipes, 0 time).
     * - Khôi phục thú cưng về 100⚡ năng lượng, 0 streak.
     */
    fun clearMockDataAndReset() {
        val today = DailyStats.getTodayDateString()
        val editor = prefs.edit()
        val allKeys = prefs.all.keys
        val dateRegex = Regex("""^stats_(\d{4}-\d{2}-\d{2})$""")

        // Xóa tất cả các bản ghi ngày lịch sử đã sinh ra bởi mock/test
        for (k in allKeys) {
            val match = dateRegex.find(k)
            if (match != null) {
                editor.remove(k)
            }
        }

        // Tạo lại ngày hôm nay hoàn toàn mới
        val cleanStats = DailyStats(date = today)
        editor.putString(KEY_PREFIX + today, cleanStats.toJson().toString())

        // Đặt lại thú cưng sạch 100⚡
        val cleanPet = PetState(
            energy = 100,
            mood = "happy",
            currentStreak = 0,
            streakDays = 0,
            lastActiveTimestamp = System.currentTimeMillis(),
            accessories = com.example.social_media_tracker_app.data.model.PetAccessories()
        )
        editor.putString(KEY_PET_STATE, cleanPet.toJson())
        editor.apply()

        _todayStats.value = cleanStats
        _petState.value = cleanPet
        onStatsChangedListener?.invoke(cleanStats)
    }

    fun resetTestData() {
        clearMockDataAndReset()
    }

    // ─── App Configuration Management ───────────────────────────────────────

    fun updateConfig(config: AppConfig, fromRemote: Boolean = false) {
        _appConfig.value = config
        prefs.edit().putString(KEY_CONFIG, config.toJson()).apply()
        if (!fromRemote) {
            onConfigChangedListener?.invoke(config)
        }
    }

    // ─── Long-term Analytics & Backup / Restore ──────────────────────────────

    fun getAllDailyRecords(): List<DailyStats> {
        val list = mutableListOf<DailyStats>()
        val allKeys = prefs.all.keys
        val dateRegex = Regex("""^stats_(\d{4}-\d{2}-\d{2})$""")

        for (k in allKeys) {
            val match = dateRegex.find(k)
            if (match != null) {
                val date = match.groupValues[1]
                list.add(loadStats(date))
            }
        }
        list.sortBy { it.date }
        return list
    }

    fun exportAllDataAsJson(): String {
        val root = JSONObject()
        val recordsArr = JSONArray()

        getAllDailyRecords().forEach { stats ->
            recordsArr.put(stats.toJson())
        }
        root.put("records", recordsArr)
        root.put("app_config", JSONObject(_appConfig.value.toJson()))
        root.put("pet_state", JSONObject(_petState.value.toJson()))
        root.put("exportedAt", System.currentTimeMillis())

        return root.toString(2)
    }

    fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val editor = prefs.edit()

            if (root.has("records")) {
                val arr = root.getJSONArray("records")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val date = item.getString("date")
                    editor.putString(KEY_PREFIX + date, item.toString())
                }
            }

            if (root.has("app_config")) {
                val confObj = root.getJSONObject("app_config")
                val config = AppConfig.fromJson(confObj.toString())
                editor.putString(KEY_CONFIG, config.toJson())
                _appConfig.value = config
            }

            if (root.has("pet_state")) {
                val petObj = root.getJSONObject("pet_state")
                val pet = PetState.fromJson(petObj.toString())
                editor.putString(KEY_PET_STATE, pet.toJson())
                _petState.value = pet
            }

            editor.apply()
            _todayStats.value = loadStats(DailyStats.getTodayDateString())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            false
        }
    }

    // ─── Persistence Helpers ────────────────────────────────────────────────

    private fun loadStats(date: String): DailyStats {
        val rawJson = prefs.getString(KEY_PREFIX + date, null) ?: return DailyStats(date = date)
        return try {
            DailyStats.Serializer.fromJson(JSONObject(rawJson))
        } catch (e: Exception) {
            Log.e(TAG, "Error loading stats for date $date", e)
            DailyStats(date = date)
        }
    }

    fun generateMockHistory(daysCount: Int = 7) {
        val cal = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val editor = prefs.edit()
        val rand = java.util.Random()

        for (i in daysCount - 1 downTo 0) {
            val dCal = java.util.Calendar.getInstance().apply {
                time = cal.time
                add(java.util.Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = sdf.format(dCal.time)

            // Random realistic metrics
            val baseSwipes = rand.nextInt(35) + 10 // 10..44
            val baseReloads = rand.nextInt(12) + 1 // 1..12
            val activeSec = (rand.nextInt(1500) + 600).toLong() // 10..35 mins
            val passiveSec = (rand.nextInt(800) + 100).toLong()

            // 24-hour distribution: peak around 12h-13h and 21h-23h
            val hourlyList = (0..23).map { h ->
                val isPeak = h in 12..13 || h in 21..23
                val multiplier = if (isPeak) 3 else 1
                val hSwipes = if (rand.nextBoolean()) (rand.nextInt(4) + 1) * multiplier else 0
                val hReloads = if (isPeak && rand.nextBoolean()) rand.nextInt(3) else 0
                val hActive = (hSwipes * 25L).coerceAtLeast(0L)
                HourlyStatsItem(
                    hour = h,
                    swipes = hSwipes,
                    longVideos = if (isPeak && rand.nextBoolean()) 1 else 0,
                    feedScrolled = if (isPeak) rand.nextInt(3) else 0,
                    reloads = hReloads,
                    activeSeconds = hActive
                )
            }

            val mockVideos = listOf(
                WatchedVideoItem(id = "yt_1_$dateStr", title = "Học lập trình Jetpack Compose & Clean Arch", channel = "Android Dev", platform = "youtube", category = "goal", watchedSeconds = 450, durationSeconds = 600, isCompletion = false),
                WatchedVideoItem(id = "yt_2_$dateStr", title = "Luyện nghe Tiếng Anh thụ động IELTS 7.0", channel = "English Hub", platform = "youtube", category = "goal", watchedSeconds = 320, durationSeconds = 360, isCompletion = true),
                WatchedVideoItem(id = "yt_3_$dateStr", title = "Nhạc Lofi Chill thư giãn tập trung làm việc", channel = "Chill Beats", platform = "youtube", category = "leisure", watchedSeconds = 600, durationSeconds = 1200, isCompletion = false),
                WatchedVideoItem(id = "yt_4_$dateStr", title = "Tin hot drama mạng xã hội hôm nay", channel = "Drama Viral", platform = "youtube", category = "distraction", watchedSeconds = 25, durationSeconds = 180, isCompletion = false),
                WatchedVideoItem(id = "yt_5_$dateStr", title = "Video ngắn hài hước chó mèo xả stress", channel = "Pet Funny", platform = "youtube", category = "leisure", watchedSeconds = 40, durationSeconds = 60, isCompletion = true)
            )

            val mockStats = DailyStats(
                date = dateStr,
                youtube = YoutubeStats(
                    summary = SummaryStats(
                        activeSeconds = activeSec,
                        passiveSeconds = passiveSec,
                        reloadCount = baseReloads
                    ),
                    shorts = ShortVideoStats(
                        totalSwipes = baseSwipes,
                        validViews = (baseSwipes * 0.7).toInt(),
                        loopViews = rand.nextInt(5)
                    ),
                    longVideos = LongVideoStats(
                        totalWatched = 3,
                        usefulCount = 2,
                        impulsiveCount = 1
                    )
                ),
                watchedVideos = mockVideos,
                hourly = hourlyList,
                temptation = TemptationStats(
                    resistedCount = rand.nextInt(4) + 1,
                    succumbedCount = rand.nextInt(2)
                ),
                petEnergyEndOfDay = 60 + rand.nextInt(40)
            )

            editor.putString(KEY_PREFIX + dateStr, mockStats.toJson().toString())
            if (i == 0) {
                _todayStats.value = mockStats
            }
        }
        editor.apply()
    }

    private fun saveStats(stats: DailyStats) {
        val statsWithPet = if (stats.date == DailyStats.getTodayDateString()) {
            stats.copy(petEnergyEndOfDay = _petState.value.energy)
        } else {
            stats
        }
        _todayStats.value = statsWithPet
        try {
            prefs.edit().putString(KEY_PREFIX + statsWithPet.date, statsWithPet.toJson().toString()).apply()
            onStatsChangedListener?.invoke(statsWithPet)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stats for date ${statsWithPet.date}", e)
        }
    }

    private fun loadConfig(): AppConfig {
        val raw = prefs.getString(KEY_CONFIG, null) ?: return AppConfig()
        return AppConfig.fromJson(raw)
    }

    private fun loadPetState(): PetState {
        val raw = prefs.getString(KEY_PET_STATE, null) ?: return PetState()
        return PetState.fromJson(raw)
    }

    private fun savePetState(state: PetState) {
        _petState.value = state
        prefs.edit().putString(KEY_PET_STATE, state.toJson()).apply()
    }
}
