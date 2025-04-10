package com.example.domentiacare.data.remote.api

import com.example.domentiacare.data.remote.dto.KakaoLoginResponse
import com.example.domentiacare.data.remote.dto.KakaoTokenRequest
import com.example.domentiacare.data.remote.dto.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/kakao")
    fun kakaoLogin(
        @Body tokenRequest: KakaoTokenRequest
    ): Call<KakaoLoginResponse>

    @GET("/api/user/me")
    fun getMyInfo(
        @Header("Authorization") token: String
    ): Call<User>
}