package com.example.domentiacare

import com.example.domentiacare.service.CallRecordAnalyzeService
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    private val viewModel: CallLogViewModel by viewModels()
    private val IS_DEV_MODE = true

    // 🆕 알림 데이터를 담을 MutableState
    private lateinit var notificationDataState: MutableState<NotificationData?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        // TTS 서비스 테스트
//        TTSServiceManager.init(this){
//            // 2. 두 번째 목소리로 고정해서 이후 speak할 때 사용
//            TTSServiceManager.speak("이 목소리는 두 번째 한국어 voice입니다.")
//
//            // 필요시 tts shutdown
//            // TTSServiceManager.shutdown()
//        }

        val serviceIntent = Intent(this, CallRecordAnalyzeService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        enableEdgeToEdge()
        setContent {
            // 🆕 알림 데이터 상태 초기화
            notificationDataState = remember { mutableStateOf(extractNotificationData(intent)) }

            DomentiaCareTheme {
                // 🆕 순차적 권한 요청 컴포저블 호출
                SequentialPermissionRequester()

                if (IS_DEV_MODE) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 기존 앱 네비게이션에 알림 데이터 전달
                        Box(modifier = Modifier.weight(1f)) {
                            AppNavHost(notificationData = notificationDataState.value)
                        }
                    }
                } else {
                    // 정식 릴리즈에서는 기존 UI만 표시
                    AppNavHost(notificationData = notificationDataState.value)
                }
            }
        }
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
}