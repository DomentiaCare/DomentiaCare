// data/local/RecordStorage.kt
package com.example.domentiacare.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.domentiacare.data.model.Record
import com.example.domentiacare.data.model.ProcessStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordStorage(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("records", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Record 저장
    suspend fun saveRecord(record: Record): Result<Record> = withContext(Dispatchers.IO) {
        try {
            val records = getAllRecords().toMutableList()
            val existingIndex = records.indexOfFirst { it.localId == record.localId }

            if (existingIndex != -1) {
                // 기존 레코드 업데이트
                records[existingIndex] = record.copy(updatedAt = System.currentTimeMillis())
            } else {
                // 새 레코드 추가
                records.add(record)
            }

            val json = gson.toJson(records)
            prefs.edit().putString("records_list", json).apply()

            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 모든 Record 조회
    suspend fun getAllRecords(): List<Record> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString("records_list", "[]") ?: "[]"
            val type = object : TypeToken<List<Record>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Record ID로 조회
    suspend fun getRecordById(localId: String): Record? = withContext(Dispatchers.IO) {
        getAllRecords().find { it.localId == localId }
    }

    // 처리 상태별 Record 조회
    suspend fun getRecordsByStatus(
        transcriptStatus: ProcessStatus? = null,
        analysisStatus: ProcessStatus? = null,
        parseStatus: ProcessStatus? = null
    ): List<Record> = withContext(Dispatchers.IO) {
        getAllRecords().filter { record ->
            (transcriptStatus == null || record.transcriptStatus == transcriptStatus) &&
                    (analysisStatus == null || record.analysisStatus == analysisStatus) &&
                    (parseStatus == null || record.parseStatus == parseStatus)
        }
    }

    // Transcript 업데이트
    suspend fun updateTranscript(localId: String, transcript: String, status: ProcessStatus) = withContext(Dispatchers.IO) {
        updateRecord(localId) { record ->
            record.copy(
                transcript = transcript,
                transcriptStatus = status,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // Llama 결과 업데이트
    suspend fun updateAnalysisResult(localId: String, result: String, status: ProcessStatus) = withContext(Dispatchers.IO) {
        updateRecord(localId) { record ->
            record.copy(
                result = result,
                analysisStatus = status,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // 추출된 일정 업데이트
    suspend fun updateExtractedSchedules(localId: String, schedules: List<SimpleSchedule>, status: ProcessStatus) = withContext(Dispatchers.IO) {
        updateRecord(localId) { record ->
            record.copy(
                extractedSchedules = schedules,
                parseStatus = status,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // Record 업데이트 헬퍼 함수
    private suspend fun updateRecord(localId: String, updateFunction: (Record) -> Record) {
        try {
            val records = getAllRecords().toMutableList()
            val index = records.indexOfFirst { it.localId == localId }
            if (index != -1) {
                records[index] = updateFunction(records[index])
                val json = gson.toJson(records)
                prefs.edit().putString("records_list", json).apply()
            }
        } catch (e: Exception) {
            // 로그만 출력
        }
    }

    // 처리 상태 업데이트만
    suspend fun updateProcessStatus(
        localId: String,
        transcriptStatus: ProcessStatus? = null,
        analysisStatus: ProcessStatus? = null,
        parseStatus: ProcessStatus? = null
    ) = withContext(Dispatchers.IO) {
        updateRecord(localId) { record ->
            record.copy(
                transcriptStatus = transcriptStatus ?: record.transcriptStatus,
                analysisStatus = analysisStatus ?: record.analysisStatus,
                parseStatus = parseStatus ?: record.parseStatus,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // 모든 추출된 일정들 조회 (모든 Record에서)
    suspend fun getAllExtractedSchedules(): List<SimpleSchedule> = withContext(Dispatchers.IO) {
        getAllRecords()
            .mapNotNull { it.extractedSchedules }
            .flatten()
    }
}