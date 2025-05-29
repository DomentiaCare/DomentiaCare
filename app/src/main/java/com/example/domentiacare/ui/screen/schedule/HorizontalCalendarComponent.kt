package com.example.domentiacare.ui.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domentiacare.data.local.schedule.Schedule
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HorizontalCalendarComponent(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    schedules: List<Schedule>
) {
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

    Column {
        // 현재 월 표시
        CalendarHeader(currentMonth = visibleMonth)

        Spacer(modifier = Modifier.height(10.dp))

        // 요일 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { dayOfWeek ->
                val textColor = when (dayOfWeek) {
                    DayOfWeek.SUNDAY -> Color(0xFFD32F2F)
                    DayOfWeek.SATURDAY -> Color(0xFF1976D2)
                    else -> Color.Black
                }
                Text(
                    text = when (dayOfWeek) {
                        DayOfWeek.SUNDAY -> "일"
                        DayOfWeek.MONDAY -> "월"
                        DayOfWeek.TUESDAY -> "화"
                        DayOfWeek.WEDNESDAY -> "수"
                        DayOfWeek.THURSDAY -> "목"
                        DayOfWeek.FRIDAY -> "금"
                        DayOfWeek.SATURDAY -> "토"
                    },
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 달력
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                val daySchedules = schedules.filter { schedule ->
                    val scheduleDate = schedule.startDate.substring(0, 10)
                    LocalDate.parse(scheduleDate) == day.date
                }
                val hasSchedule = daySchedules.isNotEmpty()
                val isSelected = day.date == selectedDate
                val isToday = day.date == LocalDate.now()
                val isCurrentMonth = day.position == DayPosition.MonthDate
                val dayOfWeek = day.date.dayOfWeek

                val dayColor = when {
                    isSelected -> Color.White
                    !isCurrentMonth -> Color.LightGray
                    dayOfWeek == DayOfWeek.SUNDAY -> Color(0xFFD32F2F)
                    dayOfWeek == DayOfWeek.SATURDAY -> Color(0xFF1976D2)
                    else -> Color.Black
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(
                            color = when {
                                isSelected -> Color(0xFFF49000)
                                isToday -> Color(0xFFfbc271)
                                else -> Color.Transparent
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable(enabled = isCurrentMonth) {
                            onDateSelected(day.date)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            color = dayColor,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )

                        if (hasSchedule) {
                            Spacer(modifier = Modifier.height(2.dp))
                            // 첫 번째 일정의 제목을 작게 표시
                            val firstSchedule = daySchedules.first()
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFF2196F3),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = firstSchedule.title,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    color = Color.White
                                )
                            }

                            // 여러 일정이 있으면 점으로 표시
                            if (daySchedules.size > 1) {
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
            }
        )
    }
}

@Composable
private fun CalendarHeader(currentMonth: YearMonth) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}