package com.example.domentiacare.data.local.schedule

import android.content.Context
import android.util.Log
import com.example.domentiacare.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val context: Context
) {
    val dao = ScheduleDatabaseProvider.getDatabase(context).scheduleDao()

    suspend fun syncSchedulesIfNeeded() {  //오프라인에서 온라인 되었을때 sync
        // sync가 false인것만 가져와서 백엔드로 넘김
        val unsynced = dao.getUnsyncedSchedules()
        if (unsynced.isEmpty()) return

        val dtoList = unsynced.map {
            ScheduleDto(it.title, it.description, it.startDate, it.endDate)
        }

        val response = RetrofitClient.authApi.syncSchedules(dtoList)
        if (response.isSuccessful) {
            val updated = unsynced.map { it.copy(isSynced = true) }
            dao.updateSchedules(updated)
        }
    }

    suspend fun addSchedule(schedule: Schedule, isOnline: Boolean) {
        //온라인이든 오프라인이든 일정 추가했을때의 함수
        val toInsert = if (isOnline) {
            // 온라인이면 바로 서버 전송 시도
            val dto = ScheduleDto(schedule.title, schedule.description, schedule.startDate, schedule.endDate, schedule.isAi, schedule.isCompleted, schedule.recordName )
            val response = RetrofitClient.authApi.syncSchedules(listOf(dto))

            if (response.isSuccessful) {
                schedule.copy(isSynced = true)
            } else {
                schedule.copy(isSynced = false) // 실패 시 로컬에 비동기 상태로 저장
            }
        } else {
            schedule.copy(isSynced = false)
        }
        dao.insertSchedule(toInsert)

    }


    suspend fun addSchedules(schedules: List<Schedule>) {
        dao.insertSchedules(schedules)
    }

    suspend fun getAllSchedules(): List<Schedule> {
        return dao.getAllSchedules()
    }

    suspend fun syncServerSchedules(userId: Long) {  //서버에 연결되었을때 서버DB 가져오는코드
        try {
            val response = RetrofitClient.authApi.getPendingSchedules()
            if (response.isNotEmpty()) {
                addSchedules(response)
                Log.d("FCM", "✅ 서버 일정 수신 + Room 저장 + 알림 전송 완료")
            }
        } catch (e: Exception) {
            Log.e("FCM", "❌ 서버 일정 수신 실패", e)
        }
    }

    fun getScheduleFlow(): Flow<List<Schedule>> {  //RoomDB에 자동으로 넣으면 자동으로 가져오게함
        return dao.getAllSchedulesFlow()
    }

    suspend fun clearLocalSchedules() {  //로그아웃시
        val allSchedules = dao.getAllSchedules()
        allSchedules.forEach {
            cancelAlarm(context, it)
        }

        dao.deleteAllSchedules()
        Log.d("ScheduleAlarm", "🧹 모든 알람 취소 및 일정 삭제 완료")

    }

    suspend fun getServerScheduleOnLogin() {  //로그인시 모두 가져옴
        try {
            val response = RetrofitClient.authApi.getServerScheduleOnLogin()
            if (response.isNotEmpty()) {
                addSchedules(response)
                Log.d("KakaoLogin", "로그인 시 일정 수신 + Room 저장 완료")
            }
        } catch (e: Exception) {
            Log.e("KakaoLogin", "❌ 로그인 시 서버 일정 수신 실패", e)
        }
    }
}
