package com.example.domentiacare.network.dto

import com.google.gson.annotations.SerializedName

data class ScheduleResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("userId")
    val userId: Long,

    @SerializedName("creatorId")
    val creatorId: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String,

    @SerializedName("isAi")
    val isAi: Boolean,

    @SerializedName("isCompleted")
    val isCompleted: Boolean,

    @SerializedName("notificationSent")
    val notificationSent: Boolean,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("creatorName")
    val creatorName: String?,

    @SerializedName("userName")
    val userName: String?
)