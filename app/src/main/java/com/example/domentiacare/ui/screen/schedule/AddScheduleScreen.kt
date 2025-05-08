package com.example.domentiacare.ui.screen.schedule

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.data.remote.dto.Schedule
import com.example.domentiacare.ui.component.BottomNavBar
import com.example.domentiacare.ui.component.SimpleDropdown
import com.example.domentiacare.ui.component.TopBar
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate

@Composable
fun AddScheduleScreen(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    selectedDate: String,
    viewModel: ScheduleViewModel
) {

    var scheduleText by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf("09") }
    var selectedMinute by remember { mutableStateOf("00") }

    val hours = (0..23).map { String.format("%02d", it) }
    val minutes = listOf("00", "10", "20", "30", "40", "50")

    Scaffold(
        topBar = { TopBar(title = "일정 추가", drawerState = drawerState, scope = scope) },
        bottomBar = { BottomNavBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("선택된 날짜: $selectedDate", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = scheduleText,
                onValueChange = { scheduleText = it },
                label = { Text("일정 내용") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 시간 선택 (시)
            SimpleDropdown(
                label = "시 선택",
                options = hours,
                selectedOption = selectedHour,
                onOptionSelected = { selectedHour = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 시간 선택 (분)
            SimpleDropdown(
                label = "분 선택",
                options = minutes,
                selectedOption = selectedMinute,
                onOptionSelected = { selectedMinute = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val time = "$selectedHour:$selectedMinute"
                    val date = LocalDate.parse(selectedDate)
                    val newSchedule = Schedule(date = date, time = time, content = scheduleText)
                    Log.d("AddSchedule", "$selectedDate $time: $scheduleText")
                    viewModel.addSchedule(newSchedule) // ✅ ViewModel에 추가
                    Log.d("AddSchedule", "스케쥴스${viewModel.schedules}")
                    Log.d("AddSchedule", "오늘 스케줄스${viewModel.getSchedulesForDate(date)}")
                    navController.popBackStack()                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("추가")
            }
        }
    }
}

