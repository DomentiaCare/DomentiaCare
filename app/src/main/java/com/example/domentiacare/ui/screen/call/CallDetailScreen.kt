package com.example.domentiacare.ui.screen.call

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.domentiacare.MyApplication
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.data.sync.SimpleSyncManager
import com.example.domentiacare.network.ScheduleApiService
import com.example.domentiacare.data.util.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

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
    var initialDate = LocalDateTime.now()

    // 일정 제목/메모
    var memo by remember { mutableStateOf(initialMemo) }

    // 날짜/시간 드롭다운 State
    var selectedYear by remember { mutableStateOf(LocalDateTime.now().year.toString()) }
    var selectedMonth by remember { mutableStateOf(LocalDateTime.now().monthValue.toString().padStart(2, '0')) }
    var selectedDay by remember { mutableStateOf(LocalDateTime.now().dayOfMonth.toString().padStart(2, '0')) }
    var selectedHour by remember { mutableStateOf(LocalDateTime.now().hour.toString().padStart(2, '0')) }
    var selectedMinute by remember { mutableStateOf(LocalDateTime.now().minute.toString().padStart(2, '0')) }
    var selectedPlace by remember { mutableStateOf("") }

    // 저장 상태
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }

    val years = (2023..2027).map { it.toString() }
    val months = (1..12).map { it.toString().padStart(2, '0') }
    val days = (1..31).map { it.toString().padStart(2, '0') }
    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    val scrollState = rememberScrollState()
    val scrennScrollState = rememberScrollState()

    var transcript by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val llamaServiceManager = MyApplication.llamaServiceManager
    var isAnalyzing by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 현재 사용자 ID를 미리 가져와서 저장 (Swagger 테스트에서 사용한 userId: 6)
    val currentUserId = remember {
        val savedUserId = UserPreferences.getUserId(context)
        if (savedUserId > 0) savedUserId else 6L // Swagger에서 성공한 userId 사용
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrennScrollState)
    ) {
        // 통화 정보
        Text("${recordLog.name}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("${recordLog.type} ${recordLog.time}", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // 오디오
        Text("통화 녹음", fontWeight = FontWeight.SemiBold)
        AudioPlayer(file.path)
        Spacer(modifier = Modifier.height(16.dp))

        // 텍스트 대화
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("통화 텍스트", fontWeight = FontWeight.SemiBold)

            // Whisper 변환 버튼
            DMT_Button(
                text = if (isLoading) "변환중..." else "STT 변환",
                onClick = {
                    Log.d("CallDetailScreen", "STT 변환 버튼 클릭")
                    val m4aFile = File(file.path)
                    val outputDir = File("/sdcard/Recordings/wav/")
                    if (!outputDir.exists()) outputDir.mkdirs()
                    val outputWavFile = File(outputDir, m4aFile.nameWithoutExtension + ".wav")
                    convertM4aToWavForWhisper(m4aFile, outputWavFile)
                    Log.d("RecordingLogItem", "변환 완료: ${outputWavFile.absolutePath}")

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
                        onUpdate = { }
                    )
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

            // llama 변환 버튼
            DMT_Button(
                text = if (isAnalyzing) "분석중..." else "일정 분석",
                onClick = {
                    if (isAnalyzing || transcript.isBlank()) return@DMT_Button

                    isAnalyzing = true
                    memo = ""
                    var lastParsedResult = ""

                    val prompt = """
                        Please analyze the following phone conversation and extract schedule information.
                        Output only two sections in the following format. . **Do NOT use Markdown or any formatting.**
                        Summary: [A representative title for the schedule, extracted from the conversation.]
                        Schedule: {"date": "YYYY-MM-DD or day description", "time": "HH:MM", "place": "location name"}
            
                        Instructions:
                        1. Extract a representative title for this conversation that can be used as a schedule title. Output as 'Summary'.
                        2. Extract schedule information in JSON format with exactly these keys: "date", "time", "place".
                        3. If multiple times are mentioned, prioritize the main event time.
                        4. Output only the summary and JSON, nothing else.
            
                        Phone conversation:
                        "$transcript"
                    """.trimIndent()

                    coroutineScope.launch {
                        try {
                            val result = llamaServiceManager.sendQuery(prompt) { partialText ->
                                memo = partialText
                                // 부분 응답에서도 완성된 Schedule이면 파싱
                                if (
                                    partialText.contains("Schedule:") &&
                                    partialText.trim().endsWith("}") &&
                                    lastParsedResult != partialText
                                ) {
                                    lastParsedResult = partialText
                                    val (title, date, hour, minute, place) = parseLlamaScheduleResponseFull(partialText)
                                    coroutineScope.launch(Dispatchers.Main) {
                                        memo = title
                                        val localDate = parseDateToLocalDate(date)
                                        selectedYear = localDate.year.toString()
                                        selectedMonth = localDate.monthValue.toString().padStart(2, '0')
                                        selectedDay = localDate.dayOfMonth.toString().padStart(2, '0')
                                        selectedHour = hour.padStart(2, '0')
                                        selectedMinute = minute.padStart(2, '0')
                                        selectedPlace = place
                                    }
                                }
                            }

                            Log.d("Llama-final-result", result)

                            // 최종 응답에서도 체크
                            if (
                                result.contains("Schedule:") &&
                                result.trim().endsWith("}") &&
                                lastParsedResult != result
                            ) {
                                lastParsedResult = result
                                val (title, date, hour, minute, place) = parseLlamaScheduleResponseFull(result)
                                withContext(Dispatchers.Main) {
                                    memo = title
                                    val localDate = parseDateToLocalDate(date)
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
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isAnalyzing = false
                            }
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
            Box(modifier = Modifier.weight(1.5f)) {
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

        Spacer(modifier = Modifier.height(8.dp))
        // 장소 필드 자동 반영
        OutlinedTextField(
            value = selectedPlace,
            onValueChange = { selectedPlace = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("장소 입력") },
            label = { Text("장소") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFF49000)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 저장 메시지 표시
        if (saveMessage.isNotEmpty()) {
            Text(
                text = saveMessage,
                color = if (saveMessage.contains("성공")) Color.Green else Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 저장 버튼
        DMT_Button(
            text = if (isSaving) "저장중..." else "저장",
            onClick = {
                // 입력 검증 강화
                if (memo.isBlank()) {
                    saveMessage = "일정 제목을 입력해주세요."
                    return@DMT_Button
                }

                // 유효한 사용자 ID 확인
                if (currentUserId <= 0) {
                    saveMessage = "사용자 정보를 확인할 수 없습니다."
                    return@DMT_Button
                }

                val selectedDateTime = LocalDateTime.of(
                    selectedYear.toInt(),
                    selectedMonth.toInt(),
                    selectedDay.toInt(),
                    selectedHour.toInt(),
                    selectedMinute.toInt()
                )

                // 현재 시간보다 이전 시간인지 체크
                if (selectedDateTime.isBefore(LocalDateTime.now())) {
                    saveMessage = "과거 시간으로는 일정을 생성할 수 없습니다."
                    return@DMT_Button
                }

                // 간단한 Offline-First 저장 로직 (Room 대신 SharedPreferences 사용)
                isSaving = true
                saveMessage = ""

                coroutineScope.launch {
                    try {
                        // 간단한 일정 객체 생성
                        val simpleSchedule = SimpleSchedule(
                            localId = UUID.randomUUID().toString(),
                            userId = currentUserId,
                            title = if (memo.isBlank()) "Call Schedule" else memo,
                            description = "Call recording extracted schedule${if (selectedPlace.isNotEmpty()) " - Location: $selectedPlace" else ""}",
                            startDate = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                            endDate = selectedDateTime.plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                            isAi = true
                        )

                        // SimpleSyncManager를 통한 Offline-First 저장
                        val syncManager = SimpleSyncManager.getInstance(context)
                        val result = syncManager.saveSchedule(simpleSchedule)

                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                saveMessage = "✅ 일정이 저장되었습니다."
                                Log.d("CallDetailScreen", "로컬 저장 성공: ${result.getOrNull()?.localId}")

                                // 성공 후 이전 화면으로 이동
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
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )
    }
}

// 현재 사용자 ID를 가져오는 함수
private fun getCurrentUserId(context: android.content.Context): Long {
    return UserPreferences.getUserId(context)
}

// 나머지 함수들은 동일하게 유지...
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

    // robust 날짜 파싱
    val dateString = parseDateSmart(dateRaw)
    // robust 시간 파싱
    val (hour, minute) = parseTimeSmart(timeRaw)

    Log.d("parseLlama", "Final hour: $hour, minute: $minute")

    return Quintuple(summary, dateString, hour, minute, place)
}

// 날짜: 다양한 영어 표현 지원 ("2024-06-09", "Sunday", "tomorrow", "next Monday" 등)
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
    // 1. ISO 날짜
    try {
        if (Regex("""\d{4}-\d{2}-\d{2}""").matches(lower)) {
            LocalDate.parse(lower, DateTimeFormatter.ISO_LOCAL_DATE).let {
                return it.toString()
            }
        }
    } catch (_: Exception) {}
    // 2. "today", "tomorrow", "the day after tomorrow"
    when (lower) {
        "today" -> return today.toString()
        "tomorrow" -> return today.plusDays(1).toString()
        "the day after tomorrow", "day after tomorrow" -> return today.plusDays(2).toString()
    }
    // 3. 요일명 (Sunday ~ Saturday)
    dowMap[lower]?.let { targetDOW ->
        var daysToAdd = (targetDOW.value - today.dayOfWeek.value + 7) % 7
        if (daysToAdd == 0) daysToAdd = 7 // 항상 다음 주로
        return today.plusDays(daysToAdd.toLong()).toString()
    }
    // 4. "next Monday" 등
    val regexNextDay = Regex("""next\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday)""")
    regexNextDay.find(lower)?.let {
        val targetDOW = dowMap[it.groupValues[1]]!!
        var daysToAdd = (targetDOW.value - today.dayOfWeek.value + 7) % 7
        if (daysToAdd == 0) daysToAdd = 7
        return today.plusDays(daysToAdd.toLong()).toString()
    }
    // 5. 월/일 (ex: "June 10", "Jun 10", "10 June")
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
    // 6. 못찾으면 원문 반환
    return dateRaw
}

// robust 시간 파싱: "1230", "12:30", "twelve thirty", "12 pm", "noon" 등 커버
fun parseTimeSmart(timeRaw: String): Pair<String, String> {
    val cleaned = timeRaw.trim().lowercase(Locale.US)
    // 1. "1230"
    if (cleaned.length == 4 && cleaned.all { it.isDigit() }) {
        return cleaned.substring(0, 2) to cleaned.substring(2, 4)
    }
    // 2. "12:30" or "12-30"
    Regex("""(\d{1,2})[:\-](\d{2})""").find(cleaned)?.let { match ->
        return match.groupValues[1].padStart(2, '0') to match.groupValues[2].padStart(2, '0')
    }
    // 3. "12 pm", "12 am", "2:30 pm"
    Regex("""(\d{1,2})[:]?(\d{2})?\s*(am|pm)""").find(cleaned)?.let { match ->
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifBlank { "00" }
        val ampm = match.groupValues[3]
        if (ampm == "pm" && hour != 12) hour += 12
        if (ampm == "am" && hour == 12) hour = 0
        return hour.toString().padStart(2, '0') to minute.padStart(2, '0')
    }
    // 4. 영어 단어 ("twelve thirty", "nine", "noon", "midnight")
    val engNumMap = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12
    )
    if (cleaned.contains("noon")) return "12" to "00"
    if (cleaned.contains("midnight")) return "00" to "00"
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
            return hour to min
        }
    Regex("""(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)(?:\s*(am|pm))?""")
        .find(cleaned)?.let { match ->
            var hour = engNumMap[match.groupValues[1]] ?: 0
            val ampm = match.groupValues[2]
            if (ampm == "pm" && hour != 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return hour.toString().padStart(2, '0') to "00"
        }
    // 못 찾으면 빈 값
    return "" to ""
}

// 요일 → LocalDate로 변환 (UI 드롭다운 동기화용)
fun parseDateToLocalDate(dateString: String): LocalDate {
    return try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        val dayOfWeekMap = mapOf(
            "sunday" to DayOfWeek.SUNDAY,
            "monday" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
        )
        val now = LocalDate.now()
        val targetDayOfWeek = dayOfWeekMap[dateString.trim().lowercase()] ?: return now
        var daysToAdd = (targetDayOfWeek.value - now.dayOfWeek.value + 7) % 7
        if (daysToAdd == 0) daysToAdd = 7
        now.plusDays(daysToAdd.toLong())
    }
}

@Composable
fun AudioPlayer(
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

    // 재생 위치 실시간 업데이트
    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            if (!isSeeking) {
                position = mediaPlayer?.currentPosition ?: 0
                sliderPosition = position.toFloat()
            }
            delay(200)
        }
    }

    // SeekBar에서 사용자가 직접 이동할 때
    fun onSliderValueChange(value: Float) {
        isSeeking = true
        sliderPosition = value
    }

    fun onSliderValueChangeFinished() {
        isSeeking = false
        mediaPlayer?.seekTo(sliderPosition.toInt())
        position = sliderPosition.toInt()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFfef7e7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (mediaPlayer != null) {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                        } else {
                            mediaPlayer?.start()
                        }
                        isPlaying = !isPlaying
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        text = "${position/1000}초 / ${duration/1000}초",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ★ 재생바 (Slider)
            if (duration > 0) {
                Slider(
                    value = sliderPosition.coerceIn(0f, duration.toFloat()),
                    onValueChange = { onSliderValueChange(it) },
                    onValueChangeFinished = { onSliderValueChangeFinished() },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}