package com.example.domentiacare.data.repository

import java.io.File
import java.time.LocalDateTime

interface CallRepository {
    fun isNetworkAvailable(): Boolean
    suspend fun analyzeLlama(transcript: String): String
    suspend fun saveWavFileLocally(wavFile: File)
    suspend fun saveLlamaDataLocally(prompt: String, response: String)
    suspend fun saveCallMetadataLocally(
        fileName: String,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    )
    suspend fun sendDataToServer(
        wavFile: File,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    )
    suspend fun clearLocalTempFiles()
}