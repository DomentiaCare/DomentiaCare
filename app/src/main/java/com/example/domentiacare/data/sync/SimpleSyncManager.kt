// data/sync/SimpleSyncManager.kt
package com.example.domentiacare.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.domentiacare.data.local.SimpleLocalStorage
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.network.ScheduleApiService
import com.example.domentiacare.network.dto.ScheduleCreateRequest
import kotlinx.coroutines.*
import java.util.*

class SimpleSyncManager private constructor(
    private val context: Context
) {
    private val localStorage = SimpleLocalStorage(context)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    companion object {
        @Volatile
        private var INSTANCE: SimpleSyncManager? = null

        fun getInstance(context: Context): SimpleSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SimpleSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * 일정 저장 - Offline-First 방식
     */
    suspend fun saveSchedule(schedule: SimpleSchedule): Result<SimpleSchedule> {
        return try {
            // 1. 항상 로컬에 먼저 저장
            val result = localStorage.saveSchedule(schedule)

            // 2. 온라인 상태라면 즉시 동기화 시도
            if (isOnline() && result.isSuccess) {
                CoroutineScope(Dispatchers.IO).launch {
                    syncSingleSchedule(schedule)
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 단일 일정 동기화
     */
    private suspend fun syncSingleSchedule(schedule: SimpleSchedule) {
        try {
            Log.d("SimpleSyncManager", "동기화 시작: ${schedule.localId}")

            // 동기화 중 상태로 변경
            localStorage.updateSyncStatus(schedule.localId, "SYNCING")

            // 서버로 전송
            val request = ScheduleCreateRequest(
                userId = schedule.userId,
                title = schedule.title,
                description = schedule.description,
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                isAi = schedule.isAi
            )

            val response = ScheduleApiService.createSchedule(request, context)

            // 성공 시 상태 변경
            localStorage.updateSyncStatus(schedule.localId, "SYNCED")
            Log.d("SimpleSyncManager", "동기화 성공: ${response.id}")

        } catch (e: Exception) {
            // 실패 시 상태 변경
            localStorage.updateSyncStatus(schedule.localId, "SYNC_FAILED")
            Log.e("SimpleSyncManager", "동기화 실패: ${e.message}")
        }
    }

    /**
     * 모든 미동기화 일정 동기화
     */
    suspend fun syncAllPendingSchedules() {
        try {
            val pendingSchedules = localStorage.getPendingSchedules()
            Log.d("SimpleSyncManager", "미동기화 일정 ${pendingSchedules.size}개 발견")

            for (schedule in pendingSchedules) {
                syncSingleSchedule(schedule)
                delay(1000) // 서버 부하 방지
            }
        } catch (e: Exception) {
            Log.e("SimpleSyncManager", "일괄 동기화 실패", e)
        }
    }

    /**
     * 네트워크 상태 확인
     */
    private fun isOnline(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 저장된 일정 목록 조회
     */
    suspend fun getAllSchedules(): List<SimpleSchedule> {
        return localStorage.getAllSchedules()
    }
}