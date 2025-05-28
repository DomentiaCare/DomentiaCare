package com.example.domentiacare

import com.example.domentiacare.service.CallRecordAnalyzeService
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.domentiacare.data.util.getCallRecordingFiles
import com.example.domentiacare.ui.screen.call.CallLogViewModel
import com.example.domentiacare.service.LocationForegroundService
import com.example.domentiacare.ui.AppNavHost
import com.example.domentiacare.ui.theme.DomentiaCareTheme
import com.example.domentiacare.ui.test.TestLlamaActivity
import dagger.hilt.android.AndroidEntryPoint

import com.example.domentiacare.service.androidtts.TTSServiceManager

// AI Assistant imports
import com.example.domentiacare.assistant.AIAssistant
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.shadow

// 알림 데이터를 담는 데이터 클래스
data class NotificationData(
    val fromNotification: Boolean = false,
    val targetScreen: String? = null,
    val scheduleData: ScheduleNotificationData? = null,
    val notificationId: Int = -1
)

data class ScheduleNotificationData(
    val summary: String? = null,
    val date: String? = null,
    val time: String? = null,
    val place: String? = null
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val assistantEnabledByUser = mutableStateOf(false)
    private val viewModel: CallLogViewModel by viewModels()
    private val IS_DEV_MODE = true

    // 🆕 알림 데이터를 담을 MutableState
    private lateinit var notificationDataState: MutableState<NotificationData?>

    // AI 어시스턴트 변수 추가
    private var aiAssistant: AIAssistant? = null
    private val assistantActiveState = mutableStateOf(false)
    private val assistantRecordingState = mutableStateOf(false)
    private val assistantAnalyzingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate 진입")

        // AI 어시스턴트 초기화
        initializeAIAssistant()
        Log.d("MainActivity", "initializeAIAssistant() 호출")

        val serviceIntent = Intent(this, CallRecordAnalyzeService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        enableEdgeToEdge()
        setContent {
            // 알림 데이터 상태 초기화
            notificationDataState = remember { mutableStateOf(extractNotificationData(intent)) }

            DomentiaCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    SequentialPermissionRequester()

                    //AI어시스턴트 버튼 출현 위치
                    val offsetX = remember { mutableStateOf(850f) }
                    val offsetY = remember { mutableStateOf(1700f) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            content = { _ ->
                                // 🆕 상태 카드 제거하고 바로 AppNavHost 표시
                                AppNavHost(
                                    notificationData = notificationDataState.value,
                                    getAssistantState = { assistantEnabledByUser.value },
                                    toggleAssistant = {
                                        // 🆕 설정에서 스위치를 끌 때 강제 중지
                                        if (assistantEnabledByUser.value) {
                                            // 현재 켜져있는데 끄려고 함 → 강제 중지
                                            Log.d("MainActivity", "🛑 설정에서 AI 어시스턴트 비활성화 - 강제 중지")
                                            aiAssistant?.forceStop(showMessage = true)
                                        }
                                        assistantEnabledByUser.value = !assistantEnabledByUser.value
                                    }
                                )
                            }
                        )

                        // 🆕 플로팅 상태 카드 - 상단에 위치
                        AIAssistantFloatingStatusCard()

                        // FAB는 Box의 상단에서 레이아웃 흐름과 무관하게 위치
                        if (assistantEnabledByUser.value) {
                            FloatingActionButton(
                                onClick = {
                                    toggleAIAssistant()
                                },
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            offsetX.value.toInt(),
                                            offsetY.value.toInt()
                                        )
                                    }
                                    .zIndex(1f)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            offsetX.value += dragAmount.x
                                            offsetY.value += dragAmount.y
                                        }
                                    },
                                containerColor = when {
                                    assistantRecordingState.value || assistantAnalyzingState.value -> MaterialTheme.colorScheme.error  // 녹음/분석 중: 빨간색
                                    assistantActiveState.value -> MaterialTheme.colorScheme.secondary // 대기 중: 보조색
                                    else -> MaterialTheme.colorScheme.primary                         // 비활성: 기본색
                                }
                            ) {
                                Icon(
                                    imageVector = when {
                                        assistantRecordingState.value || assistantAnalyzingState.value -> Icons.Default.Stop     // 녹음/분석 중: 정지 아이콘
                                        assistantActiveState.value -> Icons.Default.Mic        // 대기 중: 마이크 아이콘
                                        else -> Icons.Default.MicOff                           // 비활성: 마이크 꺼짐 아이콘
                                    },
                                    contentDescription = when {
                                        assistantRecordingState.value || assistantAnalyzingState.value -> "강제 중지"
                                        assistantActiveState.value -> "녹음 시작"
                                        else -> "AI 어시스턴트 활성화"
                                    },
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 🆕 플로팅 AI 어시스턴트 상태 카드 - 애니메이션과 함께
    @Composable
    private fun AIAssistantFloatingStatusCard() {
        // 🆕 상태에 따른 표시 여부 결정
        val shouldShowCard = assistantAnalyzingState.value ||
                assistantRecordingState.value ||
                assistantActiveState.value

        // 🆕 애니메이션으로 부드럽게 나타나기/사라지기
        AnimatedVisibility(
            visible = shouldShowCard,
            enter = slideInVertically(
                initialOffsetY = { -it }, // 위에서 아래로 슬라이드
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it }, // 아래에서 위로 슬라이드
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 70.dp) // 🆕 상단에서 60dp 아래로 이동
                .zIndex(2f) // FAB보다 위에 표시
        ) {
            // 🆕 상단에 고정된 플로팅 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        assistantAnalyzingState.value -> MaterialTheme.colorScheme.tertiaryContainer
                        assistantRecordingState.value -> MaterialTheme.colorScheme.errorContainer
                        assistantActiveState.value -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    when {
                        assistantAnalyzingState.value -> {
                            // 분석 중 상태
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "🧠 명령 분석 중... (버튼 클릭 시 중지)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        assistantRecordingState.value -> {
                            // 녹음 중 상태
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "녹음 중",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "🎤 음성 인식 중... (버튼 클릭 시 중지)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        assistantActiveState.value -> {
                            // 대기 중 상태
                            Icon(
                                Icons.Default.MicOff,
                                contentDescription = "대기 중",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "🎙️ 준비됨. 버튼을 눌러 말하세요",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // AI 어시스턴트 플로팅 버튼
    @Composable
    private fun AIAssistantFAB() {
        val buttonColor = when {
            assistantRecordingState.value || assistantAnalyzingState.value -> MaterialTheme.colorScheme.error  // 녹음/분석 중: 빨간색
            assistantActiveState.value -> MaterialTheme.colorScheme.secondary // 대기 중: 보조색
            else -> MaterialTheme.colorScheme.primary                         // 비활성: 기본색
        }

        val buttonIcon = when {
            assistantRecordingState.value || assistantAnalyzingState.value -> Icons.Default.Stop     // 녹음/분석 중: 정지 아이콘
            assistantActiveState.value -> Icons.Default.Mic        // 대기 중: 마이크 아이콘
            else -> Icons.Default.MicOff                           // 비활성: 마이크 꺼짐 아이콘
        }

        val contentDescription = when {
            assistantRecordingState.value || assistantAnalyzingState.value -> "강제 중지"
            assistantActiveState.value -> "녹음 시작"
            else -> "AI 어시스턴트 활성화"
        }

        FloatingActionButton(
            onClick = {
                Log.d("MainActivity", "FloatingActionButton 클릭됨")
                toggleAIAssistant()
            },
            containerColor = buttonColor
        ) {
            Icon(
                imageVector = buttonIcon,
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }

    fun getAssistantState(): Boolean {
        return aiAssistant?.isActive() == true
    }

    fun toggleAssistantFromSetting() {
        toggleAIAssistant() // 기존 FAB에서 쓰는 함수 재활용
    }

    // 🆕 수정된 AI 어시스턴트 토글 - 강제 중지 로직 추가
    private fun toggleAIAssistant() {
        Log.d("MainActivity", "toggleAIAssistant() 호출됨, aiAssistant: $aiAssistant")

        aiAssistant?.let { assistant ->
            val isRecording = assistant.isCurrentlyRecording()
            val isAnalyzing = assistant.isCurrentlyAnalyzing()
            val isWaiting = assistant.isWaiting()
            val isActive = assistant.isActive()

            Log.d("MainActivity", "🔍 현재 상태 - isRecording: $isRecording, isAnalyzing: $isAnalyzing, isWaiting: $isWaiting, isActive: $isActive")

            when {
                isRecording || isAnalyzing -> {
                    // 🆕 녹음 중이거나 분석 중 → 강제 중지
                    Log.d("MainActivity", "🛑 녹음/분석 중 - 강제 중지 실행")
                    assistant.forceStop(showMessage = true)
                    updateAssistantStates()
                }
                isWaiting -> {
                    // 대기 중 → 녹음 시작
                    Log.d("MainActivity", "🎤 대기 중 - 녹음 시작")
                    assistant.activateAssistant()
                    updateAssistantStates()
                }
                else -> {
                    // 비활성 → 활성화
                    Log.d("MainActivity", "🚀 비활성 상태 - 권한 확인 후 어시스턴트 시작")
                    checkPermissionsAndStartAssistant()
                }
            }
        } ?: run {
            Log.e("MainActivity", "❌ aiAssistant가 null입니다!")
        }
    }

    // 어시스턴트 상태 업데이트
    private fun updateAssistantStates() {
        aiAssistant?.let { assistant ->
            assistantActiveState.value = assistant.isActive()
            assistantRecordingState.value = assistant.isCurrentlyRecording()
            assistantAnalyzingState.value = assistant.isCurrentlyAnalyzing()

            // 상태 변화 로깅 - 분석 상태도 추가
            Log.d("MainActivity", "📊 상태 업데이트 - Active: ${assistantActiveState.value}, Recording: ${assistantRecordingState.value}, Analyzing: ${assistantAnalyzingState.value}")

            // 녹음이 중지되고 처리가 시작되면 5초 후 상태 다시 확인
            if (!assistant.isCurrentlyRecording() && assistant.isActive()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    assistantActiveState.value = assistant.isActive()
                    assistantRecordingState.value = assistant.isCurrentlyRecording()
                    assistantAnalyzingState.value = assistant.isCurrentlyAnalyzing()
                }, 5000)
            }
        }
    }

    // 권한 확인 후 어시스턴트 시작 - 디버깅 로그 추가
    private fun checkPermissionsAndStartAssistant() {
        Log.d("MainActivity", "🔐 checkPermissionsAndStartAssistant() 진입")

        val hasRecordPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        Log.d("MainActivity", "🎤 RECORD_AUDIO 권한 상태: $hasRecordPermission")

        if (!hasRecordPermission) {
            // 권한이 없으면 사용자에게 알림만 표시 (중복 권한 요청 방지)
            Log.d("MainActivity", "❌ 권한 없음 - 사용자에게 알림 표시")
            Toast.makeText(this, "음성 인식을 위해 마이크 권한이 필요합니다.\n설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show()

            // 또는 설정 화면으로 이동하는 옵션 제공
            showPermissionDialog()
        } else {
            // 권한 있음 → 어시스턴트 시작
            Log.d("MainActivity", "✅ 권한 있음 - 어시스턴트 시작")
            startAIAssistant()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("마이크 권한 필요")
            .setMessage("AI 어시스턴트 기능을 사용하려면 마이크 권한이 필요합니다.\n설정에서 권한을 허용하시겠습니까?")
            .setPositiveButton("설정으로 이동") { _, _ ->
                // 앱 설정 화면으로 이동
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // AI 어시스턴트 시작 - 디버깅 로그 추가
    private fun startAIAssistant() {
        Log.d("MainActivity", "🎤 startAIAssistant() 진입")

        aiAssistant?.let { assistant ->
            Log.d("MainActivity", "✅ aiAssistant 확인됨 - activateAssistant() 호출")
            assistant.activateAssistant()
            updateAssistantStates()
            Log.d("MainActivity", "🎤 AI 어시스턴트 활성화 완료")
        } ?: run {
            Log.e("MainActivity", "❌ aiAssistant가 null입니다!")
        }
    }

    // 일정 액션 처리
    private fun handleScheduleAction(action: String, details: String) {
        Log.d("MainActivity", "📅 스케줄 액션: $action - $details")

        when (action) {
            "check" -> {
                // 일정 조회 로직
                if (details == "today") {
                    // 오늘 일정 조회
                    Log.d("MainActivity", "오늘 일정 조회 실행")
                } else if (details == "tomorrow") {
                    // 내일 일정 조회
                    Log.d("MainActivity", "내일 일정 조회 실행")
                }
            }
            "find" -> {
                // 위치 찾기 로직
                if (details == "patient") {
                    Log.d("MainActivity", "환자 위치 찾기 실행")
                } else if (details == "caregiver") {
                    Log.d("MainActivity", "보호자 위치 찾기 실행")
                }
            }
        }
    }

    // AI 어시스턴트 초기화 함수 - 수정된 버전
    private fun initializeAIAssistant() {
        Log.d("MainActivity", "initializeAIAssistant() 진입")
        aiAssistant = AIAssistant(
            context = this,
            onScheduleAction = { action, details ->
                handleScheduleAction(action, details)
            },
            onStateChanged = {
                // 상태 변경 시 UI 업데이트 콜백
                Log.d("MainActivity", "🔄 AI Assistant 상태 변경됨 - UI 업데이트 실행")
                updateAssistantStates()
            }
        )
        Log.d("MainActivity", "✅ AI 어시스턴트 초기화 완료")
    }

    // 🆕 순차적 권한 요청 컴포저블
    @Composable
    fun SequentialPermissionRequester() {
        val context = LocalContext.current
        val activity = context as Activity

        // 요청할 권한 리스트 (Android 버전에 따라 동적으로 구성)
        val permissions = remember {
            mutableListOf<String>().apply {
                // 기본 권한들
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.READ_PHONE_STATE)
                add(Manifest.permission.RECORD_AUDIO) // AI 어시스턴트용 추가

                // Android 버전에 따른 오디오/저장소 권한
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_AUDIO)
                    add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                // 백그라운드 위치 권한 (Android 10 이상)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }

        // 현재 요청할 권한의 인덱스 상태
        var currentIndex by remember { mutableStateOf(0) }
        var allPermissionsRequested by remember { mutableStateOf(false) }

        // 런처: 하나의 권한을 요청함
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val currentPermission = permissions[currentIndex]

            if (isGranted) {
                Log.d("Permission", "✅ $currentPermission 허용됨")
                handlePermissionGranted(currentPermission)
            } else {
                Log.d("Permission", "❌ $currentPermission 거절됨")
                handlePermissionDenied(currentPermission)
            }

            // 다음 권한으로 이동
            currentIndex++
        }

        // 최초 실행 시 권한 요청 시작
        LaunchedEffect(currentIndex) {
            if (currentIndex < permissions.size && !allPermissionsRequested) {
                val permission = permissions[currentIndex]

                // 이미 허용된 권한인지 확인
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "📋 $permission 이미 허용됨")
                    handlePermissionGranted(permission)
                    currentIndex++
                } else {
                    // 백그라운드 위치 권한의 경우 특별 처리
                    if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                            // 정밀 위치 권한이 없으면 백그라운드 위치 권한 건너뛰기
                            Log.d("Permission", "⚠️ 정밀 위치 권한이 없어 백그라운드 위치 권한 건너뛰기")
                            currentIndex++
                            return@LaunchedEffect
                        }
                    }

                    permissionLauncher.launch(permission)
                }
            } else if (!allPermissionsRequested) {
                Log.d("Permission", "🎉 모든 권한 요청 완료")
                allPermissionsRequested = true
                onAllPermissionsProcessed()
            }
        }
    }

    // 권한이 허용되었을 때 처리
    private fun handlePermissionGranted(permission: String) {
        when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                Log.d("Permission", "정밀 위치 권한 허용됨")
            }
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                Log.d("Permission", "백그라운드 위치 권한 허용됨")
            }
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                Log.d("Permission", "🎙️ 오디오 파일 접근 권한 허용됨")
            }
            Manifest.permission.POST_NOTIFICATIONS -> {
                Log.d("Permission", "📢 알림 권한 허용됨")
            }
            Manifest.permission.READ_PHONE_STATE -> {
                Log.d("Permission", "📞 통화 기록 권한 허용됨")
                viewModel.loadCallLogs(this)
            }
            Manifest.permission.RECORD_AUDIO -> {
                Log.d("Permission", "🎤 음성 녹음 권한 허용됨")
                // 권한이 허용되면 AI 어시스턴트를 즉시 시작할 수 있도록 상태 업데이트
                Toast.makeText(this, "이제 AI 어시스턴트를 사용할 수 있습니다!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 권한이 거절되었을 때 처리
    private fun handlePermissionDenied(permission: String) {
        when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                Toast.makeText(this, "정확한 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                Toast.makeText(this, "백그라운드 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                Toast.makeText(this, "녹음 파일 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
            Manifest.permission.POST_NOTIFICATIONS -> {
                Toast.makeText(this, "❌ 알림 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
            }
            Manifest.permission.READ_PHONE_STATE -> {
                Toast.makeText(this, "통화 기록 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
            Manifest.permission.RECORD_AUDIO -> {
                Toast.makeText(this, "음성 인식 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 모든 권한 처리 완료 후 실행할 로직
    private fun onAllPermissionsProcessed() {
        // 위치 서비스 시작 (위치 권한이 있는 경우)
        if (hasFineLocationPermission()) {
            startLocationService()
        }
    }

    // 🆕 수정된 onNewIntent 메서드 - override 키워드와 올바른 시그니처 사용
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 🆕 새 인텐트가 들어올 때 알림 데이터 업데이트
        if (::notificationDataState.isInitialized) {
            notificationDataState.value = extractNotificationData(intent)
        }
    }

    // 🆕 인텐트에서 알림 데이터 추출
    private fun extractNotificationData(intent: Intent?): NotificationData? {
        return if (intent?.getBooleanExtra("from_notification", false) == true) {
            Log.d("MainActivity", "알림에서 앱 실행됨")
            Log.d("MainActivity", "Target Screen: ${intent.getStringExtra("target_screen")}")
            Log.d("MainActivity", "Schedule Summary: ${intent.getStringExtra("schedule_summary")}")

            NotificationData(
                fromNotification = true,
                targetScreen = intent.getStringExtra("target_screen"),
                scheduleData = ScheduleNotificationData(
                    summary = intent.getStringExtra("schedule_summary"),
                    date = intent.getStringExtra("schedule_date"),
                    time = intent.getStringExtra("schedule_time"),
                    place = intent.getStringExtra("schedule_place")
                ),
                notificationId = intent.getIntExtra("notification_id", -1)
            )
        } else {
            Log.d("MainActivity", "일반 앱 실행")
            null
        }
    }

    // 유틸리티 메서드들
    private fun hasFineLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    //AI 어시스턴트 버튼
    @Composable
    private fun DraggableAIAssistantFAB() {
        var offsetX by rememberSaveable { mutableStateOf(0f) }
        var offsetY by rememberSaveable { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                }
        ) {
            AIAssistantFAB()
        }
    }

    override fun onDestroy() {
        // 🆕 AI 어시스턴트 리소스 정리 - 강제 중지 포함
        Log.d("MainActivity", "onDestroy 호출됨 - AI 어시스턴트 정리")
        aiAssistant?.destroy()
        super.onDestroy()
    }

    companion object {
        private const val RECORD_AUDIO_REQUEST_CODE = 1001
    }
}