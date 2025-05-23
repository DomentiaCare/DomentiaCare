package com.example.domentiacare.data.api

import java.io.File
import java.time.LocalDateTime

interface CallApiService {
    suspend fun uploadCallData(
        wavFile: File,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    )
}