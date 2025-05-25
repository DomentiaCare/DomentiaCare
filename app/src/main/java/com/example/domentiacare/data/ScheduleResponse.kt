package com.example.domentiacare.data

data class ScheduleResponse(
    val id: Long,
    val userId: Long,
    val creatorId: Long,
    val title: String,
    val description: String?,
    val startDate: String,
    val endDate: String,
    val isAi: Boolean,
    val isCompleted: Boolean,
    val notificationSent: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val creatorName: String?,
    val userName: String?
)