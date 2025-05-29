package com.example.domentiacare.data.remote.api

import com.example.domentiacare.data.local.schedule.Schedule
import com.example.domentiacare.data.local.schedule.ScheduleDto
import com.example.domentiacare.data.remote.dto.KakaoLoginResponse
import com.example.domentiacare.data.remote.dto.KakaoTokenRequest
import com.example.domentiacare.data.remote.dto.LocationRequestBody
import com.example.domentiacare.data.remote.dto.Patient
import com.example.domentiacare.data.remote.dto.Phone
import com.example.domentiacare.data.remote.dto.RegisterUserRequest
import com.example.domentiacare.data.remote.dto.User
import com.example.domentiacare.network.RecordResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {  //  /auth/** 는 백엔드에서 jwt토큰 없이도 접근할 수 있는 api 백엔드의 SecurityConfig를 참고하면됨
    //kakao에서 받은 token을 백엔드로 보냄
    @POST("/auth/kakao")
    fun kakaoLogin(
        @Body tokenRequest: KakaoTokenRequest  //body에 넣어서 보냄
    ): Call<KakaoLoginResponse>  //반환 타입

    //jwt를 이용해서 내 정보 가져오기
    @GET("/api/user/me")
    fun getMyInfo(
        @Header("Authorization") token: String  //header에 넣어서 보냄
    ): Call<User>  //반환 타입

    //회원가입
    @POST("/auth/register")
    fun registerUser(
        @Body user: RegisterUserRequest
    ): Call<KakaoLoginResponse>

    // 백엔드로 위치보내는 서비스 api
    @POST("/api/location")
    fun sendLocation(@Body location: LocationRequestBody): Call<Void>

    // FCM 토큰 전송
    @POST("/api/user/fcm-token")
    fun sendFcmToken(
        @Body token: Map<String, String>
    ): Call<Void>

    //환자 목록 조회
    @GET("/api/patients/select")
    suspend fun getPatients(

    ): List<Patient>

    //환자 등록
    @POST("/api/patients/insert")
    suspend fun addPatients(
        @Body phone: Phone
    ): Response<Unit>

    // 환자 위치 Redis에서 받아오기
    @GET("/api/location/select/{patientId}")
    fun getCurrentLocation(@Path("patientId") patientId: Long): Call<LocationRequestBody>

    // 백엔드로 위치보내는 서비스 api
    @POST("/api/location/save")
    fun saveDBLocation(@Body location: LocationRequestBody): Call<Void>

    // 백엔드로 위치보내서 웹소켓 서비스 api
    @POST("/api/location/websocket")
    fun websocketLocation(@Body location: LocationRequestBody): Call<Void>

    @POST("/api/schedules/sync")
    suspend fun syncSchedules(@Body schedules: List<ScheduleDto>): Response<Unit>

    @GET("/api/schedules/send-patientSchedules") // 신호왔을때 모두 받아오기
    suspend fun getPendingSchedules(): List<Schedule>

    @GET("/api/schedules/on-login")  //로그인되었을때 모두 받아오기
    suspend fun getServerScheduleOnLogin(): List<Schedule>
    
    @GET("api/records/user/{patientId}")
    suspend fun getPatientRecords(@Path("patientId") patientId: String?): Response<List<RecordResponse>>

    @GET("api/schedules/patient/{id}")
    suspend fun getSchedules(@Path("id") patientId: Long): List<ScheduleDto>

    @POST("/api/schedules/addPatientSchedule")
    suspend fun addPatientSchedule(@Body schedules: ScheduleDto): Response<Unit>
}