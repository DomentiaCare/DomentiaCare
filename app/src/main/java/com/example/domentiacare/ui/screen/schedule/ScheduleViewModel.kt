package com.example.domentiacare.ui.screen.schedule

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.data.local.schedule.Schedule
import com.example.domentiacare.data.local.schedule.ScheduleRepository
import com.example.domentiacare.data.local.schedule.scheduleAlarm
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScheduleViewModel(context: Context)
    : ViewModel() {

    private val repository = ScheduleRepository(context)

    var schedules = mutableStateListOf<Schedule>()
        private set

    init {
//        loadSchedules()
        viewModelScope.launch {  // Flow를 사용해서 자동으로 갱신되게
            repository.getScheduleFlow().collect { scheduleList ->
                schedules.clear()
                schedules.addAll(scheduleList)

                //알림 등록하기
                scheduleList.forEach { schedule ->
                    try {
                        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        val triggerTime = LocalDateTime.parse(schedule.startDate, formatter)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                        val now = System.currentTimeMillis()

                        if (triggerTime > now ) {
                            scheduleAlarm(context, schedule) // ⬅️ 알림 등록
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // 파싱 실패 등 예외 처리
                    }
                }

            }
        }

    }
    fun addSchedule(schedule: Schedule, isOnline: Boolean) {
//        schedules.add(schedule)
        viewModelScope.launch {
            repository.addSchedule(schedule, isOnline)
//            loadSchedules()
        }
    }
    fun addSchedules(schedule: List<Schedule>) {
//        schedules.add(schedule)
        viewModelScope.launch {
            repository.addSchedules(schedule)
//            loadSchedules()
        }
    }

//    fun getSchedulesForDate(date: LocalDate): List<Schedule> {
//        return schedules.filter { it.date == date }
//    }
fun getSchedulesForDate(date: LocalDate): List<Schedule> {
    return schedules.filter {
        // date 비교 로직: 날짜 문자열 처리 필요
        LocalDate.parse(it.startDate.substring(0, 10)) == date
    }
}


    fun loadSchedules() {  //변경이 생길때마다 호출하는 함수 근데 자동으로 Flow해서 필요없어짐?
        viewModelScope.launch {
            val loaded = repository.getAllSchedules()
            schedules.clear()
            schedules.addAll(loaded)
        }
    }

    fun syncOfflineSchedules() {
        viewModelScope.launch {
            repository.syncSchedulesIfNeeded()
//            loadSchedules() // 최신화 반영
        }
    }

    fun syncServerSchedules(){
        viewModelScope.launch {
            CurrentUser.user?.let { repository.syncServerSchedules(it.id) }
//            loadSchedules() // 최신화 반영
        }
    }

    fun clearSchedulesOnLogout() {
        viewModelScope.launch {
            repository.clearLocalSchedules()
            // schedules 리스트도 비워지게 Flow가 감지해서 자동 반영됨
        }
    }

    fun syncFromServerAfterLogin() {
        viewModelScope.launch {
            repository.getServerScheduleOnLogin() // 서버에서 일정 받아와 Room에 저장
            // Flow가 자동 감지해서 UI 갱신됨
        }
    }
}
