package com.example.domentiacare.data.remote.dto

data class RegisterUserRequest(  //회원가입 정보 백엔드로 보내는 DTO
    // Add other fields as needed
    val email: String,
    val nickname: String,
    val phone: String,
    val gender: String,
    val birthDate: String,
    val address: String,
    val addressDetail1: String,
    val addressDetail2: String,
    val role: String,
    val sigungu: String  // 시군구 정보 추가
)