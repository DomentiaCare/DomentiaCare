package com.example.domentiacare.data.remote.dto

data class KakaoLoginResponse(
    val jwt: String,
    val user: User
)