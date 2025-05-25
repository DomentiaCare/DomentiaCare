package com.example.domentiacare.data

import java.util.UUID

data class ScheduleData(
    val id: String = UUID.randomUUID().toString(), // 고유 ID
    val whisperPrompt: String,
    val whisperResult: String,
    val llamaPrompt: String,
    val llamaResult: String,
    val scheduleInfo: ScheduleInfo,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false // 서버 동기화 여부
)