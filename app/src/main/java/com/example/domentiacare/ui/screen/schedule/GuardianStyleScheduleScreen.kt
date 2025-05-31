package com.example.domentiacare.ui.screen.schedule

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domentiacare.ScheduleNotificationData
import com.example.domentiacare.data.isOnline
import com.example.domentiacare.data.local.SimpleLocalStorage
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.data.local.schedule.Schedule
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
    notificationData: ScheduleNotificationData? = null
) {
    Log.d("ScheduleScreen", "화면 시작됨")

    val today = LocalDate.now()
    val schedules = viewModel.schedules
    var selectedDate by remember { mutableStateOf(today) }
    val context = LocalContext.current
    val localStorage = remember { SimpleLocalStorage(context) }
    var loadedSchedule by remember { mutableStateOf<SimpleSchedule?>(null) }

    // Schedule 객체를 Map으로 변환
    val scheduleMap = schedules.groupBy { schedule ->
        LocalDate.parse(schedule.startDate.substring(0, 10))
    }

    // 선택된 날짜의 일정 리스트
    // 선택된 날짜의 일정 리스트 (시간 순서대로 정렬)
    val schedulesForSelectedDate = scheduleMap[selectedDate]
        ?.sortedBy { schedule ->
            // startDate에서 시간 부분을 추출하여 정렬 기준으로 사용
            val timePart = schedule.startDate.substring(11, 16) // "HH:mm" 형태로 추출
            timePart
        }
        ?.map { schedule ->
            "${schedule.startDate.substring(11, 16)} - ${schedule.title}"
        } ?: emptyList()

    // 달력 상태
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek() }
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstDayOfWeek = daysOfWeek.first(),
        firstVisibleMonth = currentMonth
    )
    val visibleMonth = calendarState.firstVisibleMonth.yearMonth

    // 코루틴으로 데이터 불러오기
    LaunchedEffect(Unit) {
        loadedSchedule = localStorage.getOverwrittenSchedule()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 자동 등록된 일정 카드 (ScheduleScreen 로직)
            loadedSchedule?.let { data ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "자동 등록된 일정",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 일정 정보 표시
                        data.title?.let {
                            InfoRow(label = "제목", value = it)
                        }

                        data.startDate?.let { startDate ->
                            val parts = startDate.split("T")
                            val datePart = parts.getOrNull(0) ?: ""
                            InfoRow(label = "날짜", value = datePart)
                        }

                        data.startDate?.let { startDate ->
                            val parts = startDate.split("T")
                            val timePart = parts.getOrNull(1)?.replace("Z", "") ?: ""
                            val formattedTime = timePart.substring(0, 2) + ":" + timePart.substring(2, 4)
                            InfoRow(label = "시간", value = formattedTime)
                        }

                        data.description?.let {
                            InfoRow(label = "설명", value = it)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 액션 버튼들
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    Log.d("ScheduleScreen", "일정 수정 클릭")
                                    val schedule = convertSimpleToRoomSchedule(data)
                                    viewModel.addSchedule(schedule, isOnline(context))
                                    localStorage.clearOverwrittenSchedule()
                                    loadedSchedule = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("저장")
                            }

                            OutlinedButton(
                                onClick = {
                                    Log.d("ScheduleScreen", "일정 취소 클릭")
                                    localStorage.clearOverwrittenSchedule()
                                    loadedSchedule = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("취소")
                            }
                        }
                    }
                }
            }

            // GuardianScheduleScreen 스타일의 달력 헤더
            CalendarTitle(currentMonth = visibleMonth)

            // 요일 표시
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { dayOfWeek ->
                    val textColor = when (dayOfWeek) {
                        java.time.DayOfWeek.SUNDAY -> Color.Red
                        java.time.DayOfWeek.SATURDAY -> Color(0xFF1976D2)
                        else -> Color.Black
                    }
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // HorizontalCalendar with Schedule logic
            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    val hasSchedule = scheduleMap[day.date]?.isNotEmpty() == true
                    val isSelected = day.date == selectedDate
                    val isCurrentMonth = day.position == DayPosition.MonthDate
                    val dayOfWeek = day.date.dayOfWeek

                    val dayColor = when {
                        isSelected -> Color.White
                        !isCurrentMonth -> Color.LightGray
                        dayOfWeek == java.time.DayOfWeek.SUNDAY -> Color.Red
                        dayOfWeek == java.time.DayOfWeek.SATURDAY -> Color(0xFF1976D2)
                        else -> Color.Black
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(if (isSelected) Color(0xFFED7D31) else Color.Transparent)
                            .clickable {
                                selectedDate = day.date
                                println("선택된 날짜: ${day.date}")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                color = dayColor
                            )
                            if (hasSchedule) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFFED7D31), shape = CircleShape)
                                )
                            }
                        }
                    }
                }
            )

            // 선택된 날짜 표시
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFED7D31).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Schedule Icon",
                        tint = Color(0xFFED7D31),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)", Locale.KOREAN)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFED7D31)
                    )
                }
            }

            // 일정 리스트
            if (schedulesForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "No Schedule",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "등록된 일정이 없습니다",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(schedulesForSelectedDate.withIndex().toList()) { (index, item) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            Color(0xFFED7D31),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item,
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // FloatingActionButton
        FloatingActionButton(
            onClick = { navController.navigate("addSchedule/${selectedDate}") { launchSingleTop = true } },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFFED7D31)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Schedule",
                tint = Color.White
            )
        }
    }
}

@Composable
fun CalendarTitle(currentMonth: YearMonth) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFED7D31))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + currentMonth.year,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// 정보 표시용 헬퍼 컴포넌트
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun convertSimpleToRoomSchedule(simple: SimpleSchedule): Schedule {
    return Schedule(
        title = simple.title,
        description = simple.description,
        startDate = convertToIsoDateTime(simple.startDate),
        endDate = convertToIsoDateTime(simple.endDate),
        isAi = true,
        recordName = simple.file_name
    )
}

fun convertToIsoDateTime(raw: String): String {
    val parts = raw.split("T")
    val dayOfWeekStr = parts.getOrNull(0) ?: return ""
    val timePartRaw = parts.getOrNull(1)?.replace("Z", "") ?: return ""

    val hour = timePartRaw.substring(0, 2).toIntOrNull() ?: 0
    val minute = timePartRaw.substring(2, 4).toIntOrNull() ?: 0

    val dayOfWeek = when (dayOfWeekStr.lowercase(Locale.getDefault())) {
        "monday" -> DayOfWeek.MONDAY
        "tuesday" -> DayOfWeek.TUESDAY
        "wednesday" -> DayOfWeek.WEDNESDAY
        "thursday" -> DayOfWeek.THURSDAY
        "friday" -> DayOfWeek.FRIDAY
        "saturday" -> DayOfWeek.SATURDAY
        "sunday" -> DayOfWeek.SUNDAY
        else -> return ""
    }

    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek
    val daysUntilTarget = (dayOfWeek.value - todayDayOfWeek.value + 7) % 7
    val targetDate = today.plusDays(daysUntilTarget.toLong())

    val resultDateTime = LocalDateTime.of(targetDate, LocalTime.of(hour, minute))
    return resultDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}