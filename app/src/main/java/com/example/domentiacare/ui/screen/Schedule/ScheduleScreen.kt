package com.example.domentiacare.ui.screen.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.domentiacare.ui.component.BottomNavBar
import com.example.domentiacare.ui.component.CustomCalendar
import com.example.domentiacare.ui.component.TopBar
import com.example.domentiacare.viewmodel.CalendarViewModel
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ScheduleScreen(navController: NavController, viewModel: CalendarViewModel = hiltViewModel()) {
    val holidays = viewModel.holidays
    val currentYear = LocalDate.now().year
    val currentMonth = LocalDate.now().monthValue

    LaunchedEffect(Unit) {
        viewModel.loadHolidays(currentYear, currentMonth)
    }
    val today = LocalDate.now()

    //topbar
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(today) }
    val state = rememberWeekCalendarState(
        startDate = today.minusMonths(1),
        endDate = today.plusMonths(1),
        firstVisibleWeekDate = today,
        firstDayOfWeek = DayOfWeek.SUNDAY
    )

    Scaffold(
        topBar = { TopBar(title = "DomenticaCare", drawerState = drawerState, scope = scope) },
        bottomBar = { BottomNavBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            CustomCalendar(
                holidays = holidays,
                onDateSelected = { selectedDate ->
                    // 선택된 날짜 처리
                    println("선택된 날짜: $selectedDate")
                }
            )
        }
    }
}
