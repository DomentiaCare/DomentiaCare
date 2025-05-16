package com.example.domentiacare.data.remote.api

import com.example.domentiacare.data.remote.dto.KakaoLoginResponse
import com.example.domentiacare.data.remote.dto.KakaoTokenRequest
import com.example.domentiacare.data.remote.dto.LocationRequestBody
import com.example.domentiacare.data.remote.dto.RegisterUserRequest
import com.example.domentiacare.data.remote.dto.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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

}