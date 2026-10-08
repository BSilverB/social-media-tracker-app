package com.example.social_media_tracker_app

import com.example.social_media_tracker_app.data.model.DailyStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyStatsUnitTest {

    @Test
    fun testDefaultStatsInitialization() {
        val stats = DailyStats()
        assertEquals(0, stats.totalSwipes)
        assertEquals(0, stats.validViews)
        assertEquals(0, stats.loopViews)
        assertEquals(0L, stats.activeSeconds)
        assertTrue(stats.date.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun testFormattedActiveTime() {
        val stats1 = DailyStats(activeSeconds = 45)
        assertEquals("45s", stats1.formattedActiveTime)

        val stats2 = DailyStats(activeSeconds = 125)
        assertEquals("2m 5s", stats2.formattedActiveTime)

        val stats3 = DailyStats(activeSeconds = 3600)
        assertEquals("1h 0m", stats3.formattedActiveTime)
    }

    @Test
    fun testDeepViewRatioPercentage() {
        val statsEmpty = DailyStats(totalSwipes = 0, validViews = 0)
        assertEquals(0, statsEmpty.deepViewRatioPercentage)

        val statsFifty = DailyStats(totalSwipes = 10, validViews = 5)
        assertEquals(50, statsFifty.deepViewRatioPercentage)

        val statsSeventyFive = DailyStats(totalSwipes = 4, validViews = 3)
        assertEquals(75, statsSeventyFive.deepViewRatioPercentage)
    }

    @Test
    fun testStandardizedStorageSchema() {
        val original = DailyStats(
            date = "2026-10-08",
            unmatchedQueue = listOf(
                com.example.social_media_tracker_app.data.model.WatchedVideoItem(
                    id = "unmatched_1",
                    title = "Video AI Unmatched",
                    durationSeconds = 400,
                    isCompletion = true
                )
            ),
            reflection = com.example.social_media_tracker_app.data.model.ReflectionData(
                lesson1 = "Học tập",
                lesson2 = "Kỷ luật",
                aiFeedback = "Làm tốt lắm"
            )
        )

        val json = original.toJson()
        val parsed = DailyStats.Serializer.fromJson(json)

        assertEquals("2026-10-08", parsed.date)
        assertEquals(1, parsed.unmatchedQueue.size)
        assertEquals("unmatched_1", parsed.unmatchedQueue[0].id)
        assertEquals(400, parsed.unmatchedQueue[0].durationSeconds)
        assertTrue(parsed.unmatchedQueue[0].isCompletion)
        assertEquals("Làm tốt lắm", parsed.reflection.aiFeedback)
    }

    @Test
    fun testSummaryStatsStandardAndLegacyCompatibility() {
        val summary = com.example.social_media_tracker_app.data.model.SummaryStats(
            activeSeconds = 120,
            passiveSeconds = 30,
            reloadCount = 2
        )
        val json = summary.toJson()

        assertEquals(120L, json.getLong("activeSeconds"))
        assertEquals(30L, json.getLong("passiveSeconds"))
        assertEquals(120L, json.getLong("activeTimeSeconds"))
        assertEquals(30L, json.getLong("passiveTimeSeconds"))

        val parsed = com.example.social_media_tracker_app.data.model.SummaryStats.fromJson(json)
        assertEquals(120L, parsed.activeSeconds)
        assertEquals(120L, parsed.activeTimeSeconds)
        assertEquals(30L, parsed.passiveSeconds)
        assertEquals(30L, parsed.passiveTimeSeconds)
    }

    @Test
    fun testHourlyStatsWithLongVideosAndFeedScrolled() {
        val hourly = com.example.social_media_tracker_app.data.model.HourlyStatsItem(
            hour = 14,
            swipes = 15,
            longVideos = 2,
            feedScrolled = 25,
            reloads = 3,
            activeSeconds = 450
        )
        val json = hourly.toJson()
        val parsed = com.example.social_media_tracker_app.data.model.HourlyStatsItem.fromJson(json)

        assertEquals(14, parsed.hour)
        assertEquals(15, parsed.swipes)
        assertEquals(2, parsed.longVideos)
        assertEquals(25, parsed.feedScrolled)
        assertEquals(3, parsed.reloads)
        assertEquals(450L, parsed.activeSeconds)
    }
}
