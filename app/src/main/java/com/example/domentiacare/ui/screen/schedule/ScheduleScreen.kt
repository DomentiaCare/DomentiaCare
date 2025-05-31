/*
package com.example.domentiacare.ui.screen.schedule

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.ScheduleNotificationData
import com.example.domentiacare.data.isOnline
import com.example.domentiacare.data.local.SimpleLocalStorage
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.data.local.schedule.Schedule
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun ScheduleScreen(
    navController: NavController,
    viewModel: ScheduleViewModel,
    notificationData: ScheduleNotificationData? = null // 🆕 알림 데이터 추가
) {
    Log.d("ScheduleScreen", "화면 시작됨")

    val today = LocalDate.now()
    val schedules = viewModel.schedules
    var selectedDate by remember { mutableStateOf(today) }
    val state = rememberWeekCalendarState(
        startDate = today.minusMonths(1),
        endDate = today.plusMonths(1),
        firstVisibleWeekDate = today,
        firstDayOfWeek = DayOfWeek.SUNDAY
    )
    val context = LocalContext.current
    val localStorage = remember { SimpleLocalStorage(context) }
    var loadedSchedule by remember { mutableStateOf<SimpleSchedule?>(null) }
    // 코루틴으로 데이터 불러오기
    LaunchedEffect(Unit) {
        loadedSchedule = localStorage.getOverwrittenSchedule()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
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
                                // 일정 수정 로직
                                Log.d("ScheduleScreen", "일정 수정 클릭")
                                val schedule = convertSimpleToRoomSchedule(data)
                                viewModel.addSchedule(schedule ,isOnline(context))
                                localStorage.clearOverwrittenSchedule()
                                loadedSchedule = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("저장")
                        }

                        OutlinedButton(
                            onClick = {
                                // 일정 확인 로직
                                // 일정 화면으로 넘어가야함
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
        // 🆕 알림에서 온 경우 상단에 특별한 카드 표시
        */
/*notificationData?.let { data ->
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
                    data.summary?.let {
                        InfoRow(label = "제목", value = it)
                    }

                    data.date?.let {
                        InfoRow(label = "날짜", value = it)
                    }

                    data.time?.let {
                        InfoRow(label = "시간", value = it)
                    }

                    data.place?.let {
                        InfoRow(label = "장소", value = it)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 액션 버튼들
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // 일정 수정 로직
                                Log.d("ScheduleScreen", "일정 수정 클릭")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("수정하기")
                        }

                        OutlinedButton(
                            onClick = {
                                // 일정 확인 로직
                                // 일정 화면으로 넘어가야함
                                Log.d("ScheduleScreen", "일정 확인 클릭")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("확인")
                        }
                    }

                }
            }
        }*//*


        // 기존 캘린더 UI
        // 새로운 코드 (추가):
        HorizontalCalendarComponent(
            selectedDate = selectedDate,
            onDateSelected = {
                selectedDate = it
                println("선택된 날짜: $it")
                navController.navigate("scheduleDetail/${it}")
            },
            schedules = schedules
        )
    }
        // 오른쪽 하단에 FAB 추가
        FloatingActionButton(
            onClick = { navController.navigate("addSchedule/${today}") { launchSingleTop = true } },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "일정 추가")
        }
}
}

// 🆕 정보 표시용 헬퍼 컴포넌트
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
        recordName = simple.file_name // 또는 다른 적절한 의미로 매핑
    )
}


fun convertToIsoDateTime(raw: String): String {
    // 1. 요일과 시간 분리
    val parts = raw.split("T")
    val dayOfWeekStr = parts.getOrNull(0) ?: return ""
    val timePartRaw = parts.getOrNull(1)?.replace("Z", "") ?: return ""

    // 2. 시간 파싱 (ex: 1230:00.000 → 12:30)
    val hour = timePartRaw.substring(0, 2).toIntOrNull() ?: 0
    val minute = timePartRaw.substring(2, 4).toIntOrNull() ?: 0

    // 3. 요일 매핑
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

    // 4. 오늘 날짜 기준으로 가장 가까운 해당 요일 구하기
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek
    val daysUntilTarget = (dayOfWeek.value - todayDayOfWeek.value + 7) % 7
    val targetDate = today.plusDays(daysUntilTarget.toLong())

    // 5. 최종 ISO 형식 조합
    val resultDateTime = LocalDateTime.of(targetDate, LocalTime.of(hour, minute))
    return resultDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // "2025-05-30T12:30:00"
}
*/
