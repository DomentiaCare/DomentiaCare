package com.example.domentiacare

import com.example.domentiacare.service.CallRecordAnalyzeService
import android.Manifest
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
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var finePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var backgroundPermissionLauncher: ActivityResultLauncher<String>

    // 🆕 알림 데이터를 담을 MutableState
    private lateinit var notificationDataState: MutableState<NotificationData?>

    // 🔹 POST_NOTIFICATIONS 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permission", "✅ 알림 권한 허용됨")
        } else {
            Toast.makeText(this, "❌ 알림 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 통화 기록 권한
    private val requestReadCallLog = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadCallLogs(this)
        } else {
            Toast.makeText(this, "통화 기록 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

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

        // ✅ Android 13 이상일 경우 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotificationPermission()
        }

        // ✅ 오디오 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestAudioPermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            requestAudioPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // 위치 권한 관련 초기화
        initializeLocationPermissions()

        enableEdgeToEdge()
        setContent {
            // 🆕 알림 데이터 상태 초기화
            notificationDataState = remember { mutableStateOf(extractNotificationData(intent)) }

            DomentiaCareTheme {
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

    // 위치 권한 초기화 메서드 분리
    private fun initializeLocationPermissions() {
        finePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("Permission", "정밀 위치 권한 허용됨")
                requestBackgroundLocationPermission()
            } else {
                Toast.makeText(this, "정확한 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

        backgroundPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("Permission", "백그라운드 위치 권한 허용됨")
                startLocationService()
            } else {
                Toast.makeText(this, "백그라운드 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

        if (hasFineLocationPermission()) {
            if (hasBackgroundLocationPermission()) {
                startLocationService()
            } else {
                requestBackgroundLocationPermission()
            }
        } else {
            requestFineLocationPermission()
        }
    }

    // 기존 메서드들 유지...
    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("Permission", "🎙️ 오디오 파일 접근 권한 허용됨")
        } else {
            Toast.makeText(this, "녹음 파일 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

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

    private fun requestFineLocationPermission() {
        finePermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun askNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Permission", "📢 알림 권한 이미 허용됨")
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            AlertDialog.Builder(this)
                .setTitle("알림 권한 필요")
                .setMessage("위치 이탈 알림을 수신하려면 알림 권한이 필요합니다.")
                .setPositiveButton("허용") { _, _ ->
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton("거부", null)
                .show()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}