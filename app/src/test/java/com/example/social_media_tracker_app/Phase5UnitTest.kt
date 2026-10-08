package com.example.social_media_tracker_app

import com.example.social_media_tracker_app.data.model.AppConfig
import com.example.social_media_tracker_app.data.model.DailyStats
import com.example.social_media_tracker_app.data.model.PetAccessories
import com.example.social_media_tracker_app.data.model.PetState
import com.example.social_media_tracker_app.data.model.ReflectionData
import com.example.social_media_tracker_app.domain.pomodoro.PomodoroSessionType
import com.example.social_media_tracker_app.domain.pomodoro.PomodoroState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase5UnitTest {

    @Test
    fun testPetMoodCalculation() {
        assertEquals("happy", PetState.calculateMood(100))
        assertEquals("happy", PetState.calculateMood(70))
        assertEquals("neutral", PetState.calculateMood(69))
        assertEquals("neutral", PetState.calculateMood(40))
        assertEquals("sad", PetState.calculateMood(39))
        assertEquals("sad", PetState.calculateMood(0))
    }

    @Test
    fun testPetStateSerialization() {
        val original = PetState(
            energy = 85,
            mood = "happy",
            currentStreak = 5,
            streakDays = 5,
            lastPokeEnergyTime = 123456789L,
            accessories = PetAccessories(
                unlockedItems = listOf("sunglasses"),
                equippedHead = "sunglasses"
            )
        )

        val json = original.toJson()
        val parsed = PetState.fromJson(json)

        assertEquals(85, parsed.energy)
        assertEquals("happy", parsed.mood)
        assertEquals(5, parsed.streakDays)
        assertEquals("sunglasses", parsed.accessories.equippedHead)
        assertTrue(parsed.accessories.unlockedItems.contains("sunglasses"))
    }

    @Test
    fun testAppConfigSerialization() {
        val config = AppConfig(
            masterGoal = "Học tập để phát triển",
            emotionalAnchorImage = "test_img",
            geminiApiKey = "dummy_gemini_test_key_123"
        )
        val json = config.toJson()
        val parsed = AppConfig.fromJson(json)

        assertEquals("Học tập để phát triển", parsed.masterGoal)
        assertEquals("dummy_gemini_test_key_123", parsed.geminiApiKey)
        assertEquals(15, parsed.thresholds.m1)
        assertEquals(30, parsed.thresholds.m2)
        assertEquals(45, parsed.thresholds.m3)
    }

    @Test
    fun testDailyStatsWithReflection() {
        val ref = ReflectionData(
            lesson1 = "Thở 12s rất tốt",
            lesson2 = "Cần ngủ sớm",
            rating = 5,
            submittedAt = 987654321L
        )
        val stats = DailyStats(
            totalSwipes = 20,
            validViews = 15,
            reflection = ref
        )

        assertEquals(75, stats.deepViewRatioPercentage)
        assertEquals("Thở 12s rất tốt", stats.reflection.lesson1)
        assertEquals(5, stats.reflection.rating)
    }

    @Test
    fun testPomodoroStateLabels() {
        val focusMusicState = PomodoroState(
            isRunning = true,
            sessionType = PomodoroSessionType.FOCUS,
            remainingSeconds = 25 * 60,
            currentCycle = 1,
            totalCycles = 2,
            focusMode = "music"
        )
        assertEquals("🎵 25:00 (1/2)", focusMusicState.statusLabel)
        assertTrue(focusMusicState.isFocusSession)
        assertFalse(focusMusicState.isBreakSession)

        val focusStudyState = PomodoroState(
            isRunning = true,
            sessionType = PomodoroSessionType.FOCUS,
            remainingSeconds = 25 * 60,
            currentCycle = 2,
            totalCycles = 4,
            focusMode = "study"
        )
        assertEquals("📚 25:00 (2/4)", focusStudyState.statusLabel)

        val breakState = PomodoroState(
            isRunning = true,
            sessionType = PomodoroSessionType.BREAK,
            remainingSeconds = 5 * 60,
            currentCycle = 1,
            totalCycles = 2
        )
        assertEquals("☕ 05:00 (Nghỉ 1/2)", breakState.statusLabel)
        assertFalse(breakState.isFocusSession)
        assertTrue(breakState.isBreakSession)
    }

    @Test
    fun testPlatformThresholdsAndBedtimeConfig() {
        val config = AppConfig(
            bedtime = com.example.social_media_tracker_app.data.model.BedtimeConfig(
                enabled = true,
                start = "23:00",
                end = "06:00"
            ),
            reflection = com.example.social_media_tracker_app.data.model.ReflectionConfig(
                reminderTime = "21:45"
            )
        )
        val json = config.toJson()
        val parsed = AppConfig.fromJson(json)

        assertTrue(parsed.bedtime.enabled)
        assertEquals("23:00", parsed.bedtime.start)
        assertEquals("06:00", parsed.bedtime.end)
        assertEquals("23:00", parsed.bedtime.startTime)
        assertEquals("06:00", parsed.bedtime.endTime)
        assertEquals("21:45", parsed.reflection.reminderTime)
    }

    @Test
    fun testMusicVideoStatsInDailyStats() {
        val stats = DailyStats(
            date = "2026-10-03"
        )
        assertEquals(0, stats.youtube.musicVideos.totalWatched)
        assertEquals(0, stats.youtube.musicVideos.totalDurationSeconds)

        val json = stats.toJson()
        val parsed = DailyStats.Serializer.fromJson(org.json.JSONObject(json.toString()))
        assertEquals(0, parsed.youtube.musicVideos.totalWatched)
    }

    @Test
    fun testAppConfigKeywordsAndLocalPrefs() {
        val config = AppConfig(
            masterGoal = "Phát triển bản thân",
            targetKeywords = listOf("code", "kotlin"),
            leisureKeywords = listOf("lofi"),
            distractionKeywords = listOf("drama"),
            localPreferences = com.example.social_media_tracker_app.data.model.LocalPreferences(
                hudPositionX = 30,
                hudPositionY = 90,
                grayscaleMode = "bedtime"
            )
        )
        val jsonStr = config.toJson()
        val jsonObj = org.json.JSONObject(jsonStr)

        assertTrue(jsonObj.has("keywords"))
        val kObj = jsonObj.getJSONObject("keywords")
        assertEquals("code", kObj.getJSONArray("target").getString(0))
        assertEquals("lofi", kObj.getJSONArray("leisure").getString(0))
        assertEquals("drama", kObj.getJSONArray("distraction").getString(0))

        assertTrue(jsonObj.has("localPreferences"))
        val parsed = AppConfig.fromJson(jsonStr)
        assertEquals(listOf("code", "kotlin"), parsed.targetKeywords)
        assertEquals(listOf("lofi"), parsed.leisureKeywords)
        assertEquals(listOf("drama"), parsed.distractionKeywords)
        assertEquals(30, parsed.localPreferences.hudPositionX)
        assertEquals(90, parsed.localPreferences.hudPositionY)
        assertEquals("bedtime", parsed.localPreferences.grayscaleMode)
    }

    @Test
    fun testUserPersonaVulnerabilitiesAndStrengths() {
        val persona = com.example.social_media_tracker_app.data.model.UserPersonaData(
            vulnerabilities = listOf("Dễ lướt khuya"),
            strengths = listOf("Tập trung học tốt")
        )
        val pet = PetState(userPersona = persona)
        val jsonStr = pet.toJson()

        val parsed = PetState.fromJson(jsonStr)
        assertEquals(listOf("Dễ lướt khuya"), parsed.userPersona.vulnerabilities)
        assertEquals(listOf("Tập trung học tốt"), parsed.userPersona.strengths)
    }
}
