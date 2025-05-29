package com.example.domentiacare.ui.screen.call

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.domentiacare.ui.screen.call.business.handleSaveSchedule
import com.example.domentiacare.ui.screen.call.components.*
import com.example.domentiacare.ui.screen.call.theme.OrangeLight
import com.example.domentiacare.ui.screen.call.theme.OrangePrimary
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
                    onTranscribe = { /* STT 변환 로직 */ }
                )

                ScheduleAnalysisSection(
                    memo = memo,
                    onMemoChange = { memo = it },
                    isAnalyzing = isAnalyzing,
                    onAnalyze = { /* 일정 분석 로직 */ }
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
                    days = days,
                    hours = hours,
                    minutes = minutes
                )

                SaveButton(
                    isSaving = isSaving,
                    onSave = {
                        handleSaveSchedule(
                            extractedTitle = extractedTitle,
                            currentUserId = currentUserId,
                            selectedYear = selectedYear,
                            selectedMonth = selectedMonth,
                            selectedDay = selectedDay,
                            selectedHour = selectedHour,
                            selectedMinute = selectedMinute,
                            selectedPlace = selectedPlace,
                            context = context,
                            coroutineScope = coroutineScope,
                            onSavingChange = { isSaving = it },
                            onMessageChange = { saveMessage = it },
                            onSuccess = {
                                coroutineScope.launch {
                                    delay(1500)
                                    navController.popBackStack()
                                }
                            }
                        )
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