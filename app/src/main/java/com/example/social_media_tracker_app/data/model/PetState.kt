package com.example.social_media_tracker_app.data.model

import org.json.JSONArray
import org.json.JSONObject

data class PetAccessories(
    val unlockedItems: List<String> = emptyList(), // e.g. ["sunglasses", "laurel"]
    val equippedHead: String? = null // e.g. "sunglasses" or "laurel"
)

data class PuppetPhotos(
    val happyImage: String = "",
    val neutralImage: String = "",
    val sadImage: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("happyImage", happyImage)
        put("neutralImage", neutralImage)
        put("sadImage", sadImage)
    }

    companion object {
        fun fromJson(obj: JSONObject?): PuppetPhotos {
            if (obj == null) return PuppetPhotos()
            return PuppetPhotos(
                happyImage = obj.optString("happyImage", ""),
                neutralImage = obj.optString("neutralImage", ""),
                sadImage = obj.optString("sadImage", "")
            )
        }
    }
}

data class AiSprites(
    val happy: String = "",
    val neutral: String = "",
    val sad: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("happy", happy)
        put("neutral", neutral)
        put("sad", sad)
    }

    companion object {
        fun fromJson(obj: JSONObject?): AiSprites {
            if (obj == null) return AiSprites()
            return AiSprites(
                happy = obj.optString("happy", ""),
                neutral = obj.optString("neutral", ""),
                sad = obj.optString("sad", "")
            )
        }
    }
}

data class PersonaDayHistory(
    val date: String = "",
    val usefulPct: Int = 0,
    val activeMinutes: Int = 0,
    val tomorrowMission: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("usefulPct", usefulPct)
        put("activeMinutes", activeMinutes)
        put("tomorrowMission", tomorrowMission)
    }

    companion object {
        fun fromJson(obj: JSONObject?): PersonaDayHistory {
            if (obj == null) return PersonaDayHistory()
            return PersonaDayHistory(
                date = obj.optString("date", ""),
                usefulPct = obj.optInt("usefulPct", 0),
                activeMinutes = obj.optInt("activeMinutes", 0),
                tomorrowMission = obj.optString("tomorrowMission", "")
            )
        }
    }
}

data class UserPersonaData(
    val totalTrackedDays: Int = 0,
    val streakRecord: Int = 0,
    val totalUsefulVideos: Int = 0,
    val avgUsefulPct: Int = 0,
    val vulnerabilities: List<String> = DEFAULT_VULNERABILITIES,
    val strengths: List<String> = DEFAULT_STRENGTHS,
    val history: List<PersonaDayHistory> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("totalTrackedDays", totalTrackedDays)
        put("streakRecord", streakRecord)
        put("totalUsefulVideos", totalUsefulVideos)
        put("avgUsefulPct", avgUsefulPct)
        val vulnArr = JSONArray()
        vulnerabilities.forEach { vulnArr.put(it) }
        put("vulnerabilities", vulnArr)
        val strArr = JSONArray()
        strengths.forEach { strArr.put(it) }
        put("strengths", strArr)
        val arr = JSONArray()
        history.forEach { arr.put(it.toJson()) }
        put("history", arr)
    }

    companion object {
        val DEFAULT_VULNERABILITIES = listOf("Dễ lướt vô thức sau 21h30", "Hay F5 khi gặp bài khó")
        val DEFAULT_STRENGTHS = listOf("Xem trọn vẹn video học tập dài trên 10 phút")

        fun fromJson(obj: JSONObject?): UserPersonaData {
            if (obj == null) return UserPersonaData()
            val histList = mutableListOf<PersonaDayHistory>()
            val arr = obj.optJSONArray("history")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)
                    if (item != null) histList.add(PersonaDayHistory.fromJson(item))
                }
            }
            val vulnList = mutableListOf<String>()
            val vulnArr = obj.optJSONArray("vulnerabilities")
            if (vulnArr != null) {
                for (i in 0 until vulnArr.length()) vulnList.add(vulnArr.getString(i))
            } else {
                vulnList.addAll(DEFAULT_VULNERABILITIES)
            }
            val strengthList = mutableListOf<String>()
            val strArr = obj.optJSONArray("strengths")
            if (strArr != null) {
                for (i in 0 until strArr.length()) strengthList.add(strArr.getString(i))
            } else {
                strengthList.addAll(DEFAULT_STRENGTHS)
            }
            return UserPersonaData(
                totalTrackedDays = obj.optInt("totalTrackedDays", 0),
                streakRecord = obj.optInt("streakRecord", 0),
                totalUsefulVideos = obj.optInt("totalUsefulVideos", 0),
                avgUsefulPct = obj.optInt("avgUsefulPct", 0),
                vulnerabilities = vulnList,
                strengths = strengthList,
                history = histList
            )
        }
    }
}

data class PetState(
    val energy: Int = 100, // 0 - 100
    val mood: String = "happy", // "happy" (>=70) | "neutral" (40-69) | "sad" (<40)
    val currentStreak: Int = 1,
    val streakDays: Int = 1,
    val lastPokeEnergyTime: Long = 0L,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val accessories: PetAccessories = PetAccessories(),
    val mode: String = "default", // "default" | "puppet" | "ai_generated"
    val puppetPhotos: PuppetPhotos = PuppetPhotos(),
    val aiSprites: AiSprites = AiSprites(),
    val userPersona: UserPersonaData = UserPersonaData()
) {
    val isDisappeared: Boolean
        get() = energy <= 0

    fun getActivePuppetImagePath(): String? {
        if (mode != "puppet" || isDisappeared) return null
        val path = when (mood) {
            "happy" -> puppetPhotos.happyImage.ifEmpty { puppetPhotos.neutralImage.ifEmpty { puppetPhotos.sadImage } }
            "neutral" -> puppetPhotos.neutralImage.ifEmpty { puppetPhotos.happyImage.ifEmpty { puppetPhotos.sadImage } }
            else -> puppetPhotos.sadImage.ifEmpty { puppetPhotos.neutralImage.ifEmpty { puppetPhotos.happyImage } }
        }
        return path.ifEmpty { null }
    }

    fun getActiveAiSprite(): String? {
        if (mode != "ai_generated" || isDisappeared) return null
        val sprite = when (mood) {
            "happy" -> aiSprites.happy.ifEmpty { aiSprites.neutral.ifEmpty { aiSprites.sad } }
            "neutral" -> aiSprites.neutral.ifEmpty { aiSprites.happy.ifEmpty { aiSprites.sad } }
            else -> aiSprites.sad.ifEmpty { aiSprites.neutral.ifEmpty { aiSprites.happy } }
        }
        return sprite.ifEmpty { null }
    }

    companion object {
        fun calculateMood(energy: Int): String {
            return when {
                energy >= 70 -> "happy"
                energy >= 40 -> "neutral"
                else -> "sad"
            }
        }

        fun fromJson(jsonStr: String): PetState {
            return try {
                val obj = JSONObject(jsonStr)
                val unlocked = mutableListOf<String>()
                var equippedHead: String? = null

                if (obj.has("accessories")) {
                    val acc = obj.getJSONObject("accessories")
                    if (acc.has("unlockedItems")) {
                        val arr = acc.getJSONArray("unlockedItems")
                        for (i in 0 until arr.length()) unlocked.add(arr.getString(i))
                    }
                    if (acc.has("equippedHead") && !acc.isNull("equippedHead")) {
                        equippedHead = acc.getString("equippedHead")
                    }
                }

                val streak = obj.optInt("streakDays", obj.optInt("currentStreak", 1))
                val energy = obj.optInt("energy", 100).coerceIn(0, 100)
                val puppetPhotos = PuppetPhotos.fromJson(obj.optJSONObject("puppetPhotos"))
                val aiSprites = AiSprites.fromJson(obj.optJSONObject("aiSprites"))
                val userPersona = UserPersonaData.fromJson(obj.optJSONObject("userPersona"))

                PetState(
                    energy = energy,
                    mood = calculateMood(energy),
                    currentStreak = streak,
                    streakDays = streak,
                    lastPokeEnergyTime = obj.optLong("lastPokeEnergyTime", 0L),
                    lastActiveTimestamp = obj.optLong("lastActiveTimestamp", System.currentTimeMillis()),
                    accessories = PetAccessories(unlockedItems = unlocked, equippedHead = equippedHead),
                    mode = obj.optString("mode", "default"),
                    puppetPhotos = puppetPhotos,
                    aiSprites = aiSprites,
                    userPersona = userPersona
                )
            } catch (e: Exception) {
                PetState()
            }
        }
    }

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("energy", energy)
        obj.put("mood", mood)
        obj.put("currentStreak", currentStreak)
        obj.put("streakDays", streakDays)
        obj.put("lastPokeEnergyTime", lastPokeEnergyTime)
        obj.put("lastActiveTimestamp", lastActiveTimestamp)
        obj.put("mode", mode)
        obj.put("puppetPhotos", puppetPhotos.toJson())
        obj.put("aiSprites", aiSprites.toJson())
        obj.put("userPersona", userPersona.toJson())

        val accObj = JSONObject()
        val unlockedArr = JSONArray()
        accessories.unlockedItems.forEach { unlockedArr.put(it) }
        accObj.put("unlockedItems", unlockedArr)
        if (accessories.equippedHead != null) {
            accObj.put("equippedHead", accessories.equippedHead)
        } else {
            accObj.put("equippedHead", JSONObject.NULL)
        }
        obj.put("accessories", accObj)

        return obj.toString()
    }
}
