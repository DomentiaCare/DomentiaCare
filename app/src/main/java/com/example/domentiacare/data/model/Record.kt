// data/model/Record.kt
package com.example.domentiacare.data.model

import com.example.domentiacare.data.local.SimpleSchedule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 통화 녹음 + AI 처리 결과를 통합한 레코드
data class Record(
    val localId: String,
    val userId: Long,

    // 기존 RecordingFile 정보
    val name: String,
    val path: String,
    val lastModified: Long,
    val size: Long,

    // AI 처리 단계별 결과
    val transcript: String? = null,        // Whisper 변환 완료된 데이터
    val result: String? = null,           // Llama 답변 (원본)
    val extractedSchedules: List<SimpleSchedule>? = null, // 파싱된 일정들 (SimpleSchedule 형식)

    // 처리 상태
    val transcriptStatus: ProcessStatus = ProcessStatus.PENDING,
    val analysisStatus: ProcessStatus = ProcessStatus.PENDING,
    val parseStatus: ProcessStatus = ProcessStatus.PENDING,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// AI 처리 상태
enum class ProcessStatus {
    PENDING,     // 대기중
    PROCESSING,  // 처리중
    COMPLETED,   // 완료
    FAILED       // 실패
}

// Llama 파싱 후 일정 데이터를 SimpleSchedule로 직접 변환
// CallDetailScreen의 parseLlamaScheduleResponseFull과 호환되도록 수정
fun parseLlamaScheduleResponseFull(
    llamaResult: String,
    recordLocalId: String,
    userId: Long
): List<com.example.domentiacare.data.local.SimpleSchedule> {
    return try {
        // CallDetailScreen과 동일한 파싱 로직 사용
        // Summary와 Schedule 형식으로 된 응답을 파싱
        val summaryRegex = Regex("""Summary:\s*(.+)""")
        val scheduleRegex = Regex("""Schedule:\s*(\{[\s\S]*\})""")

        val summary = summaryRegex.find(llamaResult)?.groupValues?.get(1)?.trim() ?: ""
        val jsonString = scheduleRegex.find(llamaResult)?.groupValues?.get(1)?.trim() ?: "{}"

        // JSON 배열 형태인 경우도 처리
        val schedules = if (llamaResult.trim().startsWith("[")) {
            // JSON 배열 형태
            val gson = Gson()
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val scheduleList: List<Map<String, String>> = gson.fromJson(llamaResult, type) ?: emptyList()

            scheduleList.map { scheduleMap ->
                com.example.domentiacare.data.local.SimpleSchedule(
                    localId = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    title = scheduleMap["title"] ?: "제목 없음",
                    description = "${scheduleMap["description"] ?: ""} (통화에서 추출: $recordLocalId)",
                    startDate = scheduleMap["startDate"] ?: "",
                    endDate = scheduleMap["endDate"] ?: "",
                    isAi = true,
                    syncStatus = "LOCAL_ONLY",
                    createdAt = System.currentTimeMillis()
                )
            }
        } else {
            // Summary + Schedule 형태 (CallDetailScreen 방식)
            val json = try {
                org.json.JSONObject(jsonString)
            } catch (e: Exception) {
                org.json.JSONObject()
            }

            val dateRaw = json.optString("date").trim()
            val timeRaw = json.optString("time").trim()
            val place = json.optString("place").trim()

            // 하나의 일정만 추출
            listOf(
                com.example.domentiacare.data.local.SimpleSchedule(
                    localId = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    title = summary.ifBlank { "통화 일정" },
                    description = "통화에서 추출된 일정${if (place.isNotEmpty()) " - 장소: $place" else ""}",
                    startDate = "${dateRaw}T${timeRaw}:00.000Z",
                    endDate = "${dateRaw}T${timeRaw}:00.000Z", // 종료 시간은 시작과 동일하게 설정
                    isAi = true,
                    syncStatus = "LOCAL_ONLY",
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        schedules
    } catch (e: Exception) {
        emptyList()
    }
}

// RecordingFile에서 Record 생성 헬퍼 함수
fun RecordingFile.toRecord(userId: Long): Record {
    return Record(
        localId = java.util.UUID.randomUUID().toString(),
        userId = userId,
        name = this.name,
        path = this.path,
        lastModified = this.lastModified,
        size = this.size
    )
}