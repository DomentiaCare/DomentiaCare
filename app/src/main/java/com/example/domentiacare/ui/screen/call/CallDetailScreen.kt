package com.example.domentiacare.ui.screen.call

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.data.model.RecordingFile
import com.example.domentiacare.ui.component.SimpleDropdown
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.data.sync.SimpleSyncManager
import com.example.domentiacare.data.util.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

// 주황색 컬러 팔레트
private val OrangePrimary = Color(0xFFFF6B35)
private val OrangeLight = Color(0xFFFFE5DB)
private val OrangeSecondary = Color(0xFFFF8C42)
private val OrangeDark = Color(0xFFBF2600)

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

    var initialMemo = ""
    var memo by remember { mutableStateOf(initialMemo) }
    var extractedTitle by remember { mutableStateOf("") }

    // 날짜/시간 상태
    var selectedYear by remember { mutableStateOf(LocalDateTime.now().year.toString()) }
    var selectedMonth by remember { mutableStateOf(LocalDateTime.now().monthValue.toString().padStart(2, '0')) }
    var selectedDay by remember { mutableStateOf(LocalDateTime.now().dayOfMonth.toString().padStart(2, '0')) }
    var selectedHour by remember { mutableStateOf(LocalDateTime.now().hour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(LocalDateTime.now().minute.toString().padStart(2, '0')) }
    var selectedPlace by remember { mutableStateOf("") }

    // 상태 변수들
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val years = (2023..2027).map { it.toString() }
    val months = (1..12).map { it.toString().padStart(2, '0') }
    val days = (1..31).map { it.toString().padStart(2, '0') }
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

    // 전체 배경을 흰색으로 설정
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            topBar = {
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
                            onClick = { navController.popBackStack() },
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

                // 통화 정보 헤더
                CallInfoHeader(recordLog = recordLog)

                // 상태 메시지
                if (saveMessage.isNotEmpty()) {
                    StatusMessage(message = saveMessage)
                }

                // 오디오 플레이어
                AudioPlayerSection(filePath = file.path)

                // 통화 텍스트 섹션
                TranscriptSection(
                    transcript = transcript,
                    isLoading = isLoading,
                    onTranscribe = {
                        // STT 변환 로직
                    }
                )

                // 일정 분석 섹션
                ScheduleAnalysisSection(
                    memo = memo,
                    onMemoChange = { memo = it },
                    isAnalyzing = isAnalyzing,
                    onAnalyze = {
                        // 일정 분석 로직
                    }
                )

                // 날짜/시간 선택 섹션
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
                    days = days,
                    hours = hours,
                    minutes = minutes
                )

                // 저장 버튼
                SaveButton(
                    isSaving = isSaving,
                    onSave = {
                        if (extractedTitle.isBlank()) {
                            saveMessage = "일정 제목을 입력해주세요."
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
                                val simpleSchedule = SimpleSchedule(
                                    localId = UUID.randomUUID().toString(),
                                    userId = currentUserId,
                                    title = extractedTitle.ifBlank { "Call Schedule" },
                                    description = "Call recording extracted schedule${if (selectedPlace.isNotEmpty()) " - Location: $selectedPlace" else ""}",
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

                // 하단 여백
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CallInfoHeader(recordLog: RecordLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(OrangeLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recordLog.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1E)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF49454E)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${recordLog.type} • ${recordLog.time}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF49454E)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.contains("성공") || message.contains("✅"))
                Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (message.contains("성공") || message.contains("✅"))
                    Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (message.contains("성공") || message.contains("✅"))
                    Color(0xFF4CAF50) else Color(0xFFD32F2F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.contains("성공") || message.contains("✅"))
                    Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun AudioPlayerSection(filePath: String) {
    SectionCard(
        title = "통화 녹음",
        icon = Icons.Default.GraphicEq
    ) {
        ModernAudioPlayer(filePath = filePath)
    }
}

@Composable
fun TranscriptSection(
    transcript: String,
    isLoading: Boolean,
    onTranscribe: () -> Unit
) {
    SectionCard(
        title = "통화 텍스트",
        icon = Icons.Default.TextFields
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 200.dp)
                .background(
                    Color(0xFFF8F9FA),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            if (transcript.isBlank()) {
                Text(
                    text = "통화 내용이 텍스트로 변환됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1C1B1E),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
fun ScheduleAnalysisSection(
    memo: String,
    onMemoChange: (String) -> Unit,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit
) {
    SectionCard(
        title = "일정 내용",
        icon = Icons.Default.EventNote
    ) {
        OutlinedTextField(
            value = memo,
            onValueChange = onMemoChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("예: 재통화 예정", color = Color(0xFF9E9E9E)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = OrangePrimary,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            minLines = 2,
            maxLines = 4
        )
    }
}

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
    days: List<String>,
    hours: List<String>,
    minutes: List<String>
) {
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
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
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

@Composable
fun SaveButton(
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Button(
        onClick = onSave,
        enabled = !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OrangePrimary,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE0E0E0),
            disabledContentColor = Color(0xFF9E9E9E)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isSaving) "저장 중..." else "일정 저장",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1B1E)
                    )
                }
                actionButton?.invoke()
            }

            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ModernAudioPlayer(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    DisposableEffect(filePath) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                duration = this.duration
            }
            duration = mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            errorMessage = "오디오 파일을 열 수 없습니다: ${e.message}"
        }
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            if (!isSeeking) {
                position = mediaPlayer?.currentPosition ?: 0
                sliderPosition = position.toFloat()
            }
            delay(200)
        }
    }

    fun onSliderValueChange(value: Float) {
        isSeeking = true
        sliderPosition = value
    }

    fun onSliderValueChangeFinished() {
        isSeeking = false
        mediaPlayer?.seekTo(sliderPosition.toInt())
        position = sliderPosition.toInt()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                OrangeLight.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 재생/일시정지 버튼
                IconButton(
                    onClick = {
                        if (mediaPlayer != null) {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                            } else {
                                mediaPlayer?.start()
                            }
                            isPlaying = !isPlaying
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(OrangePrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "일시정지" else "재생",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD32F2F)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(position),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454E)
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454E)
                            )
                        }
                    }
                }
            }

            // 재생바
            if (duration > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderPosition.coerceIn(0f, duration.toFloat()),
                    onValueChange = { onSliderValueChange(it) },
                    onValueChangeFinished = { onSliderValueChangeFinished() },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = OrangePrimary,
                        activeTrackColor = OrangePrimary,
                        inactiveTrackColor = Color(0xFFE0E0E0)
                    )
                )
            }
        }
    }
}

fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

// 나머지 기존 함수들 유지
data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

fun parseLlamaScheduleResponseFull(response: String): Quintuple<String, String, String, String, String> {
    val summaryRegex = Regex("""Summary:\s*(.+)""")
    val scheduleRegex = Regex("""Schedule:\s*(\{[\s\S]*\})""")

    val summary = summaryRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""
    val jsonString = scheduleRegex.find(response)?.groupValues?.get(1)?.trim() ?: "{}"

    Log.d("parseLlama", "Llama response: $response")
    Log.d("parseLlama", "Parsed summary: $summary")
    Log.d("parseLlama", "Parsed jsonString: $jsonString")

    val json = try {
        JSONObject(jsonString)
    } catch (e: Exception) {
        Log.d("parseLlama", "Invalid JSON: $jsonString (${e.message})")
        JSONObject()
    }

    val dateRaw = json.optString("date").trim()
    val timeRaw = json.optString("time").trim()
    val place = json.optString("place").trim()

    Log.d("parseLlama", "Parsed date: $dateRaw, timeRaw: $timeRaw, place: $place")

    val dateString = parseDateSmart(dateRaw)
    val (hour, minute) = parseTimeSmart(timeRaw)

    Log.d("parseLlama", "Final hour: $hour, minute: $minute")

    return Quintuple(summary, dateString, hour, minute, place)
}

fun parseDateSmart(dateRaw: String): String {
    val lower = dateRaw.trim().lowercase(Locale.US)
    val today = LocalDate.now()
    val dowMap = mapOf(
        "sunday" to DayOfWeek.SUNDAY,
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
    )

    try {
        if (Regex("""\d{4}-\d{2}-\d{2}""").matches(lower)) {
            LocalDate.parse(lower, DateTimeFormatter.ISO_LOCAL_DATE).let {
                return it.toString()
            }
        }
    } catch (_: Exception) {}

    when (lower) {
        "today" -> return today.toString()
        "tomorrow" -> return today.plusDays(1).toString()
        "the day after tomorrow", "day after tomorrow" -> return today.plusDays(2).toString()
    }

    dowMap[lower]?.let { targetDOW ->
        var daysToAdd = (targetDOW.value - today.dayOfWeek.value + 7) % 7
        if (daysToAdd == 0) daysToAdd = 7
        return today.plusDays(daysToAdd.toLong()).toString()
    }

    val regexNextDay = Regex("""next\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday)""")
    regexNextDay.find(lower)?.let {
        val targetDOW = dowMap[it.groupValues[1]]!!
        var daysToAdd = (targetDOW.value - today.dayOfWeek.value + 7) % 7
        if (daysToAdd == 0) daysToAdd = 7
        return today.plusDays(daysToAdd.toLong()).toString()
    }

    val regexThisDay = Regex("""this\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday)""")
    regexThisDay.find(lower)?.let {
        val targetDOW = dowMap[it.groupValues[1]]!!
        val todayDow = today.dayOfWeek.value
        val daysToAdd = (targetDOW.value - todayDow + 7) % 7
        return today.plusDays(daysToAdd.toLong()).toString()
    }

    val regexMD1 = Regex("""([a-zA-Z]+)\s+(\d{1,2})""")
    val regexMD2 = Regex("""(\d{1,2})\s+([a-zA-Z]+)""")
    fun monthStrToNum(mon: String): Int = when (mon.lowercase(Locale.US).take(3)) {
        "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6;
        "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
        else -> 0
    }
    regexMD1.find(lower)?.let {
        val month = monthStrToNum(it.groupValues[1])
        val day = it.groupValues[2].toIntOrNull() ?: 1
        if (month in 1..12) return LocalDate.of(today.year, month, day).toString()
    }
    regexMD2.find(lower)?.let {
        val day = it.groupValues[1].toIntOrNull() ?: 1
        val month = monthStrToNum(it.groupValues[2])
        if (month in 1..12) return LocalDate.of(today.year, month, day).toString()
    }

    return dateRaw
}

fun parseTimeSmart(timeRaw: String): Pair<String, String> {
    val cleaned = timeRaw.trim().lowercase(Locale.US)
    Log.d("parseTimeSmart", "입력: '$timeRaw' -> 정리: '$cleaned'")

    if (cleaned.length == 4 && cleaned.all { it.isDigit() }) {
        val result = cleaned.substring(0, 2) to cleaned.substring(2, 4)
        Log.d("parseTimeSmart", "4자리 숫자 파싱: $result")
        return result
    }

    Regex("""(\d{1,2})[\:\-\.](\d{2})""").find(cleaned)?.let { match ->
        val result = match.groupValues[1].padStart(2, '0') to match.groupValues[2].padStart(2, '0')
        Log.d("parseTimeSmart", "구분자 포함 파싱: $result")
        return result
    }

    val ampmPatterns = listOf(
        Regex("""(\d{1,2}):(\d{2})\s*(am|pm)"""),
        Regex("""(\d{1,2})\s*(am|pm)"""),
        Regex("""(\d{1,2}):(\d{2})\s*([ap])"""),
        Regex("""(\d{1,2})\s*([ap])""")
    )

    for (pattern in ampmPatterns) {
        pattern.find(cleaned)?.let { match ->
            var hour = match.groupValues[1].toInt()
            val minute = if (match.groupValues.size > 3 && match.groupValues[2].isNotEmpty()) {
                match.groupValues[2]
            } else {
                "00"
            }
            val ampm = match.groupValues.last().lowercase()

            when {
                ampm.startsWith("p") && hour != 12 -> hour += 12
                ampm.startsWith("a") && hour == 12 -> hour = 0
            }

            val result = hour.toString().padStart(2, '0') to minute.padStart(2, '0')
            Log.d("parseTimeSmart", "AM/PM 파싱: $result (원본: ${match.value})")
            return result
        }
    }

    Regex("""(\d{1,2}):(\d{2})""").find(cleaned)?.let { match ->
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2]
        if (hour in 0..23) {
            val result = hour.toString().padStart(2, '0') to minute.padStart(2, '0')
            Log.d("parseTimeSmart", "24시간 형식 파싱: $result")
            return result
        }
    }

    Regex("""^(\d{1,2})$""").find(cleaned)?.let { match ->
        val hour = match.groupValues[1].toInt()
        if (hour in 0..23) {
            val result = hour.toString().padStart(2, '0') to "00"
            Log.d("parseTimeSmart", "단순 시간 파싱: $result")
            return result
        }
    }

    val engNumMap = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12
    )

    when {
        cleaned.contains("noon") -> {
            Log.d("parseTimeSmart", "noon 파싱: 12:00")
            return "12" to "00"
        }
        cleaned.contains("midnight") -> {
            Log.d("parseTimeSmart", "midnight 파싱: 00:00")
            return "00" to "00"
        }
    }

    Regex("""(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\s*(thirty|fifteen|forty five|o'clock|zero)?""")
        .find(cleaned)?.let { match ->
            val hour = engNumMap[match.groupValues[1]]?.toString()?.padStart(2, '0') ?: ""
            val min = when {
                cleaned.contains("thirty") -> "30"
                cleaned.contains("fifteen") -> "15"
                cleaned.contains("forty five") -> "45"
                cleaned.contains("zero") || cleaned.contains("o'clock") -> "00"
                else -> "00"
            }
            val result = hour to min
            Log.d("parseTimeSmart", "영어 숫자+분 파싱: $result")
            return result
        }

    Regex("""(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)(?:\s*(am|pm|a|p))?""")
        .find(cleaned)?.let { match ->
            var hour = engNumMap[match.groupValues[1]] ?: 0
            val ampm = match.groupValues[2].lowercase()

            when {
                ampm.startsWith("p") && hour != 12 -> hour += 12
                ampm.startsWith("a") && hour == 12 -> hour = 0
            }

            val result = hour.toString().padStart(2, '0') to "00"
            Log.d("parseTimeSmart", "영어 숫자+AM/PM 파싱: $result")
            return result
        }

    val colloquialTimes = mapOf(
        "morning" to ("09" to "00"),
        "afternoon" to ("14" to "00"),
        "evening" to ("18" to "00"),
        "night" to ("20" to "00"),
        "early morning" to ("07" to "00"),
        "late morning" to ("11" to "00"),
        "early afternoon" to ("13" to "00"),
        "late afternoon" to ("16" to "00"),
        "early evening" to ("17" to "00"),
        "late evening" to ("21" to "00")
    )

    for ((phrase, time) in colloquialTimes) {
        if (cleaned.contains(phrase)) {
            Log.d("parseTimeSmart", "구어체 표현 파싱: $time (원본: $phrase)")
            return time
        }
    }

    Log.d("parseTimeSmart", "파싱 실패: '$timeRaw' -> 빈 값 반환")
    return "" to ""
}

fun parseDateToLocalDate(dateString: String): LocalDate {
    val now = LocalDate.now()
    val dayOfWeekMap = mapOf(
        "sunday" to DayOfWeek.SUNDAY,
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
    )

    try {
        return LocalDate.parse(dateString)
    } catch (_: Exception) { }

    val lower = dateString.trim().lowercase()

    val regexNextThis = Regex("""(next|this)\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday)""")
    regexNextThis.find(lower)?.let {
        val mode = it.groupValues[1]
        val dow = dayOfWeekMap[it.groupValues[2]] ?: return now
        val todayDow = now.dayOfWeek.value

        return when (mode) {
            "next" -> {
                var daysToAdd = (dow.value - todayDow + 7) % 7
                if (daysToAdd == 0) daysToAdd = 7
                now.plusDays(daysToAdd.toLong())
            }
            "this" -> {
                val daysToAdd = (dow.value - todayDow + 7) % 7
                now.plusDays(daysToAdd.toLong())
            }
            else -> now
        }
    }

    dayOfWeekMap[lower]?.let { dow ->
        var daysToAdd = (dow.value - now.dayOfWeek.value + 7) % 7
        if (daysToAdd == 0) daysToAdd = 7
        return now.plusDays(daysToAdd.toLong())
    }

    return now
}