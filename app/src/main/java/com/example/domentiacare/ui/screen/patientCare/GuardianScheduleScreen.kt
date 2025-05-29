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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
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
                        .background(if (isSelected) Color(0xFF1976D2) else Color.Transparent)
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
                                    .background(Color.Blue, shape = CircleShape)
                            )
                        }
                    }
                }
            },
        )

        Text(
            text = selectedDate.toString(),
            modifier = Modifier.padding(8.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(scheduleList) { item ->
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = onAddScheduleClick,
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp),
            containerColor = Color(0xFFF49000)
        ) {
            Text("+")
        }
    }
}

@Composable
fun CalendarTitle(currentMonth: YearMonth) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF607D8B))
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
