package com.example.domentiacare.ui.screen.schedule

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.ui.component.BottomNavBar
import com.example.domentiacare.ui.component.CustomCalendar
import com.example.domentiacare.ui.component.TopBar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import kotlinx.coroutines.CoroutineScope
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ScheduleScreen(navController: NavController,
                   drawerState: DrawerState,
                   scope: CoroutineScope, viewModel: ScheduleViewModel) {
    Log.d("ScheduleScreen", "화면 시작됨") // ✅ 가장 먼저

    val today = LocalDate.now()
    val currentYear = LocalDate.now().year
    val currentMonth = LocalDate.now().monthValue

    val schedules = viewModel.schedules



    var selectedDate by remember { mutableStateOf(today) }
    val state = rememberWeekCalendarState(
        startDate = today.minusMonths(1),
        endDate = today.plusMonths(1),
        firstVisibleWeekDate = today,
        firstDayOfWeek = DayOfWeek.SUNDAY
    )

    Scaffold(
        topBar = { TopBar(title = "일정 관리", drawerState = drawerState, scope = scope) },
        bottomBar = { BottomNavBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            CustomCalendar(
                selectedDate = selectedDate,
                onDateSelected = {
                    selectedDate = it // ✅ 상태 업데이트
                    println("선택된 날짜: $it")
                    navController.navigate("scheduleDetail/${it}")
                },
                schedules = schedules // 전달

            )
        }
    }
}
