package com.example.domentiacare.network

import android.content.Context
import android.util.Log
import com.example.domentiacare.data.model.Record
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.data.remote.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 서버 요청용 DTO (extractedSchedules 추가)
data class RecordCreateRequest(
    val localId: String,
    val userId: Long,
    val fileName: String,
    val fileSize: Long?,
    val originalTimestamp: String?,
    val transcript: String?,
    val llamaResult: String?,
    val transcriptStatus: String,
    val analysisStatus: String,
    val parseStatus: String,
    val extractedSchedules: String? = null // JSON 문자열로 전송
)

data class RecordUpdateRequest(
    val transcript: String?,
    val llamaResult: String?,
    val transcriptStatus: String?,
    val analysisStatus: String?,
    val parseStatus: String?,
    val extractedSchedules: String? = null // JSON 문자열로 전송
)

data class RecordResponse(
    val id: Long,
    val localId: String,
    val userId: Long,
    val fileName: String,
    val fileSize: Long?,
    val transcript: String?,
    val llamaResult: String?,
    val transcriptStatus: String,
    val analysisStatus: String,
    val parseStatus: String,
    val createdAt: String,
    val updatedAt: String,
    val extractedSchedules: String? = null // JSON 문자열로 수신
)

// Retrofit 인터페이스
interface RecordApiInterface {
    @POST("api/records")
    suspend fun createRecord(@Body request: RecordCreateRequest): Response<RecordResponse>

    @PUT("api/records/{localId}")
    suspend fun updateRecord(
        @Path("localId") localId: String,
        @Body request: RecordUpdateRequest
    ): Response<RecordResponse>

    @GET("api/records/{localId}")
    suspend fun getRecord(@Path("localId") localId: String): Response<RecordResponse>

    @GET("api/records/user/{userId}")
    suspend fun getRecordsByUser(@Path("userId") userId: Long): Response<List<RecordResponse>>
}

// API 서비스
object RecordApiService {
    private val api: RecordApiInterface by lazy {
        RetrofitClient.createService(RecordApiInterface::class.java)
    }

    private val gson = Gson()

    /**
     * Record 생성
     */
    suspend fun createRecord(record: Record, context: Context): Result<RecordResponse> {
        return try {
            val request = record.toCreateRequest()
            val response = api.createRecord(request)

            if (response.isSuccessful) {
                val result = response.body()!!
                Log.d("RecordApiService", "✅ Record 생성 성공: ${result.localId}")
                Result.success(result)
            } else {
                Log.e("RecordApiService", "❌ Record 생성 실패: ${response.code()}")
                Result.failure(Exception("Record 생성 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RecordApiService", "❌ Record 생성 예외", e)
            Result.failure(e)
        }
    }

    /**
     * Record 업데이트
     */
    suspend fun updateRecord(record: Record, context: Context): Result<RecordResponse> {
        return try {
            val request = record.toUpdateRequest()
            val response = api.updateRecord(record.localId, request)

            if (response.isSuccessful) {
                val result = response.body()!!
                Log.d("RecordApiService", "✅ Record 업데이트 성공: ${result.localId}")
                Result.success(result)
            } else {
                Log.e("RecordApiService", "❌ Record 업데이트 실패: ${response.code()}")
                Result.failure(Exception("Record 업데이트 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RecordApiService", "❌ Record 업데이트 예외", e)
            Result.failure(e)
        }
    }

    /**
     * Record 조회
     */
    suspend fun getRecord(localId: String, context: Context): Result<RecordResponse> {
        return try {
            val response = api.getRecord(localId)

            if (response.isSuccessful) {
                val result = response.body()!!
                Log.d("RecordApiService", "✅ Record 조회 성공: ${result.localId}")
                Result.success(result)
            } else {
                Log.e("RecordApiService", "❌ Record 조회 실패: ${response.code()}")
                Result.failure(Exception("Record 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RecordApiService", "❌ Record 조회 예외", e)
            Result.failure(e)
        }
    }
}

// Extension functions for conversion
private fun Record.toCreateRequest(): RecordCreateRequest {
    return RecordCreateRequest(
        localId = this.localId,
        userId = this.userId,
        fileName = this.name,
        fileSize = this.size,
        originalTimestamp = formatTimestamp(this.lastModified),
        transcript = this.transcript,
        llamaResult = this.result,
        transcriptStatus = this.transcriptStatus.name,
        analysisStatus = this.analysisStatus.name,
        parseStatus = this.parseStatus.name,
        extractedSchedules = this.extractedSchedules?.let { schedules ->
            // SimpleSchedule 리스트를 JSON 문자열로 변환
            Gson().toJson(schedules.map { schedule ->
                mapOf(
                    "localId" to schedule.localId,
                    "userId" to schedule.userId,
                    "title" to schedule.title,
                    "description" to schedule.description,
                    "startDate" to schedule.startDate,
                    "endDate" to schedule.endDate,
                    "isAi" to schedule.isAi,
                    "syncStatus" to schedule.syncStatus,
                    "createdAt" to schedule.createdAt
                )
            })
        }
    )
}

private fun Record.toUpdateRequest(): RecordUpdateRequest {
    return RecordUpdateRequest(
        transcript = this.transcript,
        llamaResult = this.result,
        transcriptStatus = this.transcriptStatus.name,
        analysisStatus = this.analysisStatus.name,
        parseStatus = this.parseStatus.name,
        extractedSchedules = this.extractedSchedules?.let { schedules ->
            // SimpleSchedule 리스트를 JSON 문자열로 변환
            Gson().toJson(schedules.map { schedule ->
                mapOf(
                    "localId" to schedule.localId,
                    "userId" to schedule.userId,
                    "title" to schedule.title,
                    "description" to schedule.description,
                    "startDate" to schedule.startDate,
                    "endDate" to schedule.endDate,
                    "isAi" to schedule.isAi,
                    "syncStatus" to schedule.syncStatus,
                    "createdAt" to schedule.createdAt
                )
            })
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (e: Exception) {
        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}