package com.example.domentiacare.ui.screen.patientCare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.data.local.schedule.ScheduleDto
import com.example.domentiacare.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun PatientAddScheduleScreen(
    navController: NavController,
    patientId: Long,
    selectedDate: String,
    viewModel: PatientScheduleViewModel
) {
    val context = LocalContext.current

    var scheduleText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf(LocalDate.parse(selectedDate)) }
    var endDate by remember { mutableStateOf(LocalDate.parse(selectedDate)) }

//    var startHour by remember { mutableStateOf("09") }
//    var startMinute by remember { mutableStateOf("00") }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(9) }
    var endMinute by remember { mutableStateOf(0) }
//    var endHour by remember { mutableStateOf("10") }
//    var endMinute by remember { mutableStateOf("00") }

    val hours = (0..23).map { String.format("%02d", it) }
    val minutes = listOf("00", "10", "20", "30", "40", "50")

    Column(modifier = Modifier.padding(16.dp)) {
        Text("일정 추가", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = scheduleText,
            onValueChange = { scheduleText = it },
            label = { Text("일정 내용") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 날짜 선택 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("시작 날짜")
                Text(
                    text = startDate.toString(),
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable {
                            showDatePicker(context, startDate) { selected ->
                                startDate = selected
                            }
                        }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("종료 날짜")
                Text(
                    text = endDate.toString(),
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable {
                            showDatePicker(context, endDate) { selected ->
                                endDate = selected
                            }
                        }
                )
            }
        }

        // 시간 선택 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TimePickerField(
                    initialHour = startHour,
                    initialMinute = startMinute,
                    onTimeSelected = { hour, minute ->
                        startHour = hour
                        startMinute = minute
                    }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                TimePickerField(
                    initialHour = endHour,
                    initialMinute = endMinute,
                    onTimeSelected = { hour, minute ->
                        endHour = hour
                        endMinute = minute
                    }
                )
            }
        }


        OutlinedTextField(
            value = descriptionText,
            onValueChange = { descriptionText = it },
            label = { Text("일정 설명") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), // 높이 조절
            singleLine = false,
            maxLines = 5
        )


        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val startDateTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute))
                val endDateTime = LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute))

                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val newSchedule = ScheduleDto(
                    title = scheduleText,
                    description = descriptionText,
                    startDate = startDateTime.format(formatter),
                    endDate = endDateTime.format(formatter),
                    isAi = false,
                    isCompleted = false,
                    recordName = null,
                    patientId = patientId
                )
                Log.d("AddSchedule", "새 일정: $patientId")
                //todo 일정 등록
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val response = RetrofitClient.authApi.addPatientSchedule(newSchedule)
                        if (response.isSuccessful) {
                            navController.popBackStack()
                        } else {
                            Log.e("AddSchedule", "실패: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("AddSchedule", "예외 발생", e)
                    }
                }
                navController.popBackStack()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("추가")
        }
    }
}


fun showDatePicker(context: Context, initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val calendar = Calendar.getInstance().apply {
        set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
    }

    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selected = LocalDate.of(year, month + 1, day)
            onDateSelected(selected)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
fun TimePickerField(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current

    // 초기 시간 상태
    var selectedTime by remember { mutableStateOf(LocalTime.of(initialHour, initialMinute)) }

    // 포맷터: 오전/오후 hh:mm
    val timeFormatter = DateTimeFormatter.ofPattern("a hh : mm", Locale.KOREA)

    Column {
        Text(
            text = selectedTime.format(timeFormatter), // → "오후 03 : 00" 같은 형식
            modifier = Modifier
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            selectedTime = LocalTime.of(hour, minute)
                            onTimeSelected(hour, minute)
                        },
                        initialHour,
                        initialMinute,
                        false // ← 12시간제로 변경 가능, true면 24시간제
                    ).show()
                }
                .padding(5.dp)
        )
    }
}
