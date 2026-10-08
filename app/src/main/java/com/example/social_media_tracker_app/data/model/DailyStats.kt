package com.example.social_media_tracker_app.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSeconds(seconds: Long): String {
    val s = maxOf(0L, seconds)
    val h = s / 3600
    val m = (s % 3600) / 60
    val remSec = s % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${remSec}s"
        else -> "${remSec}s"
    }
}

data class SummaryStats(
    val activeSeconds: Long = 0,
    val passiveSeconds: Long = 0,
    val reloadCount: Int = 0
) {
    val activeTimeSeconds: Long get() = activeSeconds
    val passiveTimeSeconds: Long get() = passiveSeconds
    val formattedActiveTime: String get() = formatSeconds(activeSeconds)
    val formattedPassiveTime: String get() = formatSeconds(passiveSeconds)

    fun toJson(): JSONObject = JSONObject().apply {
        put("activeSeconds", activeSeconds)
        put("passiveSeconds", passiveSeconds)
        put("reloadCount", reloadCount)
        put("activeTimeSeconds", activeSeconds)
        put("passiveTimeSeconds", passiveSeconds)
    }

    companion object {
        fun fromJson(obj: JSONObject?): SummaryStats {
            if (obj == null) return SummaryStats()
            val active = if (obj.has("activeSeconds")) obj.optLong("activeSeconds", 0L) else obj.optLong("activeTimeSeconds", 0L)
            val passive = if (obj.has("passiveSeconds")) obj.optLong("passiveSeconds", 0L) else obj.optLong("passiveTimeSeconds", 0L)
            return SummaryStats(
                activeSeconds = active,
                passiveSeconds = passive,
                reloadCount = obj.optInt("reloadCount", 0)
            )
        }
    }
}

data class ShortVideoStats(
    val totalSwipes: Int = 0,
    val validViews: Int = 0,
    val impulsiveCount: Int = 0,
    val loopViews: Int = 0
) {
    val deepRatioPercentage: Int
        get() = if (totalSwipes > 0) ((validViews.toDouble() / totalSwipes) * 100).toInt().coerceIn(0, 100) else 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("totalSwipes", totalSwipes)
        put("validViews", validViews)
        put("impulsiveCount", impulsiveCount)
        put("loopViews", loopViews)
    }

    companion object {
        fun fromJson(obj: JSONObject?): ShortVideoStats {
            if (obj == null) return ShortVideoStats()
            return ShortVideoStats(
                totalSwipes = obj.optInt("totalSwipes", 0),
                validViews = obj.optInt("validViews", 0),
                impulsiveCount = obj.optInt("impulsiveCount", 0),
                loopViews = obj.optInt("loopViews", 0)
            )
        }
    }
}

data class LongVideoStats(
    val totalWatched: Int = 0,
    val usefulCount: Int = 0,
    val impulsiveCount: Int = 0
) {
    val usefulRatioPercentage: Int
        get() = if (totalWatched > 0) ((usefulCount.toDouble() / totalWatched) * 100).toInt().coerceIn(0, 100) else 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("totalWatched", totalWatched)
        put("usefulCount", usefulCount)
        put("impulsiveCount", impulsiveCount)
    }

    companion object {
        fun fromJson(obj: JSONObject?): LongVideoStats {
            if (obj == null) return LongVideoStats()
            return LongVideoStats(
                totalWatched = obj.optInt("totalWatched", 0),
                usefulCount = obj.optInt("usefulCount", 0),
                impulsiveCount = obj.optInt("impulsiveCount", 0)
            )
        }
    }
}

data class MusicVideoStats(
    val totalWatched: Int = 0,
    val durationSeconds: Long = 0
) {
    val totalDurationSeconds: Long get() = durationSeconds

    fun toJson(): JSONObject = JSONObject().apply {
        put("totalWatched", totalWatched)
        put("durationSeconds", durationSeconds)
        put("totalDurationSeconds", durationSeconds)
    }

    companion object {
        fun fromJson(obj: JSONObject?): MusicVideoStats {
            if (obj == null) return MusicVideoStats()
            val dur = if (obj.has("durationSeconds")) obj.optLong("durationSeconds", 0L) else obj.optLong("totalDurationSeconds", 0L)
            return MusicVideoStats(
                totalWatched = obj.optInt("totalWatched", 0),
                durationSeconds = dur
            )
        }
    }
}

data class StudyVideoLogItem(
    val videoId: String = "",
    val title: String = "",
    val userIntent: String = "",
    val isGoal: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("videoId", videoId)
        put("title", title)
        put("userIntent", userIntent)
        put("isGoal", isGoal)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(obj: JSONObject?): StudyVideoLogItem {
            if (obj == null) return StudyVideoLogItem()
            return StudyVideoLogItem(
                videoId = obj.optString("videoId", ""),
                title = obj.optString("title", ""),
                userIntent = obj.optString("userIntent", ""),
                isGoal = obj.optBoolean("isGoal", true),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

data class FeedStats(
    val feedPostsScrolled: Int = 0,
    val feedPostsRead: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("feedPostsScrolled", feedPostsScrolled)
        put("feedPostsRead", feedPostsRead)
    }

    companion object {
        fun fromJson(obj: JSONObject?): FeedStats {
            if (obj == null) return FeedStats()
            return FeedStats(
                feedPostsScrolled = obj.optInt("feedPostsScrolled", 0),
                feedPostsRead = obj.optInt("feedPostsRead", 0)
            )
        }
    }
}

data class YoutubeStats(
    val summary: SummaryStats = SummaryStats(),
    val shorts: ShortVideoStats = ShortVideoStats(),
    val longVideos: LongVideoStats = LongVideoStats(),
    val musicVideos: MusicVideoStats = MusicVideoStats()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("summary", summary.toJson())
        put("shorts", shorts.toJson())
        put("longVideos", longVideos.toJson())
        put("musicVideos", musicVideos.toJson())
    }

    companion object {
        fun fromJson(obj: JSONObject?): YoutubeStats {
            if (obj == null) return YoutubeStats()
            return YoutubeStats(
                summary = SummaryStats.fromJson(obj.optJSONObject("summary")),
                shorts = ShortVideoStats.fromJson(obj.optJSONObject("shorts") ?: obj.optJSONObject("shortVideos")),
                longVideos = LongVideoStats.fromJson(obj.optJSONObject("longVideos")),
                musicVideos = MusicVideoStats.fromJson(obj.optJSONObject("musicVideos"))
            )
        }
    }
}

data class FacebookStats(
    val summary: SummaryStats = SummaryStats(),
    val feed: FeedStats = FeedStats(),
    val reels: ShortVideoStats = ShortVideoStats(),
    val longVideos: LongVideoStats = LongVideoStats()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("summary", summary.toJson())
        put("feed", feed.toJson())
        put("reels", reels.toJson())
        put("longVideos", longVideos.toJson())
    }

    companion object {
        fun fromJson(obj: JSONObject?): FacebookStats {
            if (obj == null) return FacebookStats()
            return FacebookStats(
                summary = SummaryStats.fromJson(obj.optJSONObject("summary")),
                feed = FeedStats.fromJson(obj.optJSONObject("feed")),
                reels = ShortVideoStats.fromJson(obj.optJSONObject("reels")),
                longVideos = LongVideoStats.fromJson(obj.optJSONObject("longVideos"))
            )
        }
    }
}

data class TiktokStats(
    val summary: SummaryStats = SummaryStats(),
    val shorts: ShortVideoStats = ShortVideoStats()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("summary", summary.toJson())
        put("shorts", shorts.toJson())
    }

    companion object {
        fun fromJson(obj: JSONObject?): TiktokStats {
            if (obj == null) return TiktokStats()
            return TiktokStats(
                summary = SummaryStats.fromJson(obj.optJSONObject("summary")),
                shorts = ShortVideoStats.fromJson(obj.optJSONObject("shorts") ?: obj.optJSONObject("shortVideos"))
            )
        }
    }
}

data class ReflectionData(
    val lesson1: String = "",
    val lesson2: String = "",
    val rating: Int = 5,
    val aiFeedback: String = "",
    val submittedAt: Long? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("lesson1", lesson1)
        put("lesson2", lesson2)
        put("rating", rating)
        put("aiFeedback", aiFeedback)
        if (submittedAt != null) put("submittedAt", submittedAt) else put("submittedAt", JSONObject.NULL)
    }

    companion object {
        fun fromJson(obj: JSONObject?): ReflectionData {
            if (obj == null) return ReflectionData()
            return ReflectionData(
                lesson1 = obj.optString("lesson1", ""),
                lesson2 = obj.optString("lesson2", ""),
                rating = obj.optInt("rating", 5),
                aiFeedback = obj.optString("aiFeedback", ""),
                submittedAt = if (obj.has("submittedAt") && !obj.isNull("submittedAt")) obj.getLong("submittedAt") else null
            )
        }
    }
}

data class WatchedVideoItem(
    val id: String = "",
    val title: String = "",
    val channel: String = "",
    val platform: String = "youtube",
    val category: String = "leisure", // "goal" | "leisure" | "distraction" | "unclassified"
    val watchedSeconds: Int = 0,
    val durationSeconds: Int = 0,
    val isCompletion: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val userOverridden: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("channel", channel)
        put("platform", platform)
        put("category", category)
        put("watchedSeconds", watchedSeconds)
        put("durationSeconds", durationSeconds)
        put("isCompletion", isCompletion)
        put("timestamp", timestamp)
        put("userOverridden", userOverridden)
    }

    companion object {
        fun fromJson(obj: JSONObject?): WatchedVideoItem {
            if (obj == null) return WatchedVideoItem()
            return WatchedVideoItem(
                id = obj.optString("id", ""),
                title = obj.optString("title", ""),
                channel = obj.optString("channel", ""),
                platform = obj.optString("platform", "youtube"),
                category = obj.optString("category", "leisure"),
                watchedSeconds = obj.optInt("watchedSeconds", 0),
                durationSeconds = obj.optInt("durationSeconds", 0),
                isCompletion = obj.optBoolean("isCompletion", false),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                userOverridden = obj.optBoolean("userOverridden", false)
            )
        }
    }
}

data class HourlyStatsItem(
    val hour: Int = 0,
    val swipes: Int = 0,
    val longVideos: Int = 0,
    val feedScrolled: Int = 0,
    val reloads: Int = 0,
    val activeSeconds: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("hour", hour)
        put("swipes", swipes)
        put("longVideos", longVideos)
        put("feedScrolled", feedScrolled)
        put("reloads", reloads)
        put("activeSeconds", activeSeconds)
    }

    companion object {
        fun fromJson(obj: JSONObject?): HourlyStatsItem {
            if (obj == null) return HourlyStatsItem()
            return HourlyStatsItem(
                hour = obj.optInt("hour", 0),
                swipes = obj.optInt("swipes", 0),
                longVideos = obj.optInt("longVideos", 0),
                feedScrolled = obj.optInt("feedScrolled", 0),
                reloads = obj.optInt("reloads", 0),
                activeSeconds = obj.optLong("activeSeconds", 0L)
            )
        }
    }
}

fun createDefaultHourlyList(): List<HourlyStatsItem> = (0..23).map { HourlyStatsItem(hour = it) }

data class TemptationStats(
    val resistedCount: Int = 0,
    val succumbedCount: Int = 0
) {
    val total: Int get() = resistedCount + succumbedCount
    val resistedRatioPercentage: Int
        get() = if (total > 0) ((resistedCount.toDouble() / total) * 100).toInt().coerceIn(0, 100) else 100

    fun toJson(): JSONObject = JSONObject().apply {
        put("resistedCount", resistedCount)
        put("succumbedCount", succumbedCount)
    }

    companion object {
        fun fromJson(obj: JSONObject?): TemptationStats {
            if (obj == null) return TemptationStats()
            return TemptationStats(
                resistedCount = obj.optInt("resistedCount", 0),
                succumbedCount = obj.optInt("succumbedCount", 0)
            )
        }
    }
}

/**
 * Daily statistics container with granular per-platform data (YouTube, Facebook, TikTok),
 * video categorization audit log, and backward-compatible aggregated properties.
 */
data class DailyStats(
    val date: String = getTodayDateString(),
    val youtube: YoutubeStats = YoutubeStats(),
    val facebook: FacebookStats = FacebookStats(),
    val tiktok: TiktokStats = TiktokStats(),
    val currentShortId: String = "",
    val reflection: ReflectionData = ReflectionData(),
    val watchedVideos: List<WatchedVideoItem> = emptyList(),
    val unmatchedQueue: List<WatchedVideoItem> = emptyList(),
    val studyVideoLogs: List<StudyVideoLogItem> = emptyList(),
    val hourly: List<HourlyStatsItem> = createDefaultHourlyList(),
    val temptation: TemptationStats = TemptationStats(),
    val petEnergyEndOfDay: Int = 100,
    val desktopSwipes: Int = 0
) {
    constructor(
        totalSwipes: Int,
        validViews: Int = 0,
        loopViews: Int = 0,
        activeSeconds: Long = 0L,
        reflection: ReflectionData = ReflectionData(),
        date: String = getTodayDateString()
    ) : this(
        date = date,
        youtube = YoutubeStats(
            summary = SummaryStats(activeSeconds = activeSeconds),
            shorts = ShortVideoStats(
                totalSwipes = totalSwipes,
                validViews = validViews,
                loopViews = loopViews
            )
        ),
        reflection = reflection
    )

    constructor(activeSeconds: Long) : this(
        youtube = YoutubeStats(
            summary = SummaryStats(activeSeconds = activeSeconds)
        )
    )

    // --- Aggregated totals across all platforms ---
    val mobileSwipes: Int
        get() = youtube.shorts.totalSwipes + facebook.reels.totalSwipes + facebook.feed.feedPostsScrolled + tiktok.shorts.totalSwipes

    val totalSwipes: Int
        get() = mobileSwipes + desktopSwipes

    val validViews: Int
        get() = youtube.shorts.validViews + facebook.reels.validViews + facebook.feed.feedPostsRead + tiktok.shorts.validViews

    val loopViews: Int
        get() = youtube.shorts.loopViews + facebook.reels.loopViews + tiktok.shorts.loopViews

    val activeSeconds: Long
        get() = youtube.summary.activeTimeSeconds + facebook.summary.activeTimeSeconds + tiktok.summary.activeTimeSeconds

    val passiveSeconds: Long
        get() = youtube.summary.passiveTimeSeconds + facebook.summary.passiveTimeSeconds + tiktok.summary.passiveTimeSeconds

    val totalSeconds: Long
        get() = activeSeconds + passiveSeconds

    val reloadCount: Int
        get() = youtube.summary.reloadCount + facebook.summary.reloadCount + tiktok.summary.reloadCount

    val formattedActiveTime: String
        get() = formatSeconds(activeSeconds)

    val formattedTotalTime: String
        get() {
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }

    val deepViewRatioPercentage: Int
        get() {
            if (totalSwipes == 0) return 0
            return ((validViews.toDouble() / totalSwipes.toDouble()) * 100).toInt().coerceIn(0, 100)
        }

    // --- Content Alignment & Video Categorization Metrics ---
    val goalVideosCount: Int
        get() = watchedVideos.count { it.category == "goal" }

    val leisureVideosCount: Int
        get() = watchedVideos.count { it.category == "leisure" }

    val distractionVideosCount: Int
        get() = watchedVideos.count { it.category == "distraction" }

    val unclassifiedVideosCount: Int
        get() = watchedVideos.count { it.category == "unclassified" }

    val totalCategorizedCount: Int
        get() = watchedVideos.size

    val goalRatioPercentage: Int
        get() = if (totalCategorizedCount > 0) ((goalVideosCount.toDouble() / totalCategorizedCount) * 100).toInt().coerceIn(0, 100) else 0

    val leisureRatioPercentage: Int
        get() = if (totalCategorizedCount > 0) ((leisureVideosCount.toDouble() / totalCategorizedCount) * 100).toInt().coerceIn(0, 100) else 0

    val distractionRatioPercentage: Int
        get() = if (totalCategorizedCount > 0) ((distractionVideosCount.toDouble() / totalCategorizedCount) * 100).toInt().coerceIn(0, 100) else 0

    val unclassifiedRatioPercentage: Int
        get() = if (totalCategorizedCount > 0) ((unclassifiedVideosCount.toDouble() / totalCategorizedCount) * 100).toInt().coerceIn(0, 100) else 0

    val totalLeisureWatchedSeconds: Long
        get() = watchedVideos.filter { it.category == "leisure" }.sumOf { it.watchedSeconds.toLong() }

    val totalLongVideosWatched: Int
        get() = youtube.longVideos.totalWatched + facebook.longVideos.totalWatched

    val deepWatchCount: Int
        get() = youtube.longVideos.usefulCount + facebook.longVideos.usefulCount +
                watchedVideos.count { it.watchedSeconds >= 180 || it.category == "goal" }

    val impulsiveSkipCount: Int
        get() = youtube.longVideos.impulsiveCount + facebook.longVideos.impulsiveCount +
                watchedVideos.count { it.watchedSeconds < 30 && it.category == "distraction" }

    val usefulVideosRatioPercentage: Int
        get() {
            val total = maxOf(totalLongVideosWatched, watchedVideos.size)
            return if (total > 0) ((deepWatchCount.toDouble() / total) * 100).toInt().coerceIn(0, 100) else goalRatioPercentage
        }

    val impulsiveSkipRatioPercentage: Int
        get() {
            val total = maxOf(totalLongVideosWatched, watchedVideos.size)
            return if (total > 0) ((impulsiveSkipCount.toDouble() / total) * 100).toInt().coerceIn(0, 100) else 0
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("youtube", youtube.toJson())
        put("facebook", facebook.toJson())
        put("tiktok", tiktok.toJson())
        put("currentShortId", currentShortId)
        put("reflection", reflection.toJson())

        val videosArr = JSONArray()
        watchedVideos.forEach { videosArr.put(it.toJson()) }
        put("watchedVideos", videosArr)

        val unmatchedArr = JSONArray()
        unmatchedQueue.forEach { unmatchedArr.put(it.toJson()) }
        put("unmatchedQueue", unmatchedArr)

        val studyArr = JSONArray()
        studyVideoLogs.forEach { studyArr.put(it.toJson()) }
        put("studyVideoLogs", studyArr)

        val hourlyArr = JSONArray()
        hourly.forEach { hourlyArr.put(it.toJson()) }
        put("hourly", hourlyArr)

        put("temptation", temptation.toJson())
        put("petEnergyEndOfDay", petEnergyEndOfDay)
        put("desktopSwipes", desktopSwipes)
    }

    companion object Serializer {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun isMusicVideo(title: String, channel: String): Boolean {
            val ch = channel.lowercase()
            if (ch.endsWith("- topic") || ch.endsWith("- chủ đề") || ch.contains("official music") || ch.contains("records") || ch.contains("vevo")) {
                return true
            }
            val t = title.lowercase()
            val musicRegex = Regex("""\b(music|lofi|chill|playlist|nhạc|bài hát|soundtrack|acoustic|instrumental|remix|piano|ambient|audio|mv|official music video|karaoke|beat|lyric|lyrics|mashup|ost|medley|guitar|synthwave|relaxing)\b""", RegexOption.IGNORE_CASE)
            return musicRegex.containsMatchIn(t) || musicRegex.containsMatchIn(ch)
        }

        fun fromJson(json: JSONObject): DailyStats {
            val date = json.optString("date", getTodayDateString())
            val ytObj = json.optJSONObject("youtube")
            val fbObj = json.optJSONObject("facebook")
            val ttObj = json.optJSONObject("tiktok")
            val refObj = json.optJSONObject("reflection")

            val yt = if (ytObj != null) {
                YoutubeStats.fromJson(ytObj)
            } else {
                val flatSwipes = json.optInt("totalSwipes", 0)
                val flatValid = json.optInt("validViews", 0)
                val flatActive = json.optLong("activeSeconds", 0L)
                val flatReload = json.optInt("reloadCount", 0)
                val flatLoops = json.optInt("loopViews", 0)
                val flatImpulsive = json.optInt("impulsiveCount", 0)
                val flatUseful = json.optInt("usefulCount", 0)
                val flatLong = json.optInt("longWatchedCount", 0)

                YoutubeStats(
                    summary = SummaryStats(activeSeconds = flatActive, reloadCount = flatReload),
                    shorts = ShortVideoStats(totalSwipes = flatSwipes, validViews = flatValid, loopViews = flatLoops, impulsiveCount = maxOf(0, flatSwipes - flatValid)),
                    longVideos = LongVideoStats(totalWatched = flatLong, usefulCount = flatUseful, impulsiveCount = flatImpulsive)
                )
            }

            val fb = FacebookStats.fromJson(fbObj)
            val tt = TiktokStats.fromJson(ttObj)
            val ref = ReflectionData.fromJson(refObj)

            val vList = mutableListOf<WatchedVideoItem>()
            val videosArr = json.optJSONArray("watchedVideos")
            if (videosArr != null) {
                for (i in 0 until videosArr.length()) {
                    val vObj = videosArr.optJSONObject(i)
                    if (vObj != null) {
                        vList.add(WatchedVideoItem.fromJson(vObj))
                    }
                }
            }

            val uList = mutableListOf<WatchedVideoItem>()
            val unmatchedArr = json.optJSONArray("unmatchedQueue")
            if (unmatchedArr != null) {
                for (i in 0 until unmatchedArr.length()) {
                    val uObj = unmatchedArr.optJSONObject(i)
                    if (uObj != null) {
                        uList.add(WatchedVideoItem.fromJson(uObj))
                    }
                }
            }

            val studyList = mutableListOf<StudyVideoLogItem>()
            val studyArr = json.optJSONArray("studyVideoLogs")
            if (studyArr != null) {
                for (i in 0 until studyArr.length()) {
                    val sObj = studyArr.optJSONObject(i)
                    if (sObj != null) {
                        studyList.add(StudyVideoLogItem.fromJson(sObj))
                    }
                }
            }

            val hourlyList = mutableListOf<HourlyStatsItem>()
            val hourlyArr = json.optJSONArray("hourly")
            if (hourlyArr != null) {
                for (i in 0 until hourlyArr.length()) {
                    val hObj = hourlyArr.optJSONObject(i)
                    if (hObj != null) hourlyList.add(HourlyStatsItem.fromJson(hObj))
                }
            }
            val finalHourly = if (hourlyList.size == 24) hourlyList else createDefaultHourlyList()

            val temptation = TemptationStats.fromJson(json.optJSONObject("temptation"))
            val petEnergy = json.optInt("petEnergyEndOfDay", 100)
            val desktopSwipes = json.optInt("desktopSwipes", 0)

            return DailyStats(
                date = date,
                youtube = yt,
                facebook = fb,
                tiktok = tt,
                currentShortId = json.optString("currentShortId", ""),
                reflection = ref,
                watchedVideos = vList,
                unmatchedQueue = uList,
                studyVideoLogs = studyList,
                hourly = finalHourly,
                temptation = temptation,
                petEnergyEndOfDay = petEnergy,
                desktopSwipes = desktopSwipes
            )
        }
    }
}
