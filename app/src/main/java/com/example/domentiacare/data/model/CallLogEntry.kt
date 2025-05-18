package com.example.domentiacare.data.model

data class CallLogEntry(
    val id: String,
    val name: String?,
    val number: String,
    val type: Int,        // INCOMING_TYPE, OUTGOING_TYPE 등
    val date: Long,       // epoch millis
    val duration: Long    // 초 단위
)