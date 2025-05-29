package com.example.domentiacare.ui.screen.call

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.data.model.RecordingFile
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.data.sync.SimpleSyncManager
import com.example.domentiacare.data.util.UserPreferences
import com.example.domentiacare.ui.screen.call.components.*
import com.example.domentiacare.ui.screen.call.theme.OrangeLight
import com.example.domentiacare.ui.screen.call.theme.OrangePrimary
import com.example.domentiacare.data.util.convertM4aToWavForWhisper
import com.example.domentiacare.service.whisper.WhisperWrapper
import com.example.domentiacare.MyApplication
import com.example.domentiacare.ui.screen.call.utils.DateTimeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    filePath: String,
    navController: NavController
) {
    val file = remember(filePath) {
        val f = File(filePath)
        RecordingFile(
            name = f.name,
            path = filePath,
            lastModified = f.lastModified(),
            size = f.length()
        )
    }

    val recordLog = RecordLog(
        name = file.name.substringBeforeLast('.'),
        type = "발신",
        time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified))
    )

    // State variables
    var memo by remember { mutableStateOf("") }
    var extractedTitle by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf(LocalDateTime.now().year.toString()) }
    var selectedMonth by remember { mutableStateOf(LocalDateTime.now().monthValue.toString().padStart(2, '0')) }
    var selectedDay by remember { mutableStateOf(LocalDateTime.now().dayOfMonth.toString().padStart(2, '0')) }
    var selectedHour by remember { mutableStateOf(LocalDateTime.now().hour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(LocalDateTime.now().minute.toString().padStart(2, '0')) }
    var selectedPlace by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val years = (2023..2027).map { it.toString() }
    val months = (1..12).map { it.toString().padStart(2, '0') }
    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUserId = remember {
        val savedUserId = UserPreferences.getUserId(context)
        Log.d("CallDetailScreen", "현재 사용자 ID: $savedUserId")
        if (savedUserId > 0) savedUserId else 6L
    }

    // Llama 분석 함수 정의
    val startLlamaAnalysis = startLlamaAnalysis@{ transcriptText: String ->
        if (isAnalyzing) return@startLlamaAnalysis

        isAnalyzing = true
        memo = ""
        extractedTitle = ""
        var lastParsedResult = ""

        val prompt = """
            Please analyze the following phone conversation and extract schedule information.
            Output only two sections in the following format. **Do NOT use Markdown or any formatting.**
            Summary: [A representative title for the schedule, extracted from the conversation.]
            Schedule: {"date": "YYYY-MM-DD or day description", "time": "HH:MM", "place": "location name"}

            Instructions:
            1. Extract a representative title for this conversation that can be used as a schedule title. Output as 'Summary'.
            2. Extract schedule information in JSON format with exactly these keys: "date", "time", "place".
            3. If multiple times are mentioned, prioritize the main event time.
            4. Output only the summary and JSON, nothing else.

            Phone conversation:
            "$transcriptText"
        """.trimIndent()

        coroutineScope.launch {
            try {
                val llamaServiceManager = MyApplication.llamaServiceManager
                val result = llamaServiceManager.sendQuery(prompt) { partialText ->
                    memo = partialText

                    if (partialText.contains("Summary:") && partialText.contains("Schedule:")) {
                        val (title, _, _, _, _) = DateTimeParser.parseLlamaScheduleResponseFull(partialText)
                        if (title.isNotBlank()) {
                            extractedTitle = title
                        }
                    }

                    // 부분 응답에서도 완성된 Schedule이면 파싱
                    if (
                        partialText.contains("Schedule:") &&
                        partialText.trim().endsWith("}") &&
                        lastParsedResult != partialText
                    ) {
                        lastParsedResult = partialText
                        val (title, date, hour, minute, place) = DateTimeParser.parseLlamaScheduleResponseFull(partialText)
                        coroutineScope.launch(Dispatchers.Main) {
                            extractedTitle = title
                            val localDate = DateTimeParser.parseDateToLocalDate(date)
                            selectedYear = localDate.year.toString()
                            selectedMonth = localDate.monthValue.toString().padStart(2, '0')
                            selectedDay = localDate.dayOfMonth.toString().padStart(2, '0')
                            selectedHour = hour.padStart(2, '0')
                            selectedMinute = minute.padStart(2, '0')
                            selectedPlace = place
                        }
                    }
                }

                Log.d("AutoWorkflow", "Llama 분석 완료: $result")

                // 최종 응답에서도 체크
                if (
                    result.contains("Schedule:") &&
                    result.trim().endsWith("}") &&
                    lastParsedResult != result
                ) {
                    lastParsedResult = result
                    val (title, date, hour, minute, place) = DateTimeParser.parseLlamaScheduleResponseFull(result)
                    withContext(Dispatchers.Main) {
                        extractedTitle = title
                        val localDate = DateTimeParser.parseDateToLocalDate(date)
                        selectedYear = localDate.year.toString()
                        selectedMonth = localDate.monthValue.toString().padStart(2, '0')
                        selectedDay = localDate.dayOfMonth.toString().padStart(2, '0')
                        selectedHour = hour.padStart(2, '0')
                        selectedMinute = minute.padStart(2, '0')
                        selectedPlace = place
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    memo = "일정 분석 중 오류 발생: ${e.message}"
                    Log.e("AutoWorkflow", "일정 분석 실패", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isAnalyzing = false
                }
            }
        }
    }

    // 자동 워크플로우 실행
    LaunchedEffect(file.path) {
        // 1단계: 자동 STT 변환 시작
        isLoading = true

        withContext(Dispatchers.IO) {
            try {
                // M4A → WAV 변환
                val m4aFile = File(file.path)
                val outputDir = File("/sdcard/Recordings/wav/")
                if (!outputDir.exists()) outputDir.mkdirs()
                val outputWavFile = File(outputDir, m4aFile.nameWithoutExtension + ".wav")

                convertM4aToWavForWhisper(m4aFile, outputWavFile)
                Log.d("AutoWorkflow", "WAV 변환 완료: ${outputWavFile.absolutePath}")

                // Whisper 모델 초기화 및 변환
                val whisper = WhisperWrapper(context)
                whisper.copyModelFiles()
                whisper.initModel()

                whisper.transcribe(
                    wavPath = outputWavFile.absolutePath,
                    onResult = { result ->
                        transcript = result
                        isLoading = false
                        Log.d("AutoWorkflow", "STT 완료: $result")

                        // WAV 파일 정리
                        try {
                            if (outputWavFile.exists()) {
                                val deleted = outputWavFile.delete()
                                if (deleted) {
                                    Log.d("AutoWorkflow", "✅ WAV 파일 삭제 성공")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AutoWorkflow", "WAV 파일 삭제 오류: ${e.message}")
                        }

                        // 2단계: STT 완료 후 자동으로 Llama 분석 시작
                        if (result.isNotBlank()) {
                            coroutineScope.launch {
                                delay(500) // 잠시 대기 후 분석 시작
                                startLlamaAnalysis(result)
                            }
                        }
                    },
                    onUpdate = { /* 진행 상황 업데이트 */ }
                )

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AutoWorkflow", "STT 실패", e)
                    saveMessage = "❌ 음성 변환 실패: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            topBar = {
                CallDetailTopBar(
                    onBackClick = { navController.popBackStack() }
                )
            },
            containerColor = Color.White
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                CallInfoHeader(recordLog = recordLog)

                if (saveMessage.isNotEmpty()) {
                    StatusMessage(message = saveMessage)
                }

                AudioPlayerSection(filePath = file.path)

                TranscriptSection(
                    transcript = transcript,
                    isLoading = isLoading,
                    onTranscribe = { /* 수동 버튼은 남겨두되, 자동 실행되므로 선택적 사용 */ }
                )

                ScheduleAnalysisSection(
                    memo = memo,
                    onMemoChange = { memo = it },
                    isAnalyzing = isAnalyzing,
                    transcript = transcript,
                    onAnalyze = { /* 수동 버튼은 남겨두되, 자동 실행되므로 선택적 사용 */ }
                )

                DateTimeSelectionSection(
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    selectedDay = selectedDay,
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute,
                    selectedPlace = selectedPlace,
                    onYearChange = { selectedYear = it },
                    onMonthChange = { selectedMonth = it },
                    onDayChange = { selectedDay = it },
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it },
                    onPlaceChange = { selectedPlace = it },
                    years = years,
                    months = months,
                    hours = hours,
                    minutes = minutes
                )

                SaveButton(
                    isSaving = isSaving,
                    onSave = {
                        // 입력 검증
                        if (extractedTitle.isBlank() && memo.isBlank()) {
                            saveMessage = "일정 제목 또는 내용을 입력해주세요."
                            return@SaveButton
                        }

                        if (currentUserId <= 0) {
                            saveMessage = "사용자 정보를 확인할 수 없습니다."
                            return@SaveButton
                        }

                        val selectedDateTime = LocalDateTime.of(
                            selectedYear.toInt(),
                            selectedMonth.toInt(),
                            selectedDay.toInt(),
                            selectedHour.toInt(),
                            selectedMinute.toInt()
                        )

                        if (selectedDateTime.isBefore(LocalDateTime.now())) {
                            saveMessage = "과거 시간으로는 일정을 생성할 수 없습니다."
                            return@SaveButton
                        }

                        isSaving = true
                        saveMessage = ""

                        coroutineScope.launch {
                            try {
                                val finalTitle = extractedTitle.ifBlank { memo.ifBlank { "통화 일정" } }
                                val finalDescription = if (memo.isNotBlank()) {
                                    "통화 녹음에서 추출된 일정${if (selectedPlace.isNotEmpty()) " - 장소: $selectedPlace" else ""}"
                                } else {
                                    "Call recording extracted schedule${if (selectedPlace.isNotEmpty()) " - Location: $selectedPlace" else ""}"
                                }

                                val simpleSchedule = SimpleSchedule(
                                    localId = UUID.randomUUID().toString(),
                                    userId = currentUserId,
                                    title = finalTitle,
                                    description = finalDescription,
                                    startDate = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                                    endDate = selectedDateTime.plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                                    isAi = true
                                )

                                val syncManager = SimpleSyncManager.getInstance(context)
                                val result = syncManager.saveSchedule(simpleSchedule)

                                withContext(Dispatchers.Main) {
                                    if (result.isSuccess) {
                                        saveMessage = "✅ 일정이 저장되었습니다."
                                        Log.d("CallDetailScreen", "로컬 저장 성공: ${result.getOrNull()?.localId}")
                                        delay(1500)
                                        navController.popBackStack()
                                    } else {
                                        saveMessage = "❌ 저장 실패: ${result.exceptionOrNull()?.message}"
                                        Log.e("CallDetailScreen", "저장 실패", result.exceptionOrNull())
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    saveMessage = "❌ 저장 중 오류 발생: ${e.message}"
                                    Log.e("CallDetailScreen", "저장 예외", e)
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isSaving = false
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallDetailTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "통화 상세",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1E)
                )
                Text(
                    text = "일정 생성 및 관리",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454E)
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OrangeLight.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = OrangePrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF1C1B1E)
        ),
        modifier = Modifier.statusBarsPadding()
    )
}