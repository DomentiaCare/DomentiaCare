// data/local/SimpleLocalStorage.kt (Room 대신 임시 사용)
package com.example.domentiacare.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 임시 간단한 데이터 클래스
data class SimpleSchedule(
    val localId: String,
    val userId: Long,
    val title: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val isAi: Boolean = true,
    val syncStatus: String = "LOCAL_ONLY",
    val createdAt: Long = System.currentTimeMillis()
)

class SimpleLocalStorage(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("schedules", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun saveSchedule(schedule: SimpleSchedule): Result<SimpleSchedule> = withContext(Dispatchers.IO) {
        try {
            val schedules = getAllSchedules().toMutableList()
            schedules.add(schedule)

            val json = gson.toJson(schedules)
            prefs.edit().putString("schedules_list", json).apply()

            Result.success(schedule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllSchedules(): List<SimpleSchedule> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString("schedules_list", "[]") ?: "[]"
            val type = object : TypeToken<List<SimpleSchedule>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPendingSchedules(): List<SimpleSchedule> = withContext(Dispatchers.IO) {
        getAllSchedules().filter { it.syncStatus != "SYNCED" }
    }

    suspend fun updateSyncStatus(localId: String, status: String) = withContext(Dispatchers.IO) {
        try {
            val schedules = getAllSchedules().toMutableList()
            val index = schedules.indexOfFirst { it.localId == localId }
            if (index != -1) {
                schedules[index] = schedules[index].copy(syncStatus = status)
                val json = gson.toJson(schedules)
                prefs.edit().putString("schedules_list", json).apply()
            }
        } catch (e: Exception) {
            // 로그만 출력
        }
    }
}