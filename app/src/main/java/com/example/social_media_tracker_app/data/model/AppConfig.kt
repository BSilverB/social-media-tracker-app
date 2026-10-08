package com.example.social_media_tracker_app.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Global App Configuration matching extension's DEFAULT_CONFIG
 */
data class ThresholdConfig(
    val m1: Int = 15,
    val m2: Int = 30,
    val m3: Int = 45
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("m1", m1)
        put("m2", m2)
        put("m3", m3)
    }

    companion object {
        fun fromJson(obj: JSONObject?, defaultM1: Int = 15, defaultM2: Int = 30, defaultM3: Int = 45): ThresholdConfig {
            if (obj == null) return ThresholdConfig(defaultM1, defaultM2, defaultM3)
            return ThresholdConfig(
                m1 = obj.optInt("m1", defaultM1),
                m2 = obj.optInt("m2", defaultM2),
                m3 = obj.optInt("m3", defaultM3)
            )
        }
    }
}

data class YoutubeThresholds(
    val shorts: ThresholdConfig = ThresholdConfig(15, 30, 45),
    val long: ThresholdConfig = ThresholdConfig(3, 5, 8)
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("shorts", shorts.toJson())
        put("long", long.toJson())
    }

    companion object {
        fun fromJson(obj: JSONObject?): YoutubeThresholds {
            if (obj == null) return YoutubeThresholds()
            return YoutubeThresholds(
                shorts = ThresholdConfig.fromJson(obj.optJSONObject("shorts"), 15, 30, 45),
                long = ThresholdConfig.fromJson(obj.optJSONObject("long"), 3, 5, 8)
            )
        }
    }
}

data class FacebookThresholds(
    val reels: ThresholdConfig = ThresholdConfig(15, 30, 45),
    val feeds: ThresholdConfig = ThresholdConfig(20, 40, 60),
    val long: ThresholdConfig = ThresholdConfig(2, 4, 6)
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("reels", reels.toJson())
        put("feeds", feeds.toJson())
        put("long", long.toJson())
    }

    companion object {
        fun fromJson(obj: JSONObject?): FacebookThresholds {
            if (obj == null) return FacebookThresholds()
            return FacebookThresholds(
                reels = ThresholdConfig.fromJson(obj.optJSONObject("reels"), 15, 30, 45),
                feeds = ThresholdConfig.fromJson(obj.optJSONObject("feeds"), 20, 40, 60),
                long = ThresholdConfig.fromJson(obj.optJSONObject("long"), 2, 4, 6)
            )
        }
    }
}

data class TiktokThresholds(
    val shorts: ThresholdConfig = ThresholdConfig(15, 30, 45)
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("shorts", shorts.toJson())
    }

    companion object {
        fun fromJson(obj: JSONObject?): TiktokThresholds {
            if (obj == null) return TiktokThresholds()
            return TiktokThresholds(
                shorts = ThresholdConfig.fromJson(obj.optJSONObject("shorts"), 15, 30, 45)
            )
        }
    }
}

data class PlatformThresholds(
    val youtube: YoutubeThresholds = YoutubeThresholds(),
    val facebook: FacebookThresholds = FacebookThresholds(),
    val tiktok: TiktokThresholds = TiktokThresholds()
) {
    // Backward compatibility helper
    val m1: Int get() = youtube.shorts.m1
    val m2: Int get() = youtube.shorts.m2
    val m3: Int get() = youtube.shorts.m3

    fun withGlobalShortsThreshold(newM1: Int, newM2: Int, newM3: Int): PlatformThresholds {
        val cfg = ThresholdConfig(newM1, newM2, newM3)
        return this.copy(
            youtube = this.youtube.copy(shorts = cfg),
            facebook = this.facebook.copy(reels = cfg),
            tiktok = this.tiktok.copy(shorts = cfg)
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("youtube", youtube.toJson())
        put("facebook", facebook.toJson())
        put("tiktok", tiktok.toJson())
        // Legacy fields for backward compatibility
        put("m1", m1)
        put("m2", m2)
        put("m3", m3)
    }

    companion object {
        fun fromJson(obj: JSONObject?): PlatformThresholds {
            if (obj == null) return PlatformThresholds()

            // If object has youtube or facebook keys, parse hierarchical
            if (obj.has("youtube") || obj.has("facebook")) {
                val yt = YoutubeThresholds.fromJson(obj.optJSONObject("youtube"))
                val fb = FacebookThresholds.fromJson(obj.optJSONObject("facebook"))
                val tt = TiktokThresholds.fromJson(obj.optJSONObject("tiktok"))
                return PlatformThresholds(youtube = yt, facebook = fb, tiktok = tt)
            }

            // Fallback for flat structure: {"m1": 15, "m2": 30, "m3": 45}
            val m1 = obj.optInt("m1", 15)
            val m2 = obj.optInt("m2", 30)
            val m3 = obj.optInt("m3", 45)
            return PlatformThresholds(
                youtube = YoutubeThresholds(
                    shorts = ThresholdConfig(m1, m2, m3),
                    long = ThresholdConfig(3, 5, 8)
                ),
                facebook = FacebookThresholds(
                    reels = ThresholdConfig(m1, m2, m3),
                    feeds = ThresholdConfig(20, 40, 60),
                    long = ThresholdConfig(2, 4, 6)
                ),
                tiktok = TiktokThresholds(
                    shorts = ThresholdConfig(m1, m2, m3)
                )
            )
        }
    }
}

data class PomodoroConfig(
    val enabled: Boolean = false,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5
)

data class BedtimeConfig(
    val enabled: Boolean = true,
    val start: String = "22:30",
    val end: String = "05:00"
) {
    val startTime: String get() = start
    val endTime: String get() = end

    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("start", start)
        put("end", end)
    }

    companion object {
        fun fromJson(obj: JSONObject?): BedtimeConfig {
            if (obj == null) return BedtimeConfig()
            return BedtimeConfig(
                enabled = obj.optBoolean("enabled", true),
                start = obj.optString("start", "22:30"),
                end = obj.optString("end", "05:00")
            )
        }
    }
}

data class ReflectionConfig(
    val reminderTime: String = "22:00"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("reminderTime", reminderTime)
    }

    companion object {
        fun fromJson(obj: JSONObject?): ReflectionConfig {
            if (obj == null) return ReflectionConfig()
            return ReflectionConfig(
                reminderTime = obj.optString("reminderTime", "22:00")
            )
        }
    }
}

data class YoutubeLimits(
    val maxShortSwipes: Int = 20,
    val maxLongVideos: Int = 3
)

data class FacebookLimits(
    val maxFeedPosts: Int = 30,
    val maxReelsSwipes: Int = 20,
    val maxLongVideos: Int = 2
)

data class TiktokLimits(
    val maxShortSwipes: Int = 25
)

data class PlatformLimits(
    val youtube: YoutubeLimits = YoutubeLimits(),
    val facebook: FacebookLimits = FacebookLimits(),
    val tiktok: TiktokLimits = TiktokLimits()
)

data class LocalPreferences(
    val hudPositionX: Int = 20,
    val hudPositionY: Int = 80,
    val grayscaleMode: String = "threshold_m2"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        val hudPos = JSONObject().apply {
            put("x", hudPositionX)
            put("y", hudPositionY)
        }
        put("hudPosition", hudPos)
        put("grayscaleMode", grayscaleMode)
    }

    companion object {
        fun fromJson(obj: JSONObject?): LocalPreferences {
            if (obj == null) return LocalPreferences()
            val hud = obj.optJSONObject("hudPosition")
            val x = hud?.optInt("x", 20) ?: 20
            val y = hud?.optInt("y", 80) ?: 80
            val gray = obj.optString("grayscaleMode", "threshold_m2")
            return LocalPreferences(x, y, gray)
        }
    }
}

data class AppConfig(
    val masterGoal: String = "Muốn trở thành phiên bản tốt hơn",
    val emotionalAnchorImage: String = "",
    val geminiApiKey: String = "",
    val syncCode: String = "MF-8924",
    val autoSync: Boolean = true,
    val leisureQuotaMinutes: Int = 45,
    val thresholds: PlatformThresholds = PlatformThresholds(),
    val platformLimits: PlatformLimits = PlatformLimits(),
    val pomodoro: PomodoroConfig = PomodoroConfig(),
    val bedtime: BedtimeConfig = BedtimeConfig(),
    val reflection: ReflectionConfig = ReflectionConfig(),
    val targetKeywords: List<String> = DEFAULT_TARGET_KEYWORDS,
    val leisureKeywords: List<String> = DEFAULT_LEISURE_KEYWORDS,
    val distractionKeywords: List<String> = DEFAULT_DISTRACTION_KEYWORDS,
    val localPreferences: LocalPreferences = LocalPreferences()
) {
    val apiKey: String get() = geminiApiKey
    fun toJson(): String {
        val root = JSONObject()
        root.put("syncCode", syncCode)
        root.put("auto_sync", autoSync)
        root.put("masterGoal", masterGoal)
        root.put("emotionalAnchorImage", emotionalAnchorImage)
        root.put("geminiApiKey", geminiApiKey)
        root.put("leisureQuotaMinutes", leisureQuotaMinutes)

        root.put("thresholds", thresholds.toJson())

        val platObj = JSONObject().apply {
            val ytObj = JSONObject().apply {
                put("maxShortSwipes", platformLimits.youtube.maxShortSwipes)
                put("maxLongVideos", platformLimits.youtube.maxLongVideos)
            }
            put("youtube", ytObj)

            val fbObj = JSONObject().apply {
                put("maxFeedPosts", platformLimits.facebook.maxFeedPosts)
                put("maxReelsSwipes", platformLimits.facebook.maxReelsSwipes)
                put("maxLongVideos", platformLimits.facebook.maxLongVideos)
            }
            put("facebook", fbObj)

            val ttObj = JSONObject().apply {
                put("maxShortSwipes", platformLimits.tiktok.maxShortSwipes)
            }
            put("tiktok", ttObj)
        }
        root.put("platformLimits", platObj)

        val pomoObj = JSONObject().apply {
            put("enabled", pomodoro.enabled)
            put("focusMinutes", pomodoro.focusMinutes)
            put("breakMinutes", pomodoro.breakMinutes)
        }
        root.put("pomodoro", pomoObj)

        root.put("bedtime", bedtime.toJson())
        root.put("reflection", reflection.toJson())

        // Standard hierarchical keywords object
        val kwObject = JSONObject().apply {
            val kwArray = JSONArray()
            targetKeywords.forEach { kwArray.put(it) }
            put("target", kwArray)

            val leisureKwArray = JSONArray()
            leisureKeywords.forEach { leisureKwArray.put(it) }
            put("leisure", leisureKwArray)

            val distractionKwArray = JSONArray()
            distractionKeywords.forEach { distractionKwArray.put(it) }
            put("distraction", distractionKwArray)
        }
        root.put("keywords", kwObject)

        // Flat keywords for backwards compatibility
        val kwArray = JSONArray()
        targetKeywords.forEach { kwArray.put(it) }
        root.put("targetKeywords", kwArray)

        val leisureKwArray = JSONArray()
        leisureKeywords.forEach { leisureKwArray.put(it) }
        root.put("leisureKeywords", leisureKwArray)

        val distractionKwArray = JSONArray()
        distractionKeywords.forEach { distractionKwArray.put(it) }
        root.put("distractionKeywords", distractionKwArray)

        root.put("localPreferences", localPreferences.toJson())

        return root.toString()
    }

    companion object {
        val DEFAULT_TARGET_KEYWORDS = listOf(
            "lập trình", "tiếng anh", "kỹ năng", "sách", "học", "phát triển", "tài chính", "công nghệ"
        )

        val DEFAULT_LEISURE_KEYWORDS = listOf(
            "nhạc", "music", "song", "chill", "relax", "vlog", "du lịch", "travel",
            "ẩm thực", "nấu ăn", "food", "nấu", "thể thao", "bóng đá", "football",
            "game", "gaming", "hài", "funny", "phim", "movie", "review phim", "hoạt hình", "anime"
        )

        val DEFAULT_DISTRACTION_KEYWORDS = listOf(
            "drama", "phốt", "bóc phốt", "đại chiến", "scandal", "lộ clip", "sốc",
            "kinh hoàng", "hài bựa", "hài nhảm", "cờ bạc", "tài xỉu", "gái xinh nhảy",
            "khoe thân", "trend tiktok", "thách thức 24h", "chơi khăm", "prank", "reaction hài"
        )

        fun fromJson(jsonStr: String): AppConfig {
            return try {
                val obj = JSONObject(jsonStr)
                val thresholds = PlatformThresholds.fromJson(obj.optJSONObject("thresholds"))

                val platformLimits = if (obj.has("platformLimits")) {
                    val pl = obj.getJSONObject("platformLimits")
                    val yt = if (pl.has("youtube")) {
                        val y = pl.getJSONObject("youtube")
                        YoutubeLimits(
                            maxShortSwipes = y.optInt("maxShortSwipes", 20),
                            maxLongVideos = y.optInt("maxLongVideos", 3)
                        )
                    } else YoutubeLimits()

                    val fb = if (pl.has("facebook")) {
                        val f = pl.getJSONObject("facebook")
                        FacebookLimits(
                            maxFeedPosts = f.optInt("maxFeedPosts", 30),
                            maxReelsSwipes = f.optInt("maxReelsSwipes", 20),
                            maxLongVideos = f.optInt("maxLongVideos", 2)
                        )
                    } else FacebookLimits()

                    val tt = if (pl.has("tiktok")) {
                        val t = pl.getJSONObject("tiktok")
                        TiktokLimits(
                            maxShortSwipes = t.optInt("maxShortSwipes", 25)
                        )
                    } else TiktokLimits()

                    PlatformLimits(youtube = yt, facebook = fb, tiktok = tt)
                } else PlatformLimits()

                val pomodoro = if (obj.has("pomodoro")) {
                    val p = obj.getJSONObject("pomodoro")
                    PomodoroConfig(
                        enabled = p.optBoolean("enabled", false),
                        focusMinutes = p.optInt("focusMinutes", 25),
                        breakMinutes = p.optInt("breakMinutes", 5)
                    )
                } else PomodoroConfig()

                val bedtime = BedtimeConfig.fromJson(obj.optJSONObject("bedtime"))
                val reflection = ReflectionConfig.fromJson(obj.optJSONObject("reflection"))

                val keywords = mutableListOf<String>()
                val leisureKw = mutableListOf<String>()
                val distractionKw = mutableListOf<String>()

                if (obj.has("keywords")) {
                    val kObj = obj.getJSONObject("keywords")
                    if (kObj.has("target")) {
                        val arr = kObj.getJSONArray("target")
                        for (i in 0 until arr.length()) keywords.add(arr.getString(i))
                    }
                    if (kObj.has("leisure")) {
                        val arr = kObj.getJSONArray("leisure")
                        for (i in 0 until arr.length()) leisureKw.add(arr.getString(i))
                    }
                    if (kObj.has("distraction")) {
                        val arr = kObj.getJSONArray("distraction")
                        for (i in 0 until arr.length()) distractionKw.add(arr.getString(i))
                    }
                }

                if (keywords.isEmpty()) {
                    if (obj.has("targetKeywords")) {
                        val arr = obj.getJSONArray("targetKeywords")
                        for (i in 0 until arr.length()) keywords.add(arr.getString(i))
                    } else {
                        keywords.addAll(DEFAULT_TARGET_KEYWORDS)
                    }
                }

                if (leisureKw.isEmpty()) {
                    if (obj.has("leisureKeywords")) {
                        val arr = obj.getJSONArray("leisureKeywords")
                        for (i in 0 until arr.length()) leisureKw.add(arr.getString(i))
                    } else {
                        leisureKw.addAll(DEFAULT_LEISURE_KEYWORDS)
                    }
                }

                if (distractionKw.isEmpty()) {
                    if (obj.has("distractionKeywords")) {
                        val arr = obj.getJSONArray("distractionKeywords")
                        for (i in 0 until arr.length()) distractionKw.add(arr.getString(i))
                    } else {
                        distractionKw.addAll(DEFAULT_DISTRACTION_KEYWORDS)
                    }
                }

                val autoSync = obj.optBoolean("auto_sync", obj.optBoolean("autoSync", true))
                val localPreferences = LocalPreferences.fromJson(obj.optJSONObject("localPreferences"))

                AppConfig(
                    masterGoal = obj.optString("masterGoal", "Muốn trở thành phiên bản tốt hơn"),
                    emotionalAnchorImage = obj.optString("emotionalAnchorImage", ""),
                    geminiApiKey = obj.optString("geminiApiKey", ""),
                    syncCode = obj.optString("syncCode", "MF-8924").uppercase(),
                    autoSync = autoSync,
                    leisureQuotaMinutes = obj.optInt("leisureQuotaMinutes", 45),
                    thresholds = thresholds,
                    platformLimits = platformLimits,
                    pomodoro = pomodoro,
                    bedtime = bedtime,
                    reflection = reflection,
                    targetKeywords = keywords,
                    leisureKeywords = leisureKw,
                    distractionKeywords = distractionKw,
                    localPreferences = localPreferences
                )
            } catch (e: Exception) {
                AppConfig()
            }
        }
    }
}
