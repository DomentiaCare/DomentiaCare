package com.example.domentiacare.ui.screen.call

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domentiacare.ui.component.DMT_Button
import com.example.domentiacare.ui.component.SimpleDropdown
import java.time.LocalDateTime

@Composable
fun CallDetailScreen(
    navController: NavController
) {
    var callLog = CallLog(
        name = "밤지성",
        type = "발신",
        time = "오전 11:15"
    )
    var transcript = "진호: 이번 주말에 시간 어때?\n" +
            "소룡: 토요일은 좀 힘들고, 일요일 오후는 괜찮아. 너는?\n" +
            "진호: 나도 일요일 오후 괜찮아. 3시쯤 어때?\n" +
            "소룡: 좋아. 어디서 볼까?\n" +
            "진호: 홍대 쪽 어때? 카페도 많고 걷기도 좋고.\n" +
            "소룡: 좋지! 그럼 일요일 3시에 홍대에서 보자!\n" +
            "진호: 오케이~ 그때 보자!"
    var initialMemo = "홍대 약속"
    var initialDate = LocalDateTime.now()

    var memo by remember { mutableStateOf(initialMemo) }
    var selectedYear by remember { mutableStateOf(LocalDateTime.now().year.toString()) }
    var selectedMonth by remember { mutableStateOf(LocalDateTime.now().monthValue.toString().padStart(2, '0')) }
    var selectedDay by remember { mutableStateOf(LocalDateTime.now().dayOfMonth.toString().padStart(2, '0')) }
    var selectedHour by remember { mutableStateOf(LocalDateTime.now().hour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(LocalDateTime.now().minute.toString().padStart(2, '0')) }


    val years = (2023..2027).map { it.toString() }
    val months = (1..12).map { it.toString().padStart(2, '0') }
    val days = (1..31).map { it.toString().padStart(2, '0') }
    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    val scrollState = rememberScrollState()  //일정 박스 스크롤
    val scrennScrollState = rememberScrollState()  //화면 스크롤

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(scrennScrollState) ) {
        // 통화 정보
        Text("통화 상대: ${callLog.name}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("${callLog.type} ${callLog.time}", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // 오디오
        Text("통화 녹음", fontWeight = FontWeight.SemiBold)
        AudioPlayerStub()
        Spacer(modifier = Modifier.height(16.dp))

        // 텍스트 대화
        Text("통화 텍스트", fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp, max = 200.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text(transcript, fontSize = 14.sp, color = Color.Black)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 일정 내용 수정
        Text("일정", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("예: 재통화 예정") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFF49000)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 날짜 시간 선택
        Text("일시", fontWeight = FontWeight.SemiBold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown(
                    "년",
                    years,
                    selectedYear,
                    { selectedYear = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown(
                    "월",
                    months,
                    selectedMonth,
                    { selectedMonth = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown("일", days, selectedDay, { selectedDay = it }, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown("시", hours, selectedHour, { selectedHour = it }, modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.weight(1f)) {
                SimpleDropdown("분", minutes, selectedMinute, { selectedMinute = it }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 저장 버튼
        DMT_Button(
            text = "저장",
            onClick = {
                val selectedDateTime = LocalDateTime.of(
                    selectedYear.toInt(),
                    selectedMonth.toInt(),
                    selectedDay.toInt(),
                    selectedHour.toInt(),
                    selectedMinute.toInt()
                )
                //onSave(memo, selectedDateTime)
                Log.d("CallDetailScreen", "저장된 메모: $memo 일시: $selectedDateTime ")
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
    }


@Composable
fun AudioPlayerStub() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFfef7e7))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "재생")
            Spacer(modifier = Modifier.width(8.dp))
            Text("0:00 / 5:12")
        }
    }
}