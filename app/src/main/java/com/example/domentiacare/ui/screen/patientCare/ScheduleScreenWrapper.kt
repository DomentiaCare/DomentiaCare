package com.example.domentiacare.ui.screen.patientCare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.LocalDate

@Composable
fun ScheduleScreenWrapper(navController: NavController, patientId: Long) {
    val viewModel: PatientScheduleViewModel = viewModel() // ViewModel에서 일정 로딩
    val selectedDate = remember { mutableStateOf(LocalDate.now()) }

    val scheduleMap = viewModel.scheduleMap // 날짜별 일정 맵
    val scheduleList = scheduleMap[selectedDate.value].orEmpty()

    LaunchedEffect(patientId) {
        viewModel.loadSchedulesFromServer(patientId)
    }

    GuardianScheduleScreen(
        scheduleMap = scheduleMap,
        onDateSelected = { selectedDate.value = it },
        selectedDate = selectedDate.value,
        scheduleList = scheduleList,
        onAddScheduleClick = {
            // 일정 추가 화면 이동 로직 (예: navController.navigate("addSchedule/$patientId"))
            navController.navigate("addSchedule/$patientId/${selectedDate.value}")
        }
    )
}
