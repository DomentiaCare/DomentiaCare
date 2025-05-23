package com.example.domentiacare.data.repository
import com.example.domentiacare.data.api.CallApiService

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.time.LocalDateTime

class CallRepositoryImpl(
    private val context: Context,
    private val api: CallApiService,
    private val localDataSource: LocalDataSource
) : CallRepository {
    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun analyzeLlama(transcript: String): String {
        // Llama 모델 실행 로직
        return "일정 분석 결과" // 실제 구현 필요
    }

    override suspend fun saveWavFileLocally(wavFile: File) {
        localDataSource.saveWavFile(wavFile)
    }

    override suspend fun saveLlamaDataLocally(prompt: String, response: String) {
        localDataSource.saveLlamaData(prompt, response)
    }

    override suspend fun saveCallMetadataLocally(
        fileName: String,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    ) {
        localDataSource.saveCallMetadata(fileName, transcript, memo, dateTime)
    }

    override suspend fun sendDataToServer(
        wavFile: File,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    ) {
        // API 호출 로직
        api.uploadCallData(wavFile, transcript, memo, dateTime)
    }

    override suspend fun clearLocalTempFiles() {
        localDataSource.clearTempFiles()
    }
}