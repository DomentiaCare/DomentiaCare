package com.example.domentiacare.data

data class AiScheduleRequest(
    val userId: Long,
    val title: String,
    val description: String?,
    val startDate: String, // "yyyy-MM-dd'T'HH:mm:ss" 형식
    val endDate: String,
    val extractedContent: String?,
    val confidence: Double?
)