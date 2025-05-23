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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domentiacare.data.model.RecordingFile
import com.example.domentiacare.data.util.convertM4aToWavForWhisper
import com.example.domentiacare.service.whisper.WhisperWrapper
import com.example.domentiacare.ui.component.DMT_Button
import com.example.domentiacare.ui.component.SimpleDropdown
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*


//import LlamaServiceManager
import com.example.domentiacare.MyApplication
import kotlinx.coroutines.launch

@Composable
fun CallDetailScreen(
    filePath: String,
    navController: NavController
) {
    // 예시: 파일명에서 정보 추출, 또는 ViewModel에서 정보를 가져오도록 변경
    val file = remember(filePath) {
        // 파일 객체 생성
        val f = File(filePath)
        RecordingFile(
            name = f.name,
            path = filePath,
            lastModified = f.lastModified(),
            size = f.length()
        )
    }
    // 예시: 파일명에 따라 상대 이름/타입/시간 결정
    val recordLog = RecordLog(
        name = file.name.substringBeforeLast('.'),
        type = "발신", // 예시: 실제 타입 추출 로직으로 대체
        time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified))
    )

    var initialMemo = "" // 일정 분석 후 자동 추가하도록 수정
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

    var transcript by remember { mutableStateOf("") } // 통화 텍스트
    var isLoading by remember { mutableStateOf(false) } // 로딩 여부


    // LlamaServiceManager 연결 상태 확인
    val llamaServiceManager = MyApplication.llamaServiceManager
    var isAnalyzing by remember { mutableStateOf(false) } // 분석 중 여부

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(scrennScrollState) ) {
        // 통화 정보
        Text("${recordLog.name}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("${recordLog.type} ${recordLog.time}", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // 오디오
        Text("통화 녹음", fontWeight = FontWeight.SemiBold)
        AudioPlayerStub()
        Spacer(modifier = Modifier.height(16.dp))

        // 텍스트 대화
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("통화 텍스트", fontWeight = FontWeight.SemiBold)
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            // Whisper 변환 버튼
            DMT_Button(
                text = if (isLoading) "변환중..." else "STT 변환",
                onClick = {
                    Log.d("CallDetailScreen", "STT 변환 버튼 클릭")

                    // 1. wav 변환
                    val m4aFile = File(file.path)
                    val outputDir = File("/sdcard/Recordings/wav/")
                    if (!outputDir.exists()) outputDir.mkdirs()
                    val outputWavFile = File(outputDir, m4aFile.nameWithoutExtension + ".wav")
                    convertM4aToWavForWhisper(m4aFile, outputWavFile)
                    Log.d("RecordingLogItem", "변환 완료: ${outputWavFile.absolutePath}")

                    // 2. Whisper 변환
                    Log.d("Whisper 변환 시작", "파일 경로: ${outputWavFile.absolutePath}")

                    val whisper = WhisperWrapper(context)
                    whisper.copyModelFiles()
                    whisper.initModel()
                    whisper.transcribe(
                        wavPath = outputWavFile.absolutePath,
                        onResult = { result ->
                            transcript = result
                            isLoading = false
                        },
                        onUpdate = { logLine ->
                            // 필요시 중간 상태 로그
                        }
                    )

                    // 3. DB에 저장
                },
                modifier = Modifier
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
        Spacer(modifier = Modifier.height(16.dp))

        // 일정 내용 수정
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("일정", fontWeight = FontWeight.SemiBold)
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            // llama 변환 버튼
            DMT_Button(
                text = if (isAnalyzing) "분석중..." else "일정 분석",
                onClick = {
                    if (isAnalyzing || transcript.isBlank()) return@DMT_Button

                    isAnalyzing = true
                    memo = ""

                    val prompt = """
                        Please analyze the following phone conversation and extract schedule information. Summarize the conversation briefly and output the schedule details in JSON format with exactly these three variables: date, time, place.

                        Phone conversation:
                        "$transcript"

                        Instructions:
                        1. Provide a brief summary of the conversation
                        2. Extract schedule information and format as JSON with these exact keys: "date", "time", "place"
                        3. If multiple times are mentioned, prioritize the main event time
                        4. Output only the summary and JSON, nothing else

                        Format:
                        Summary: [brief description]
                        Schedule: {"date": "YYYY-MM-DD or day description", "time": "HH:MM", "place": "location name"}
                    """.trimIndent()

                    coroutineScope.launch {
                        try {
                            // Llama에게 프롬프트 전송 (부분 결과 실시간 반영)
                            val result = llamaServiceManager.sendQuery(prompt) { partialText ->
                                memo = partialText
                            }
                            // (최종 결과가 memo에 없으면 대입)
                            if (memo.isEmpty()) memo = result

                            // ✅ 여기서 result 혹은 memo에 대해 JSON 파싱을 수행하면 됨
                            // --- [파싱 및 일정 정보 추출은 다음 단계에서 이 위치에 추가] ---
                            // 예시:
                            // val scheduleInfo = parseScheduleJson(result)
                            // memo = scheduleInfo.summary
                            // (필요하면 날짜, 시간, 장소 변수에도 바로 대입)

                        } catch (e: Exception) {
                            memo = "일정 분석 중 오류 발생: ${e.message}"
                        } finally {
                            isAnalyzing = false
                        }
                    }
                },
                modifier = Modifier
            )
        }
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