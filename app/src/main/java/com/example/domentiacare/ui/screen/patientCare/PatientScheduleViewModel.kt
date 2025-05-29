package com.example.domentiacare.ui.screen.patientCare

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class PatientScheduleViewModel : ViewModel() {
    private val _scheduleMap = mutableStateMapOf<LocalDate, List<String>>() // 날짜별 일정
    val scheduleMap: Map<LocalDate, List<String>> = _scheduleMap

    fun loadSchedulesFromServer(patientId: Long) {
        viewModelScope.launch {
            try {
                // 예시 API 호출 (Retrofit 사용 가정)
                val schedules = RetrofitClient.authApi.getSchedules(patientId)
                _scheduleMap.clear()
                schedules.forEach { schedule ->
                    val dateTime = LocalDateTime.parse(schedule.startDate)
                    val date = dateTime.toLocalDate()
                    val timeFormatted = dateTime.toLocalTime().toString() // 예: "09:00"
                    val scheduleText = "${timeFormatted} - ${schedule.title}"

                    val existing = _scheduleMap[date].orEmpty()
                    _scheduleMap[date] = existing + scheduleText
                }
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "일정 로딩 실패", e)
            }
        }
    }
}