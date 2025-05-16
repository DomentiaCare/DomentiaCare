package com.example.domentiacare.ui.screen.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domentiacare.data.remote.dto.Schedule
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
@Composable
fun PagerCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    schedules: List<Schedule>
) {
    val today = LocalDate.now()
    val initialPage = 1000
    val pagerState = rememberPagerState(initialPage = initialPage)
    val coroutineScope = rememberCoroutineScope()

    // ✅ 현재 월 계산
    val currentMonth = YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())

    // ▶️◀️ 월 이동 버튼 + 현재 월 표시
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
        }
        Text(
            text = "${currentMonth.year}년 ${currentMonth.monthValue}월",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
        }
    }
    
    // 👉 HorizontalPager (스와이프 달력)
    HorizontalPager(
        count = Int.MAX_VALUE,
        state = pagerState
    ) { page ->
        val monthForPage = YearMonth.now().plusMonths((page - initialPage).toLong())
        SingleMonthCalendar(
            currentMonth = monthForPage,
            today = today,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            schedules = schedules
        )
    }
}