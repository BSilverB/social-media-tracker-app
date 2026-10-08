package com.example.social_media_tracker_app.bridge

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import com.example.social_media_tracker_app.data.repository.StatsRepository

/**
 * JavaScript interface bridging the injected script in WebView with native Android.
 * Registered as window.MindfulBridge in WebView.
 */
class WebAppInterface(
    private val context: Context,
    private val repository: StatsRepository,
    private val onStudyCheckInNeeded: ((videoId: String, title: String) -> Unit)? = null
) {
    companion object {
        const val INTERFACE_NAME = "MindfulBridge"
        private const val TAG = "WebAppInterface"
    }

    /**
     * Called when user swipes to a new Short / Reel / TikTok.
     */
    @JavascriptInterface
    fun onShortSwipe(platform: String, shortId: String, totalSwipes: Int) {
        Log.d(TAG, "onShortSwipe: platform=$platform, shortId=$shortId, totalSwipes=$totalSwipes")
        repository.recordSwipe(platform, shortId, totalSwipes)
    }

    @JavascriptInterface
    fun onShortSwipe(shortId: String, totalSwipes: Int) {
        onShortSwipe("youtube", shortId, totalSwipes)
    }

    /**
     * Called when a Short / Reel / TikTok has been watched for >= 2 seconds (Deep view).
     */
    @JavascriptInterface
    fun onValidView(platform: String, shortId: String, validViews: Int) {
        Log.d(TAG, "onValidView: platform=$platform, shortId=$shortId, validViews=$validViews")
        repository.recordValidView(platform, shortId, validViews)
    }

    @JavascriptInterface
    fun onValidView(shortId: String, validViews: Int) {
        onValidView("youtube", shortId, validViews)
    }

    /**
     * Called when a video completes a loop cycle.
     */
    @JavascriptInterface
    fun onLoopView(platform: String, shortId: String, loopViews: Int) {
        Log.d(TAG, "onLoopView: platform=$platform, shortId=$shortId, loopViews=$loopViews")
        repository.recordLoopView(platform, shortId, loopViews)
    }

    @JavascriptInterface
    fun onLoopView(shortId: String, loopViews: Int) {
        onLoopView("youtube", shortId, loopViews)
    }

    /**
     * Called when user reloads/refreshes the page.
     */
    @JavascriptInterface
    fun onReload(platform: String) {
        Log.d(TAG, "onReload: platform=$platform")
        repository.recordReload(platform)
    }

    /**
     * Called when a long video completes playback or is navigated away from.
     */
    @JavascriptInterface
    fun onLongVideo(platform: String, isUseful: Boolean, isImpulsive: Boolean) {
        Log.d(TAG, "onLongVideo: platform=$platform, isUseful=$isUseful, isImpulsive=$isImpulsive")
        repository.recordLongVideo(platform, isUseful, isImpulsive)
    }

    /**
     * Called when a music video plays during Pomodoro Music Focus.
     */
    @JavascriptInterface
    fun onMusicVideo(platform: String, videoId: String, title: String, duration: Int) {
        Log.d(TAG, "onMusicVideo: platform=$platform, videoId=$videoId, duration=$duration")
        repository.recordMusicVideoWatched(platform, videoId, title, duration)
    }

    /**
     * Triggered after 10s during Pomodoro Study Focus if a video is questionable / unclassified.
     */
    @JavascriptInterface
    fun onStudyCheckInRequired(videoId: String, title: String) {
        Log.d(TAG, "onStudyCheckInRequired: videoId=$videoId, title=$title")
        onStudyCheckInNeeded?.invoke(videoId, title)
    }

    /**
     * Called when a video has played for >= 3 seconds, carrying metadata for on-device categorization.
     */
    @JavascriptInterface
    fun onVideoWatched(platform: String, videoId: String, title: String, channel: String, duration: Int, watchedSeconds: Int) {
        Log.d(TAG, "onVideoWatched: platform=$platform, videoId=$videoId, title=$title, channel=$channel, watched=$watchedSeconds")
        repository.recordVideoWatched(platform, videoId, title, channel, duration, watchedSeconds)
    }

    /**
     * Called when Facebook feed is scrolled or read.
     */
    @JavascriptInterface
    fun onFeedScroll(isRead: Boolean) {
        Log.d(TAG, "onFeedScroll: isRead=$isRead")
        repository.recordFeedScroll(isRead)
    }

    /**
     * Synchronizes full stats batch from WebView script for a specific platform.
     */
    @JavascriptInterface
    fun updatePlatformStats(platform: String, jsonPayload: String) {
        Log.d(TAG, "updatePlatformStats: platform=$platform, payload=$jsonPayload")
        repository.updatePlatformStats(platform, jsonPayload)
    }

    @JavascriptInterface
    fun updateStats(jsonPayload: String) {
        repository.updateFromJson(jsonPayload)
    }

    /**
     * Called when the user transitions to a different activity mode (shorts, reels, feed, long_video, browse).
     */
    @JavascriptInterface
    fun onActivityChanged(platform: String, activityType: String, detail: String) {
        Log.d(TAG, "onActivityChanged: platform=$platform, activityType=$activityType, detail=$detail")
        repository.updateActivity(platform, activityType, detail)
    }

    /**
     * Called on scroll events: true = scrolling up (show nav/bars), false = scrolling down (hide nav/bars).
     */
    @JavascriptInterface
    fun onScrollDirection(isScrollingUp: Boolean) {
        repository.setScrollingUp(isScrollingUp)
    }

    /**
     * General logger for injected JS scripts.
     */
    @JavascriptInterface
    fun log(msg: String) {
        Log.i(TAG, "[WebViewJS] $msg")
    }
}
