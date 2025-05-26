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
fun parseLlamaScheduleResponseFull(
    llamaResult: String,
    recordLocalId: String,
    userId: Long
): List<com.example.domentiacare.data.local.SimpleSchedule> {
    return try {
        // 실제 구현에서는 Llama 응답 형식에 맞게 파싱
        // 예시: JSON 형태의 응답을 파싱
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
    } catch (e: Exception) {
        emptyList()
    }
}