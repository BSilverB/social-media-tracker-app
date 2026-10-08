package com.example.social_media_tracker_app.data.model

/**
 * Represents the user's real-time interaction mode within the Mindful WebView container.
 */
data class ActivityState(
    val platform: String = "youtube", // "youtube", "facebook", "tiktok"
    val activityType: String = "browse", // "shorts", "reels", "tiktok", "long_video", "feed", "browse"
    val detail: String = "" // Video ID, title, or post info
) {
    val isShortVideo: Boolean
        get() = activityType in listOf("shorts", "reels", "tiktok")

    val isLongVideo: Boolean
        get() = activityType == "long_video"

    val isFeed: Boolean
        get() = activityType == "feed"

    val isBrowse: Boolean
        get() = activityType == "browse"

    val displayTitle: String
        get() = when (platform.lowercase()) {
            "youtube" -> when (activityType) {
                "shorts" -> "YouTube Shorts"
                "long_video" -> "YouTube Video"
                else -> "YouTube"
            }
            "facebook" -> when (activityType) {
                "feed" -> "Facebook Bảng Tin"
                "reels" -> "Facebook Reels"
                "long_video" -> "Facebook Watch"
                else -> "Facebook"
            }
            "tiktok" -> "TikTok"
            else -> "Lướt Web"
        }

    val displayIcon: String
        get() = when (platform.lowercase()) {
            "youtube" -> if (isShortVideo) "⚡" else "🎬"
            "facebook" -> if (isFeed) "📜" else if (isShortVideo) "⚡" else "👥"
            "tiktok" -> "🎵"
            else -> "🌐"
        }
}
