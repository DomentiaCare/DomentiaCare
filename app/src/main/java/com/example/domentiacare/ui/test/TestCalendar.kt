package com.example.domentiacare.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.domentiacare.ui.screen.schedule.GuardianScheduleScreen
import java.time.LocalDate

@Composable
fun TestCalendar() {
    val selectedDate = remember { mutableStateOf(LocalDate.now()) }

    // 샘플 데이터
    val sampleMap = mapOf(
        LocalDate.now().minusDays(1) to listOf("검진 일정"),
        LocalDate.now() to listOf("약 복용", "병원 예약"),
        LocalDate.now().plusDays(2) to listOf("가족 모임")
    )

    GuardianScheduleScreen(
        scheduleMap = sampleMap,
        selectedDate = selectedDate.value,
        onDateSelected = { selectedDate.value = it },
        scheduleList = sampleMap[selectedDate.value] ?: emptyList(),
        onAddScheduleClick = { /* 일정 추가 다이얼로그 등 */ }
    )
}
