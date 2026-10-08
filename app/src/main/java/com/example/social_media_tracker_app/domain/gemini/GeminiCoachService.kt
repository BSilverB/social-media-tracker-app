package com.example.social_media_tracker_app.domain.gemini

import android.util.Log
import com.example.social_media_tracker_app.data.model.DailyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GeminiCoachResult(
    val isSuccess: Boolean,
    val coachFeedback: String,
    val tomorrowMission: String,
    val reclassifiedVideos: Map<String, String> = emptyMap(), // videoId or title -> "goal" | "leisure" | "distraction"
    val learnedTargetKeywords: List<String> = emptyList(),
    val learnedLeisureKeywords: List<String> = emptyList(),
    val learnedDistractionKeywords: List<String> = emptyList()
)

object GeminiCoachService {

    private const val TAG = "GeminiCoachService"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    suspend fun getReflectionFeedback(
        stats: DailyStats,
        masterGoal: String,
        lesson1: String,
        lesson2: String,
        apiKey: String,
        unclassifiedVideos: List<com.example.social_media_tracker_app.data.model.WatchedVideoItem> = emptyList(),
        existingTargetKeywords: List<String> = emptyList()
    ): GeminiCoachResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            // Local empathetic mindfulness coach response
            return@withContext GeminiCoachResult(
                isSuccess = true,
                coachFeedback = "Hôm nay bạn đã thực hành chánh niệm với ${stats.totalSwipes} lượt lướt (${stats.deepViewRatioPercentage}% xem sâu, " +
                        "${stats.goalRatioPercentage}% nội dung đúng mục tiêu). " +
                        "Hãy luôn nhớ mục tiêu lớn của bạn: \"$masterGoal\". Mỗi bước tiến chậm rãi đều giúp bạn làm chủ công nghệ thay vì để nó chi phối!",
                tomorrowMission = if (lesson2.isNotBlank()) "Tập trung cải thiện: $lesson2" else "Thực hiện 1 chu kỳ thở Box Breathing 12s trước khi mở mạng xã hội."
            )
        }

        try {
            val url = URL("$API_URL?key=${apiKey.trim()}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 20000
            conn.readTimeout = 25000
            conn.doOutput = true

            val sampleTitles = stats.watchedVideos.take(4).map { "• ${it.title} [${it.category}]" }.joinToString("\n")

            // Format unclassified videos for Gemini batch analysis
            val unclassifiedSection = if (unclassifiedVideos.isNotEmpty()) {
                val list = unclassifiedVideos.take(15).mapIndexed { idx, it ->
                    "${idx + 1}. [id: \"${it.id.ifBlank { it.title }}\"] \"${it.title}\" (Kênh: ${it.channel.ifBlank { "Không rõ" }}, Nền tảng: ${it.platform})"
                }.joinToString("\n")
                """
                - Danh sách các video CHƯA RÕ PHÂN LOẠI (hãy đọc hiểu nội dung và phân loại chính xác lại):
                $list
                """.trimIndent()
            } else {
                "- Hôm nay không có video nào chưa rõ phân loại."
            }

            val promptText = """
                Bạn là Trợ lý AI Phản tư (Mindful Coach) kiêm Chuyên gia Phân tích Nội dung Chánh niệm.
                
                Dữ liệu tiêu thụ nội dung hôm nay của người dùng:
                - Tổng lượt lướt: ${stats.totalSwipes} lượt
                - Video xem sâu (>=2s): ${stats.validViews} lượt
                - Thời gian hoạt động: ${stats.formattedActiveTime}
                - Tỉ lệ chú tâm: ${stats.deepViewRatioPercentage}%
                - Phân loại hiện tại: ${stats.goalVideosCount} Mục tiêu, ${stats.leisureVideosCount} Giải trí, ${stats.distractionVideosCount} Bẫy Dopamine, ${stats.unclassifiedVideosCount} Chưa rõ.
                - Một số video đã nhận diện:
                $sampleTitles
                
                $unclassifiedSection
                
                - Mục tiêu cốt lõi của người dùng: "$masterGoal"
                - Từ khóa mục tiêu hiện có: ${existingTargetKeywords.joinToString(", ")}
                - Điều tâm đắc học được hôm nay: "$lesson1"
                - Điều muốn cải thiện: "$lesson2"

                NHIỆM VỤ CỦA BẠN:
                1. Đưa ra phản hồi ngắn gọn (dưới 100 từ), thấu cảm, đánh giá xem ngày hôm nay người dùng có đang bám sát mục tiêu lớn không và giao 1 nhiệm vụ nhỏ cụ thể cho ngày mai.
                2. Nếu có danh sách video CHƯA RÕ PHÂN LOẠI ở trên, hãy đọc hiểu tiêu đề/kênh và phân loại từng video vào 1 trong 3 nhóm: "goal", "leisure", hoặc "distraction".
                3. Đề xuất từ 1 đến 4 TỪ KHÓA MỚI (hoặc tên kênh/thuật ngữ tiêu biểu) được phát hiện từ các video trên để cập nhật vào bộ lọc máy cho ngày mai:
                   - "keywords": danh sách các từ khóa mới phát hiện, mỗi từ gồm:
                     * "word": từ khóa bằng chữ thường
                     * "category": "target" (nội dung phục vụ mục tiêu), "leisure" (giải trí lành mạnh), hoặc "distraction" (bẫy dopamine / drama / hài nhảm)
                     * "confidence": độ tin cậy từ 50 đến 100
                     * "reason": lý do ngắn gọn dưới 12 từ

                Định dạng trả về JSON bắt buộc:
                {
                  "coachFeedback": "Lời nhận xét và phân tích...",
                  "tomorrowMission": "1 hành động nhỏ cụ thể cho ngày mai...",
                  "reclassifiedVideos": [
                    {"id": "id_hoac_title_video", "category": "goal|leisure|distraction"}
                  ],
                  "keywords": [
                    {
                      "word": "từ khóa 1",
                      "category": "target|leisure|distraction",
                      "confidence": 85,
                      "reason": "Lý do ngắn gọn"
                    }
                  ]
                }
            """.trimIndent()

            val contentsArr = JSONArray().apply {
                val partsArr = JSONArray().apply {
                    put(JSONObject().apply { put("text", promptText) })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", partsArr)
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArr)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                    // Clean json
                    val cleanText = text.replace("```json", "").replace("```", "").trim()
                    val parsed = JSONObject(cleanText)

                    val reclassifiedMap = mutableMapOf<String, String>()
                    if (parsed.has("reclassifiedVideos")) {
                        val arr = parsed.getJSONArray("reclassifiedVideos")
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val id = item.optString("id", "")
                            val cat = item.optString("category", "leisure").lowercase()
                            if (id.isNotBlank() && cat in listOf("goal", "leisure", "distraction")) {
                                reclassifiedMap[id] = cat
                            }
                        }
                    }

                    val learnedTarget = mutableListOf<String>()
                    val learnedLeisure = mutableListOf<String>()
                    val learnedDistraction = mutableListOf<String>()

                    // 1. Chuẩn hóa mới: Đọc từ mảng keywords chung với category
                    if (parsed.has("keywords")) {
                        val arr = parsed.getJSONArray("keywords")
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val kw = item.optString("word", "").trim().lowercase()
                            var cat = item.optString("category", "target").trim().lowercase()
                            if (cat == "goal") cat = "target"
                            if (kw.isNotBlank()) {
                                when (cat) {
                                    "target" -> if (!learnedTarget.contains(kw)) learnedTarget.add(kw)
                                    "leisure" -> if (!learnedLeisure.contains(kw)) learnedLeisure.add(kw)
                                    "distraction" -> if (!learnedDistraction.contains(kw)) learnedDistraction.add(kw)
                                    else -> if (!learnedTarget.contains(kw)) learnedTarget.add(kw)
                                }
                            }
                        }
                    }

                    // 2. Fallback cấu trúc cũ (nếu AI trả về 3 mảng riêng lẻ)
                    if (parsed.has("learnedTargetKeywords")) {
                        val arr = parsed.getJSONArray("learnedTargetKeywords")
                        for (i in 0 until arr.length()) {
                            val kw = arr.getString(i).trim().lowercase()
                            if (kw.isNotBlank() && !learnedTarget.contains(kw)) learnedTarget.add(kw)
                        }
                    }

                    if (parsed.has("learnedLeisureKeywords")) {
                        val arr = parsed.getJSONArray("learnedLeisureKeywords")
                        for (i in 0 until arr.length()) {
                            val kw = arr.getString(i).trim().lowercase()
                            if (kw.isNotBlank() && !learnedLeisure.contains(kw)) learnedLeisure.add(kw)
                        }
                    }

                    if (parsed.has("learnedDistractionKeywords")) {
                        val arr = parsed.getJSONArray("learnedDistractionKeywords")
                        for (i in 0 until arr.length()) {
                            val kw = arr.getString(i).trim().lowercase()
                            if (kw.isNotBlank() && !learnedDistraction.contains(kw)) learnedDistraction.add(kw)
                        }
                    }

                    return@withContext GeminiCoachResult(
                        isSuccess = true,
                        coachFeedback = parsed.optString("coachFeedback", "Bạn đang đi đúng hướng trên hành trình làm chủ sự chú ý của mình!"),
                        tomorrowMission = parsed.optString("tomorrowMission", "Giữ vững chánh niệm khi mở ứng dụng ngày mai."),
                        reclassifiedVideos = reclassifiedMap,
                        learnedTargetKeywords = learnedTarget,
                        learnedLeisureKeywords = learnedLeisure,
                        learnedDistractionKeywords = learnedDistraction
                    )
                }
            } else {
                Log.w(TAG, "Gemini API error code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error contacting Gemini API", e)
        }

        // Fallback on error
        return@withContext GeminiCoachResult(
            isSuccess = true,
            coachFeedback = "Hôm nay bạn đã hoàn thành bài phản tư với kết quả đáng khích lệ. Hãy giữ vững sự tập trung cho mục tiêu: \"$masterGoal\"!",
            tomorrowMission = "Thực hiện ít nhất 1 phiên Pomodoro tập trung vào ngày mai."
        )
    }

    suspend fun getLongTermCoachInsight(
        summaryStats: Map<String, Any>,
        periodLabel: String = "tuần này",
        masterGoal: String = "",
        apiKey: String
    ): LongTermCoachResult = withContext(Dispatchers.IO) {
        val totalSwipes = summaryStats["totalSwipes"] ?: 0
        val avgSwipes = summaryStats["avgSwipes"] ?: 0
        val swipeTrend = summaryStats["swipeTrendText"] ?: "Đang đối chiếu"
        val totalReloads = summaryStats["totalReloads"] ?: 0
        val deepWatchPct = summaryStats["deepWatchPct"] ?: 0
        val deepWatchCount = summaryStats["deepWatchCount"] ?: 0
        val impulsiveSkipPct = summaryStats["impulsiveSkipPct"] ?: 0
        val impulsiveSkipCount = summaryStats["impulsiveSkipCount"] ?: 0
        val usefulPct = summaryStats["usefulPct"] ?: 0
        val streakDays = summaryStats["streakDays"] ?: 0
        val temptationResistedPct = summaryStats["temptationResistedPct"] ?: 100
        val avgPetHealth = summaryStats["avgPetHealth"] ?: 100
        val vulnerableWindow = summaryStats["vulnerableWindow"] ?: "Không có khung giờ báo động"

        if (apiKey.isBlank()) {
            return@withContext LongTermCoachResult(
                isSuccess = true,
                coachFeedback = "Trong $periodLabel, bạn đã thực hiện tổng cộng $totalSwipes lượt vuốt và $totalReloads lần làm mới. Tỷ lệ xem sâu đạt $deepWatchPct%, tỷ lệ bám sát mục tiêu đạt $usefulPct%. Bạn đang từng bước lấy lại quyền kiểm soát thời gian số của mình!",
                brightSpot = "Tỷ lệ vượt qua cám dỗ thở Box Breathing đạt $temptationResistedPct% với chuỗi $streakDays ngày kỷ luật.",
                vulnerableWindowAdvice = "Lưu ý khung giờ dễ sa đà ($vulnerableWindow): Hãy chủ động cất điện thoại cách xa giường ngủ.",
                actionSuggestion = "Kích hoạt chế độ thư giãn hoặc Màn hình Đen Trắng trước 22h00."
            )
        }

        try {
            val url = URL("$API_URL?key=${apiKey.trim()}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 20000
            conn.readTimeout = 25000
            conn.doOutput = true

            val promptText = """
                Bạn là Trợ lý AI Phản tư (Mindful Coach) thấu cảm, tinh tế, đồng hành nâng cao kỷ luật bản thân và phục hồi khả năng tập trung sâu.
                Hãy phân tích dữ liệu tổng hợp dài hạn ($periodLabel) của người dùng:

                Số liệu tổng hợp:
                - Tổng số lượt vuốt (Swipes): $totalSwipes lượt (Trung bình $avgSwipes lượt/ngày)
                - So sánh biến thiên swipes: $swipeTrend
                - Tần suất F5 / Reload Home: $totalReloads lần
                - Tỷ lệ xem sâu (>=80% hoặc >3p): $deepWatchPct% ($deepWatchCount video)
                - Tỷ lệ bỏ dở vội (<15%): $impulsiveSkipPct% ($impulsiveSkipCount video)
                - Tỷ lệ nội dung hữu ích bám sát mục tiêu: $usefulPct%
                - Chuỗi ngày Focus Streak hiện tại: $streakDays ngày
                - Tỷ lệ vượt qua cám dỗ (Box Breathing xong đóng app): $temptationResistedPct%
                - Điểm sức khỏe trung bình của Pet: $avgPetHealth⚡
                - Khung giờ điểm mù / dễ mất kiểm soát nhất (Vulnerable Window): $vulnerableWindow
                - Master Goal của người dùng: "${masterGoal.ifBlank { "Trở thành phiên bản tốt hơn" }}"

                Yêu cầu phản hồi:
                - Lời đúc kết sắc sảo nhưng ấm áp, thấu cảm theo phong cách Mindful Coach (dưới 150 từ).
                - Khen ngợi rõ ràng điểm sáng nhất (ví dụ: số video xem trọn vẹn, tỷ lệ vượt qua cám dỗ, hoặc sự sụt giảm của swipes).
                - Thẳng thắn nhưng tinh tế chỉ ra "khung giờ điểm mù" (Vulnerable Window) mà người dùng hay sa đà nhất.
                - Đưa ra 1 giải pháp cụ thể ngay (ví dụ: gợi ý bật Màn hình Đen Trắng, hạ bớt hạn mức Mốc 3, hoặc hẹn giờ thư giãn).

                Trả về JSON đúng cấu trúc:
                {
                  "coachFeedback": "Lời nhận xét và đúc kết...",
                  "brightSpot": "Điểm sáng lớn nhất...",
                  "vulnerableWindowAdvice": "Lời khuyên cho khung giờ điểm mù...",
                  "actionSuggestion": "Hành động cụ thể đề xuất..."
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                val contentsArr = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray()
                    partsArr.put(JSONObject().apply { put("text", promptText) })
                    put("parts", partsArr)
                }
                contentsArr.put(contentObj)
                put("contents", contentsArr)
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.4)
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val rootJson = JSONObject(responseStr)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val parsed = JSONObject(text.trim())
                    return@withContext LongTermCoachResult(
                        isSuccess = true,
                        coachFeedback = parsed.optString("coachFeedback", "Bạn đang từng bước làm chủ dopamine và sự chú ý!"),
                        brightSpot = parsed.optString("brightSpot", "Duy trì kỷ luật chánh niệm đều đặn."),
                        vulnerableWindowAdvice = parsed.optString("vulnerableWindowAdvice", "Chú ý khung giờ đêm muộn."),
                        actionSuggestion = parsed.optString("actionSuggestion", "Đặt điện thoại xa tầm tay trước giờ đi ngủ.")
                    )
                }
            } else {
                Log.w(TAG, "Gemini API error code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getLongTermCoachInsight", e)
        }

        return@withContext LongTermCoachResult(
            isSuccess = true,
            coachFeedback = "Dữ liệu $periodLabel cho thấy bạn đang dần hình thành sự tự nhận thức về hành vi số. Hãy tiếp tục duy trì đà tiến bộ này!",
            brightSpot = "Duy trì chuỗi ngày kiên định theo dõi thói quen.",
            vulnerableWindowAdvice = "Đặc biệt chú ý khung giờ $vulnerableWindow.",
            actionSuggestion = "Hẹn giờ nhắc nhở rời xa màn hình trước giờ ngủ."
        )
    }
}

data class LongTermCoachResult(
    val isSuccess: Boolean = true,
    val coachFeedback: String = "",
    val brightSpot: String = "",
    val vulnerableWindowAdvice: String = "",
    val actionSuggestion: String = ""
)
