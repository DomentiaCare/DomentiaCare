package com.example.domentiacare.data.repository

import java.io.File
import java.time.LocalDateTime

// 4. LocalDataSource 인터페이스 및 구현체
interface LocalDataSource {
    suspend fun saveWavFile(wavFile: File)
    suspend fun saveLlamaData(prompt: String, response: String)
    suspend fun saveCallMetadata(
        fileName: String,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    )
    suspend fun clearTempFiles()
}