package com.example.domentiacare.ui.screen.patientCare

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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun GuardianScheduleScreen(
    scheduleMap: Map<LocalDate, List<String>>, // 날짜별 일정 맵
    onDateSelected: (LocalDate) -> Unit,
    selectedDate: LocalDate,
    scheduleList: List<String>,
    onAddScheduleClick: () -> Unit
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarTitle(currentMonth = visibleMonth)

            Row(  // 요일 표시
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

            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    val hasSchedule = scheduleMap[day.date].orEmpty().isNotEmpty()
                    val isSelected = day.date == selectedDate
                    val isCurrentMonth = day.position == DayPosition.MonthDate //현재달만 표시하기위함
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
                            .clickable { onDateSelected(day.date) },
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
                },
            )

            // 선택된 날짜 표시 개선
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

            // 일정 리스트 개선
            if (scheduleList.isEmpty()) {
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
                    items(scheduleList.withIndex().toList()) { (index, item) ->
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

        // FloatingActionButton을 오버레이로 배치
        FloatingActionButton(
            onClick = onAddScheduleClick,
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