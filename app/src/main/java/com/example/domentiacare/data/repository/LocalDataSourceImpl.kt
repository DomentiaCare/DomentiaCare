package com.example.domentiacare.data.repository

import android.content.Context
import java.io.File
import java.time.LocalDateTime

class LocalDataSourceImpl(private val context: Context) : LocalDataSource {
    override suspend fun saveWavFile(wavFile: File) {
        // 파일 저장 로직
    }

    override suspend fun saveLlamaData(prompt: String, response: String) {
        // Llama 데이터 저장 로직
    }

    override suspend fun saveCallMetadata(
        fileName: String,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    ) {
        // 메타데이터 저장 로직
    }

    override suspend fun clearTempFiles() {
        // 임시 파일 삭제 로직
    }
}