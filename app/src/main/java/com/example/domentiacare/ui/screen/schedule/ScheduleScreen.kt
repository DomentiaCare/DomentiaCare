package com.example.domentiacare.ui.screen.schedule

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import java.time.DayOfWeek
import java.time.LocalDate
import com.example.domentiacare.ScheduleNotificationData

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        // 🆕 알림에서 온 경우 상단에 특별한 카드 표시
        notificationData?.let { data ->
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
                                // 일정 삭제 로직
                                Log.d("ScheduleScreen", "일정 삭제 클릭")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("삭제하기")
                        }
                    }
                }
            }
        }

        // 기존 캘린더 UI
        PagerCalendar(
            selectedDate = selectedDate,
            onDateSelected = {
                selectedDate = it
                println("선택된 날짜: $it")
                navController.navigate("scheduleDetail/${it}")
            },
            schedules = schedules
        )
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