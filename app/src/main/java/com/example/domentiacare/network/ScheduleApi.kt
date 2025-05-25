// network/ScheduleApiService.kt
package com.example.domentiacare.network

import android.util.Log
import com.example.domentiacare.network.dto.ScheduleCreateRequest
import com.example.domentiacare.network.dto.ScheduleResponse
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ScheduleApi {
    @POST("api/schedules")
    suspend fun createSchedule(
        @Body request: ScheduleCreateRequest,
        @Header("Authorization") token: String
    ): Response<ScheduleResponse>

    @GET("api/schedules/my")
    suspend fun getMySchedules(
        @Header("Authorization") token: String
    ): Response<List<ScheduleResponse>>

    @GET("api/schedules/user/{userId}")
    suspend fun getUserSchedules(
        @Path("userId") userId: Long,
        @Header("Authorization") token: String
    ): Response<List<ScheduleResponse>>

    @GET("api/schedules/today/{userId}")
    suspend fun getTodaySchedules(
        @Path("userId") userId: Long,
        @Header("Authorization") token: String
    ): Response<List<ScheduleResponse>>

    @GET("api/schedules/range/{userId}")
    suspend fun getSchedulesByDateRange(
        @Path("userId") userId: Long,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Header("Authorization") token: String
    ): Response<List<ScheduleResponse>>
}

object ScheduleApiService {
    private const val BASE_URL = "http://223.194.158.248:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("HTTP_DEBUG", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val api = retrofit.create(ScheduleApi::class.java)

    // 현재 사용자의 인증 토큰을 가져오는 함수
    private fun getAuthToken(context: android.content.Context): String {
        val token = com.example.domentiacare.data.util.UserPreferences.getAuthToken(context)
        return if (!token.isNullOrBlank()) {
            "Bearer $token"
        } else {
            // TODO: Swagger에서 사용한 실제 JWT 토큰으로 교체
            "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaXBvazMxQGtha2FvLmNvbSIsInVzZXJJZCI6NiwiaWF0IjoxNzQ4MTU4NzgyLCJleHAiOjE3NDgyNDUxODJ9.IStU2OcpGycuPHqlcr1OeWVS3LuO2aANsL91c7Ucdp8"
        }
    }

    /**
     * 일정 생성
     */
    suspend fun createSchedule(request: ScheduleCreateRequest, context: android.content.Context): ScheduleResponse {
        try {
            Log.d("DEBUG_STEP2", "=== API 호출 시작 ===")
            Log.d("DEBUG_STEP2", "URL: ${BASE_URL}api/schedules")
            Log.d("DEBUG_STEP2", "요청 데이터: $request")
            Log.d("DEBUG_STEP2", "인증 토큰: ${getAuthToken(context)}")

            val response = api.createSchedule(request, getAuthToken(context))

            Log.d("DEBUG_STEP2", "=== 응답 정보 ===")
            Log.d("DEBUG_STEP2", "응답 코드: ${response.code()}")
            Log.d("DEBUG_STEP2", "응답 메시지: ${response.message()}")
            Log.d("DEBUG_STEP2", "응답 헤더: ${response.headers()}")

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("DEBUG_STEP2", "성공 응답 본문: $responseBody")
                return responseBody ?: throw Exception("서버에서 빈 응답을 받았습니다.")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("DEBUG_STEP2", "=== 에러 응답 상세 ===")
                Log.e("DEBUG_STEP2", "에러 코드: ${response.code()}")
                Log.e("DEBUG_STEP2", "에러 메시지: ${response.message()}")
                Log.e("DEBUG_STEP2", "에러 본문 (원시): '$errorBody'")
                Log.e("DEBUG_STEP2", "에러 본문 길이: ${errorBody?.length ?: 0}")

                if (errorBody?.startsWith("<!DOCTYPE") == true || errorBody?.startsWith("<html") == true) {
                    Log.e("DEBUG_STEP2", "❌ HTML 페이지가 반환됨 (서버 오류 페이지)")
                    throw Exception("서버에서 오류 페이지를 반환했습니다. HTTP ${response.code()}")
                }

                if (errorBody.isNullOrBlank()) {
                    Log.e("DEBUG_STEP2", "❌ 빈 응답 받음")
                    throw Exception("서버에서 빈 응답을 받았습니다. HTTP ${response.code()}")
                }

                throw retrofit2.HttpException(response)
            }

        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.e("DEBUG_STEP2", "❌ JSON 파싱 오류", e)
            throw Exception("서버 응답을 해석할 수 없습니다: ${e.message}")
        } catch (e: com.google.gson.stream.MalformedJsonException) {
            Log.e("DEBUG_STEP2", "❌ 잘못된 JSON 형식", e)
            throw Exception("서버에서 잘못된 형식의 응답을 받았습니다: ${e.message}")
        } catch (e: java.net.ConnectException) {
            Log.e("DEBUG_STEP2", "❌ 연결 오류", e)
            throw Exception("서버에 연결할 수 없습니다: ${e.message}")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("DEBUG_STEP2", "❌ 타임아웃 오류", e)
            throw Exception("서버 응답 시간이 초과되었습니다: ${e.message}")
        } catch (e: retrofit2.HttpException) {
            Log.e("DEBUG_STEP2", "❌ HTTP 에러", e)
            throw Exception("HTTP 오류: ${e.code()} ${e.message()}")
        } catch (e: Exception) {
            Log.e("DEBUG_STEP2", "❌ 예상치 못한 오류", e)
            throw Exception("예상치 못한 오류: ${e.message}")
        }
    }

    /**
     * 내 일정 조회
     */
    suspend fun getMySchedules(context: android.content.Context): List<ScheduleResponse> {
        return try {
            Log.d("ScheduleAPI", "=== 내 일정 조회 시작 ===")

            val response = api.getMySchedules(getAuthToken(context))

            Log.d("ScheduleAPI", "응답 코드: ${response.code()}")
            Log.d("ScheduleAPI", "응답 메시지: ${response.message()}")

            if (response.isSuccessful) {
                val schedules = response.body() ?: emptyList()
                Log.d("ScheduleAPI", "✅ 내 일정 조회 성공: ${schedules.size}개")

                // 받아온 데이터 전체 로그 출력
                schedules.forEachIndexed { index, schedule ->
                    Log.d("ScheduleAPI", "📅 일정 #${index + 1}:")
                    Log.d("ScheduleAPI", "  - ID: ${schedule.id}")
                    Log.d("ScheduleAPI", "  - 제목: ${schedule.title}")
                    Log.d("ScheduleAPI", "  - 설명: ${schedule.description}")
                    Log.d("ScheduleAPI", "  - 시작일시: ${schedule.startDate}")
                    Log.d("ScheduleAPI", "  - 종료일시: ${schedule.endDate}")
                    Log.d("ScheduleAPI", "  - AI 생성: ${schedule.isAi}")
                    Log.d("ScheduleAPI", "  - 완료 여부: ${schedule.isCompleted}")
                    Log.d("ScheduleAPI", "  - 생성자: ${schedule.creatorName}")
                    Log.d("ScheduleAPI", "  - 대상자: ${schedule.userName}")
                    Log.d("ScheduleAPI", "  - 생성일: ${schedule.createdAt}")
                    Log.d("ScheduleAPI", "  -----------")
                }

                return schedules
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ScheduleAPI", "❌ 내 일정 조회 실패: ${response.code()}")
                Log.e("ScheduleAPI", "에러 본문: $errorBody")
                return emptyList()
            }

        } catch (e: Exception) {
            Log.e("ScheduleAPI", "❌ 내 일정 조회 예외", e)
            return emptyList()
        }
    }

    /**
     * 특정 사용자 일정 조회
     */
    suspend fun getUserSchedules(userId: Long, context: android.content.Context): List<ScheduleResponse> {
        return try {
            Log.d("ScheduleAPI", "=== 사용자($userId) 일정 조회 시작 ===")

            val response = api.getUserSchedules(userId, getAuthToken(context))

            Log.d("ScheduleAPI", "응답 코드: ${response.code()}")

            if (response.isSuccessful) {
                val schedules = response.body() ?: emptyList()
                Log.d("ScheduleAPI", "✅ 사용자 일정 조회 성공: ${schedules.size}개")

                // 받아온 데이터 전체 로그 출력
                schedules.forEachIndexed { index, schedule ->
                    Log.d("ScheduleAPI", "📅 사용자($userId) 일정 #${index + 1}:")
                    Log.d("ScheduleAPI", "  - ID: ${schedule.id}")
                    Log.d("ScheduleAPI", "  - 제목: ${schedule.title}")
                    Log.d("ScheduleAPI", "  - 설명: ${schedule.description}")
                    Log.d("ScheduleAPI", "  - 시작일시: ${schedule.startDate}")
                    Log.d("ScheduleAPI", "  - 종료일시: ${schedule.endDate}")
                    Log.d("ScheduleAPI", "  - AI 생성: ${schedule.isAi}")
                    Log.d("ScheduleAPI", "  - 완료 여부: ${schedule.isCompleted}")
                    Log.d("ScheduleAPI", "  -----------")
                }

                return schedules
            } else {
                Log.e("ScheduleAPI", "❌ 사용자 일정 조회 실패: ${response.code()}")
                return emptyList()
            }

        } catch (e: Exception) {
            Log.e("ScheduleAPI", "❌ 사용자 일정 조회 예외", e)
            return emptyList()
        }
    }

    /**
     * 오늘의 일정 조회
     */
    suspend fun getTodaySchedules(userId: Long, context: android.content.Context): List<ScheduleResponse> {
        return try {
            Log.d("ScheduleAPI", "=== 오늘의 일정 조회 시작 (사용자: $userId) ===")

            val response = api.getTodaySchedules(userId, getAuthToken(context))

            if (response.isSuccessful) {
                val schedules = response.body() ?: emptyList()
                Log.d("ScheduleAPI", "✅ 오늘의 일정 조회 성공: ${schedules.size}개")

                // 오늘의 일정 로그 출력
                if (schedules.isEmpty()) {
                    Log.d("ScheduleAPI", "📅 오늘 예정된 일정이 없습니다.")
                } else {
                    schedules.forEachIndexed { index, schedule ->
                        Log.d("ScheduleAPI", "📅 오늘의 일정 #${index + 1}:")
                        Log.d("ScheduleAPI", "  - 제목: ${schedule.title}")
                        Log.d("ScheduleAPI", "  - 시간: ${schedule.startDate}")
                        Log.d("ScheduleAPI", "  - 설명: ${schedule.description}")
                        Log.d("ScheduleAPI", "  -----------")
                    }
                }

                return schedules
            } else {
                Log.e("ScheduleAPI", "❌ 오늘의 일정 조회 실패: ${response.code()}")
                return emptyList()
            }

        } catch (e: Exception) {
            Log.e("ScheduleAPI", "❌ 오늘의 일정 조회 예외", e)
            return emptyList()
        }
    }

    /**
     * 기간별 일정 조회
     */
    suspend fun getSchedulesByDateRange(
        userId: Long,
        startDate: String,
        endDate: String,
        context: android.content.Context
    ): List<ScheduleResponse> {
        return try {
            Log.d("ScheduleAPI", "=== 기간별 일정 조회 시작 ===")
            Log.d("ScheduleAPI", "사용자: $userId")
            Log.d("ScheduleAPI", "기간: $startDate ~ $endDate")

            val response = api.getSchedulesByDateRange(userId, startDate, endDate, getAuthToken(context))

            if (response.isSuccessful) {
                val schedules = response.body() ?: emptyList()
                Log.d("ScheduleAPI", "✅ 기간별 일정 조회 성공: ${schedules.size}개")

                // 기간별 일정 로그 출력
                schedules.forEachIndexed { index, schedule ->
                    Log.d("ScheduleAPI", "📅 기간별 일정 #${index + 1}:")
                    Log.d("ScheduleAPI", "  - 제목: ${schedule.title}")
                    Log.d("ScheduleAPI", "  - 일시: ${schedule.startDate} ~ ${schedule.endDate}")
                    Log.d("ScheduleAPI", "  - 설명: ${schedule.description}")
                    Log.d("ScheduleAPI", "  -----------")
                }

                return schedules
            } else {
                Log.e("ScheduleAPI", "❌ 기간별 일정 조회 실패: ${response.code()}")
                return emptyList()
            }

        } catch (e: Exception) {
            Log.e("ScheduleAPI", "❌ 기간별 일정 조회 예외", e)
            return emptyList()
        }
    }

    /**
     * 모든 일정 조회 (테스트용 - 여러 API 한번에 호출)
     */
    suspend fun getAllSchedulesWithLog(userId: Long, context: android.content.Context) {
        Log.d("ScheduleAPI", "🔍 === 전체 일정 조회 테스트 시작 ===")

        // 1. 내 일정 조회
        Log.d("ScheduleAPI", "\n1️⃣ 내 일정 조회:")
        getMySchedules(context)

        // 2. 특정 사용자 일정 조회
        Log.d("ScheduleAPI", "\n2️⃣ 사용자($userId) 일정 조회:")
        getUserSchedules(userId, context)

        // 3. 오늘의 일정 조회
        Log.d("ScheduleAPI", "\n3️⃣ 오늘의 일정 조회:")
        getTodaySchedules(userId, context)

        // 4. 이번 주 일정 조회 (예시)
        val today = java.time.LocalDateTime.now()
        val startOfWeek = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val endOfWeek = today.plusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        Log.d("ScheduleAPI", "\n4️⃣ 이번 주 일정 조회:")
        getSchedulesByDateRange(userId, startOfWeek, endOfWeek, context)

        Log.d("ScheduleAPI", "🔍 === 전체 일정 조회 테스트 완료 ===")
    }
}