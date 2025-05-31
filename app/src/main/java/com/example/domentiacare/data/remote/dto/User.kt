package com.example.domentiacare.data.remote.dto


data class User(
    val id: Long,
//    val email: String,
//    val nickname: String
    val email: String,
    val nickname: String,
    val phone: String,
    val gender: String,
    val birthDate: String,
    val address: String,
    val addressDetail1: String,
    val addressDetail2: String,
    val role: String,
    val managerId : Long,
    val managerPhone : String
)