package com.example.domentiacare.ui.screen.schedule

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.domentiacare.data.remote.dto.Schedule
import java.time.LocalDate

class ScheduleViewModel : ViewModel() {
    var schedules = mutableStateListOf<Schedule>()
        private set

    fun addSchedule(schedule: Schedule) {
        schedules.add(schedule)
    }

    fun getSchedulesForDate(date: LocalDate): List<Schedule> {
        return schedules.filter { it.date == date }
    }
}
