package com.example.domentiacare.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domentiacare.data.remote.dto.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CustomCalendar(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    schedules: List<Schedule>
) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()

    // ✅ 일요일 기준으로 날짜 시작 맞춤
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.ordinal + 7 - DayOfWeek.SUNDAY.ordinal) % 7
    val totalDays = firstDayOfWeek + lastDayOfMonth.dayOfMonth

    val dates = (0 until totalDays).map { index ->
        if (index < firstDayOfWeek) null
        else currentMonth.atDay(index - firstDayOfWeek + 1)
    }

    // ✅ 한글 요일 표시
    val koreanDaysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(modifier = modifier.padding(16.dp)) {

        // 월 변경 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
            }
            Text(
                text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ 요일 헤더 (한글 + 색상 적용)
        Row(modifier = Modifier.fillMaxWidth()) {
            koreanDaysOfWeek.forEachIndexed { index, day ->
                val color = when (index) {
                    0 -> Color(0xFFD32F2F) // 일요일
                    6 -> Color(0xFF1976D2) // 토요일
                    else -> Color.Unspecified
                }
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 날짜 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxHeight() // 달력이 아래까지
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dates) { date ->
                val daySchedule = schedules.find { it.date == date }
                val dayOfWeek = date?.dayOfWeek
                val textColor = when (dayOfWeek) {
                    DayOfWeek.SUNDAY -> if (date == selectedDate) Color.White else Color(0xFFD32F2F)
                    DayOfWeek.SATURDAY -> if (date == selectedDate) Color.White else Color(0xFF1976D2)
                    else -> if (date == selectedDate) Color.White else Color.Black
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.6f) // ✅ 정사각형보다 세로로 길게
                        .background(
                            color = when {
                                date == selectedDate -> Color(0xFF2196F3)
                                date == today -> Color(0xFFBBDEFB)
                                else -> Color.Transparent
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable(enabled = date != null) {
                            date?.let {
                                selectedDate = it
                                onDateSelected(it)
                            }
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = date?.dayOfMonth?.toString() ?: "",
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // ✅ 일정 있는 경우만 라운드된 박스로 표시
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (daySchedule != null) Color(0xFF4C8C4A) else Color.Transparent,
                                    shape = MaterialTheme.shapes.small // ✅ 둥근 모서리 적용
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (daySchedule != null) {
                                Text(
                                    text = daySchedule.content,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}
