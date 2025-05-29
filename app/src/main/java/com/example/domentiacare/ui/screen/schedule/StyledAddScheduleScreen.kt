package com.example.domentiacare.ui.screen.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domentiacare.data.isOnline
import com.example.domentiacare.data.local.schedule.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun AddScheduleScreen(
    navController: NavController,
    selectedDate: String,
    viewModel: ScheduleViewModel
) {
    val context = LocalContext.current

    var scheduleText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf(LocalDate.parse(selectedDate)) }
    var endDate by remember { mutableStateOf(LocalDate.parse(selectedDate)) }

    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(10) }
    var endMinute by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 헤더
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFED7D31).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = "Add Schedule",
                    tint = Color(0xFFED7D31),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "일정 추가",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFFED7D31)
                )
            }
        }

        // 일정 내용 입력
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "Schedule Title",
                        tint = Color(0xFFED7D31),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "일정 제목",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                TextField(
                    value = scheduleText,
                    onValueChange = { scheduleText = it },
                    placeholder = { Text("일정 제목을 입력하세요") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFFED7D31),
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFED7D31)
                    ),
                    singleLine = true
                )
            }
        }

        // 날짜 선택
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = Color(0xFFED7D31),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "날짜 설정",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "시작 날짜",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    showDatePicker(context, startDate) { selected ->
                                        startDate = selected
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = startDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")),
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "종료 날짜",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    showDatePicker(context, endDate) { selected ->
                                        endDate = selected
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = endDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 시간 선택
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = Color(0xFFED7D31),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "시간 설정",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "시작 시간",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        StyledTimePickerField(
                            initialHour = startHour,
                            initialMinute = startMinute,
                            onTimeSelected = { hour, minute ->
                                startHour = hour
                                startMinute = minute

                                // 시작 시간이 종료 시간보다 늦거나 같으면 종료 시간을 자동 조정
                                val startTime = LocalTime.of(hour, minute)
                                val currentEndTime = LocalTime.of(endHour, endMinute)

                                if (startTime >= currentEndTime) {
                                    // 시작 시간보다 1시간 후로 종료 시간 설정
                                    val newEndTime = startTime.plusHours(1)
                                    endHour = newEndTime.hour
                                    endMinute = newEndTime.minute
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "종료 시간",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        StyledTimePickerField(
                            initialHour = endHour,
                            initialMinute = endMinute,
                            onTimeSelected = { hour, minute ->
                                // 종료 시간이 시작 시간보다 빠르면 설정하지 않음
                                val selectedEndTime = LocalTime.of(hour, minute)
                                val currentStartTime = LocalTime.of(startHour, startMinute)

                                if (selectedEndTime > currentStartTime) {
                                    endHour = hour
                                    endMinute = minute
                                }
                            }
                        )
                    }
                }
            }
        }

        // 일정 설명 (기존 AddScheduleScreen에는 없었지만 추가)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Description",
                        tint = Color(0xFFED7D31),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "일정 설명",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    placeholder = { Text("일정에 대한 상세 설명을 입력하세요 (선택사항)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFED7D31),
                        focusedLabelColor = Color(0xFFED7D31),
                        cursorColor = Color(0xFFED7D31)
                    ),
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // 등록 버튼
        Button(
            onClick = {
                val startDateTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute))
                val endDateTime = LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute))

                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val newSchedule = Schedule(
                    title = scheduleText,
                    description = if (descriptionText.isNotBlank()) descriptionText else scheduleText,
                    startDate = startDateTime.format(formatter),
                    endDate = endDateTime.format(formatter),
                    isAi = false,
                    isCompleted = false,
                    isSynced = false,
                    recordName = null
                )
                val online = isOnline(context)
                viewModel.addSchedule(newSchedule, online)
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFED7D31)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "일정 등록",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
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
fun StyledTimePickerField(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    var selectedTime by remember(initialHour, initialMinute) {
        mutableStateOf(LocalTime.of(initialHour, initialMinute))
    }
    val timeFormatter = DateTimeFormatter.ofPattern("a hh:mm", Locale.KOREA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .clickable {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        selectedTime = LocalTime.of(hour, minute)
                        onTimeSelected(hour, minute)
                    },
                    selectedTime.hour,
                    selectedTime.minute,
                    false
                ).show()
            }
            .padding(12.dp)
    ) {
        Text(
            text = selectedTime.format(timeFormatter),
            fontSize = 14.sp
        )
    }
}