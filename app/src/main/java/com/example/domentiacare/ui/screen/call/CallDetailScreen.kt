package com.example.domentiacare.ui.screen.call

import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.domentiacare.data.model.CallDetailViewModel
import com.example.domentiacare.data.model.RecordingFile
import com.example.domentiacare.data.util.convertM4aToWavForWhisper
import com.example.domentiacare.service.whisper.WhisperWrapper
import com.example.domentiacare.ui.component.DMT_Button
import com.example.domentiacare.ui.component.SimpleDropdown
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
@Composable
fun CallDetailScreen(
    filePath: String,
    navController: NavController,
    viewModel: CallDetailViewModel = hiltViewModel() // Hilt로 ViewModel 주입
) {
    // ViewModel에서 상태 수집
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 초기 데이터 로드
    LaunchedEffect(filePath) {
        viewModel.loadRecordingFile(filePath)
    }

    // 저장 상태에 따른 처리
    LaunchedEffect(uiState.saveStatus) {
        when (uiState.saveStatus) {
            CallDetailViewModel.SaveStatus.SUCCESS -> {
                // 성공 메시지 표시
                Toast.makeText(context, "저장되었습니다", Toast.LENGTH_SHORT).show()
                navController.popBackStack() // 이전 화면으로 돌아가기
            }
            CallDetailViewModel.SaveStatus.ERROR -> {
                // 에러 메시지 표시
                Toast.makeText(context, "저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
            else -> { /* 다른 상태 처리 */ }
        }
    }

    // UI 렌더링
    CallDetailContent(
        recordLog = uiState.recordLog,
        transcript = uiState.transcript,
        memo = uiState.memo,
        selectedDateTime = uiState.selectedDateTime,
        isLoading = uiState.isLoading,
        onTranscribeClick = { viewModel.transcribeAudio(context) },
        onAnalyzeClick = { viewModel.analyzeSchedule() },
        onMemoChange = { viewModel.updateMemo(it) },
        onDateTimeChange = { year, month, day, hour, minute ->
            viewModel.updateDateTime(year, month, day, hour, minute)
        },
        onSaveClick = { viewModel.saveData() }
    )
}

@Composable
fun CallDetailContent(
    recordLog: RecordLog?,
    transcript: String,
    memo: String,
    selectedDateTime: LocalDateTime,
    isLoading: Boolean,
    onTranscribeClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    onDateTimeChange: (Int, Int, Int, Int, Int) -> Unit,
    onSaveClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val screenScrollState = rememberScrollState()

    // 날짜 선택용 상태들
    var selectedYear by remember { mutableStateOf(selectedDateTime.year.toString()) }
    var selectedMonth by remember { mutableStateOf(selectedDateTime.monthValue.toString().padStart(2, '0')) }
    var selectedDay by remember { mutableStateOf(selectedDateTime.dayOfMonth.toString().padStart(2, '0')) }
    var selectedHour by remember { mutableStateOf(selectedDateTime.hour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(selectedDateTime.minute.toString().padStart(2, '0')) }

    // 날짜 옵션 리스트
    val years = (2023..2027).map { it.toString() }
    val months = (1..12).map { it.toString().padStart(2, '0') }
    val days = (1..31).map { it.toString().padStart(2, '0') }
    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    // 날짜 변경 감지 및 콜백 호출
    LaunchedEffect(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute) {
        onDateTimeChange(
            selectedYear.toInt(),
            selectedMonth.toInt(),
            selectedDay.toInt(),
            selectedHour.toInt(),
            selectedMinute.toInt()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(screenScrollState)
    ) {
        // 통화 정보
        recordLog?.let {
            Text(it.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("${it.type} ${it.time}", fontSize = 14.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 오디오 플레이어
        Text("통화 녹음", fontWeight = FontWeight.SemiBold)
        AudioPlayerComposable()
        Spacer(modifier = Modifier.height(16.dp))

        // 텍스트 대화
        TranscriptSection(
            transcript = transcript,
            isLoading = isLoading,
            onTranscribeClick = onTranscribeClick
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 일정 분석
        MemoSection(
            memo = memo,
            isLoading = isLoading,
            onMemoChange = onMemoChange,
            onAnalyzeClick = onAnalyzeClick
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 날짜 시간 선택
        DateTimeSelection(
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedDay = selectedDay,
            selectedHour = selectedHour,
            selectedMinute = selectedMinute,
            onYearChange = { selectedYear = it },
            onMonthChange = { selectedMonth = it },
            onDayChange = { selectedDay = it },
            onHourChange = { selectedHour = it },
            onMinuteChange = { selectedMinute = it },
            years = years,
            months = months,
            days = days,
            hours = hours,
            minutes = minutes
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 저장 버튼
        DMT_Button(
            text = "저장",
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
    }
}

@Composable
fun TranscriptSection(
    transcript: String,
    isLoading: Boolean,
    onTranscribeClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("통화 텍스트", fontWeight = FontWeight.SemiBold)
        DMT_Button(
            text = if (isLoading) "변환중..." else "STT 변환",
            onClick = onTranscribeClick,
            enabled = !isLoading
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp, max = 100.dp)
            .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Text(transcript, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Composable
fun MemoSection(
    memo: String,
    isLoading: Boolean,
    onMemoChange: (String) -> Unit,
    onAnalyzeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("일정", fontWeight = FontWeight.SemiBold)
        DMT_Button(
            text = "일정 분석",
            onClick = onAnalyzeClick,
            enabled = !isLoading
        )
    }

    OutlinedTextField(
        value = memo,
        onValueChange = onMemoChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("예: 재통화 예정") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFF49000)
        )
    )
}

@Composable
fun DateTimeSelection(
    selectedYear: String,
    selectedMonth: String,
    selectedDay: String,
    selectedHour: String,
    selectedMinute: String,
    onYearChange: (String) -> Unit,
    onMonthChange: (String) -> Unit,
    onDayChange: (String) -> Unit,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    years: List<String>,
    months: List<String>,
    days: List<String>,
    hours: List<String>,
    minutes: List<String>
) {
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
                onYearChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SimpleDropdown(
                "월",
                months,
                selectedMonth,
                onMonthChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SimpleDropdown(
                "일",
                days,
                selectedDay,
                onDayChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SimpleDropdown(
                "시",
                hours,
                selectedHour,
                onHourChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SimpleDropdown(
                "분",
                minutes,
                selectedMinute,
                onMinuteChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AudioPlayerComposable() {
    // 기존 AudioPlayerStub을 그대로 사용
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