package com.example.domentiacare.network.dto

import com.google.gson.annotations.SerializedName

data class ScheduleCreateRequest(
    @SerializedName("userId")
    val userId: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("startDate")
    val startDate: String, // "yyyy-MM-dd'T'HH:mm:ss" 형식

    @SerializedName("endDate")
    val endDate: String, // "yyyy-MM-dd'T'HH:mm:ss" 형식

    @SerializedName("isAi")
    val isAi: Boolean = true
)