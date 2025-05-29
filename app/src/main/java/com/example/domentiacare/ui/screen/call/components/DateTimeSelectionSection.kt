package com.example.domentiacare.ui.screen.call.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domentiacare.ui.component.SimpleDropdown
import com.example.domentiacare.ui.screen.call.theme.OrangePrimary
import java.time.YearMonth

@Composable
fun DateTimeSelectionSection(
    selectedYear: String,
    selectedMonth: String,
    selectedDay: String,
    selectedHour: String,
    selectedMinute: String,
    selectedPlace: String,
    onYearChange: (String) -> Unit,
    onMonthChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    years: List<String>,
    months: List<String>,
    hours: List<String>,
    minutes: List<String>
) {
    // 월별 올바른 일수 계산
    val daysInMonth = remember(selectedYear, selectedMonth) {
        val year = selectedYear.toIntOrNull() ?: 2025
        val month = selectedMonth.toIntOrNull() ?: 1
        YearMonth.of(year, month).lengthOfMonth()
    }

    val days = remember(daysInMonth) {
        (1..daysInMonth).map { it.toString().padStart(2, '0') }
    }

    // 선택된 일자가 해당 월의 최대 일수를 초과하는 경우 자동 조정
    LaunchedEffect(daysInMonth, selectedDay) {
        val currentDay = selectedDay.toIntOrNull() ?: 1
        if (currentDay > daysInMonth) {
            onDayChange(daysInMonth.toString().padStart(2, '0'))
        }
    }

    SectionCard(
        title = "일시 및 장소",
        icon = Icons.Default.Schedule
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 날짜 선택
            Text(
                text = "날짜",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1E)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1.5f)) {
                    SimpleDropdown("년", years, selectedYear, onYearChange, Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    SimpleDropdown("월", months, selectedMonth, onMonthChange, Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    SimpleDropdown("일", days, selectedDay, onDayChange, Modifier.fillMaxWidth())
                }
            }

            // 시간 선택
            Text(
                text = "시간",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1E)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SimpleDropdown("시", hours, selectedHour, onHourChange, Modifier.fillMaxWidth())
                }
                Box(modifier = Modifier.weight(1f)) {
                    SimpleDropdown("분", minutes, selectedMinute, onMinuteChange, Modifier.fillMaxWidth())
                }
            }

            // 장소 입력
            Text(
                text = "장소",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1E)
            )

            OutlinedTextField(
                value = selectedPlace,
                onValueChange = onPlaceChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("장소를 입력하세요", color = Color(0xFF9E9E9E)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = OrangePrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E)
                    )
                }
            )
        }
    }
}