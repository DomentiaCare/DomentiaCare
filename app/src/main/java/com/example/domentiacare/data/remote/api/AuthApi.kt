package com.example.domentiacare.data.remote.api

import com.example.domentiacare.data.remote.dto.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/kakao")
    fun kakaoLogin(
        @Body tokenRequest: KakaoTokenRequest
    ): Call<KakaoLoginResponse>
}