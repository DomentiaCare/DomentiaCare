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

    // Step 3: 간단한 테스트용 GET 엔드포인트 추가
    @GET("api/schedules/my")
    suspend fun testGetMySchedules(
        @Header("Authorization") token: String
    ): Response<List<ScheduleResponse>>

    // 인증 없이 테스트할 수 있는 엔드포인트 (있다면)
    @GET("actuator/health")
    suspend fun healthCheck(): Response<Map<String, Any>>
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

    // Step 2: 토큰을 직접 설정 (테스트용)
    private fun getAuthToken(context: android.content.Context): String {
        // TODO: Swagger에서 사용한 실제 JWT 토큰으로 교체
        return "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaXBvazMxQGtha2FvLmNvbSIsInVzZXJJZCI6NiwiaWF0IjoxNzQ4MTU4NzgyLCJleHAiOjE3NDgyNDUxODJ9.IStU2OcpGycuPHqlcr1OeWVS3LuO2aANsL91c7Ucdp8" // 여기에 Swagger 토큰 붙여넣기
    }

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

            // Step 2: 원시 응답 내용 확인
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

                // HTML 응답인지 확인
                if (errorBody?.startsWith("<!DOCTYPE") == true || errorBody?.startsWith("<html") == true) {
                    Log.e("DEBUG_STEP2", "❌ HTML 페이지가 반환됨 (서버 오류 페이지)")
                    throw Exception("서버에서 오류 페이지를 반환했습니다. HTTP ${response.code()}")
                }

                // 빈 응답인지 확인
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
}