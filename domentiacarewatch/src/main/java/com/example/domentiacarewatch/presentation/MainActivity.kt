package com.example.domentiacarewatch.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.domentiacarewatch.R
import com.example.domentiacarewatch.presentation.theme.DomentiaCareTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import java.util.Locale

enum class MessageType {
    NONE,        // 기본 상태
    DANGER,      // 위험 알림 (빨간색)
    SCHEDULE     // 일정 알림 (초록색)
}

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener, TextToSpeech.OnInitListener {

    private val latestMessageState = mutableStateOf<String?>(null)
    private val messageTypeState = mutableStateOf<MessageType>(MessageType.NONE)

    // TTS 관련 변수
    private var tts: TextToSpeech? = null
    private var pendingSpeak: String? = null

    // WakeLock 관련 변수
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            installSplashScreen()
            super.onCreate(savedInstanceState)

            Log.d("WatchMainActivity", "🚀 MainActivity onCreate 시작")

            // 🔓 권한 확인 및 요청
            checkAndRequestPermissions()

            // 🌟 Always-On 설정
            setupAlwaysOnMode()

            setTheme(android.R.style.Theme_DeviceDefault)

            // TTS 초기화
            tts = TextToSpeech(this, this)

            setContent {
                WearApp(latestMessageState, messageTypeState)
            }

            Log.d("WatchMainActivity", "✅ MainActivity onCreate 완료")

        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ onCreate 오류: ${e.message}", e)
        }
    }

    // Always-On 모드 설정
    private fun setupAlwaysOnMode() {
        try {
            // 1. 화면 켜짐 유지 설정
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d("WatchMainActivity", "✅ FLAG_KEEP_SCREEN_ON 설정")

            // 2. WakeLock 획득 (화면이 꺼지지 않도록)
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "DomentiaCareWatch::AlwaysOnLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10분간 유지 (배터리 보호)
            Log.d("WatchMainActivity", "✅ WakeLock 획득")

            // 3. 화면 밝기 설정 (배터리 절약을 위해 낮게)
            val layoutParams = window.attributes
            layoutParams.screenBrightness = 0.3f // 30% 밝기
            window.attributes = layoutParams
            Log.d("WatchMainActivity", "✅ 화면 밝기 30%로 설정")

        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ Always-On 설정 오류: ${e.message}", e)
        }
    }

    private fun checkAndRequestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permissions = mutableListOf<String>()

                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.VIBRATE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(android.Manifest.permission.VIBRATE)
                    Log.d("WatchMainActivity", "⚠️ VIBRATE 권한 없음")
                } else {
                    Log.d("WatchMainActivity", "✅ VIBRATE 권한 있음")
                }

                if (permissions.isNotEmpty()) {
                    Log.d("WatchMainActivity", "🔓 권한 요청: $permissions")
                    ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
                }
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 권한 확인 오류: ${e.message}", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d("WatchMainActivity", "✅ 권한 승인됨")
                } else {
                    Log.d("WatchMainActivity", "❌ 권한 거부됨")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Log.d("WatchMainActivity", "🔄 onResume - 메시지 리스너 등록")
            Wearable.getMessageClient(this).addListener(this)

            // Always-On 모드 재설정
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            Log.d("WatchMainActivity", "✅ 메시지 리스너 등록 완료")
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ onResume 오류: ${e.message}", e)
        }
    }

    override fun onPause() {
        try {
            Log.d("WatchMainActivity", "⏸️ onPause")
            Wearable.getMessageClient(this).removeListener(this)
            super.onPause()
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ onPause 오류: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        try {
            // WakeLock 해제
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("WatchMainActivity", "✅ WakeLock 해제")
                }
            }

            tts?.shutdown()
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ onDestroy 오류: ${e.message}", e)
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        try {
            Log.d("WatchMainActivity", "📨 메시지 수신됨! - path: ${event.path}")

            val message = String(event.data)

            when (event.path) {
                "/danger_alert" -> {
                    // 🚨 위험 알림 (빨간색)
                    Log.d("WatchMainActivity", "🚨 위험 알림 메시지: $message")
                    runOnUiThread {
                        handleAlertMessage(message, MessageType.DANGER)
                    }
                }
                "/schedule_notify" -> {
                    // 📅 일정 알림 (초록색)
                    Log.d("WatchMainActivity", "📅 일정 알림 메시지: $message")
                    runOnUiThread {
                        handleAlertMessage(message, MessageType.SCHEDULE)
                    }
                }
                "/schedule_simple_notify" -> {
                    // ✅ 새로운 간단 일정 알림 (파란색)
                    Log.d("WatchMainActivity", "💙 간단 일정 알림 메시지: $message")
                    runOnUiThread {
                        handleSimpleScheduleMessage(message)
                    }
                }
                else -> {
                    Log.d("WatchMainActivity", "⚠️ 알 수 없는 path: ${event.path}")
                }
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 메시지 수신 오류: ${e.message}", e)
        }
    }

    /**
     * 🆕 알림 메시지 처리 (타입별 구분)
     */
    private fun handleAlertMessage(message: String, messageType: MessageType) {
        try {
            // 🔧 화면 표시용 메시지 (정리된 형태)
            val displayMessage = when (messageType) {
                MessageType.SCHEDULE -> formatScheduleForDisplay(message)
                MessageType.DANGER -> formatDangerAlertMessage(message)
                else -> message
            }

            // 🔧 수정: displayMessage를 화면에 표시
            latestMessageState.value = displayMessage
            messageTypeState.value = messageType

            // 워치 노티피케이션 (원본 메시지 사용)
            showSimpleNotification(message, messageType)

            // 진동
            performMessageVibration()

            // 소리 (1초 후)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                performMessageSound()
            }, 1000)

            // TTS (음성 안내) - 원본 메시지 사용
            speakTTS(message)

            // 🆕 알림 타입에 따라 다른 시간 후 복원
            val displayTime = when (messageType) {
                MessageType.DANGER -> 15000L    // 위험 알림: 15초
                MessageType.SCHEDULE -> 10000L  // 일정 알림: 10초
                MessageType.NONE -> 0L
                else -> 10000L // 기본값
            }

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                latestMessageState.value = null
                messageTypeState.value = MessageType.NONE
            }, displayTime)

            Log.d("WatchMainActivity", "✅ ${messageType.name} 알림 처리 완료")
            Log.d("WatchMainActivity", "📱 화면 표시: $displayMessage")
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 알림 처리 오류: ${e.message}", e)
        }
    }

    /**
     * 🆕 일정 메시지를 화면 표시용으로 포맷팅
     */
    private fun formatScheduleForDisplay(message: String): String {
        return try {
            Log.d("WatchMainActivity", "🔧 포맷팅 시작 - 원본 메시지: $message")

            val lines = message.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            Log.d("WatchMainActivity", "🔧 분할된 라인: $lines (개수: ${lines.size})")

            if (lines.size < 3) {
                Log.d("WatchMainActivity", "🔧 라인 수 부족, 원본 반환")
                return message // 파싱 실패 시 원본 반환
            }

            val title = lines[0]
            val dateTime = lines[1]
            val location = lines[2]

            Log.d("WatchMainActivity", "🔧 파싱: title=$title, dateTime=$dateTime, location=$location")

            // 날짜/시간 파싱
            val dateTimeParts = dateTime.split(" ")
            if (dateTimeParts.size != 2) {
                Log.d("WatchMainActivity", "🔧 날짜시간 형식 오류, 원본 반환")
                return message
            }

            val datePart = dateTimeParts[0] // "2025-06-01"
            val timePart = dateTimeParts[1] // "12:30"

            val dateParts = datePart.split("-")
            if (dateParts.size != 3) {
                Log.d("WatchMainActivity", "🔧 날짜 형식 오류, 원본 반환")
                return message
            }

            val year = dateParts[0]
            val month = dateParts[1]
            val day = dateParts[2]

            val timeParts = timePart.split(":")
            if (timeParts.size != 2) {
                Log.d("WatchMainActivity", "🔧 시간 형식 오류, 원본 반환")
                return message
            }

            val hour = timeParts[0]
            val minute = timeParts[1]

            // 화면 표시용 포맷
            val formattedMessage = "일정 제목 : $title\n일정 시간 : ${year}년 ${month}월 ${day}일 ${hour}시 ${minute}분\n약속 장소 : $location"

            Log.d("WatchMainActivity", "📱 포맷팅 완료: $formattedMessage")
            return formattedMessage

        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 화면 포맷팅 오류: ${e.message}", e)
            return message // 파싱 실패 시 원본 반환
        }
    }

    private fun handleSimpleScheduleMessage(message: String) {
        try {
            // 간단한 메시지는 그대로 표시 (별도 포맷팅 불필요)
            latestMessageState.value = message
            messageTypeState.value = MessageType.SCHEDULE // 기존 SCHEDULE 타입 사용 또는 새로운 타입 생성

            // 워치 노티피케이션
            showSimpleNotification(message, MessageType.SCHEDULE)

            // 진동
            performMessageVibration()

            // 소리 (1초 후)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                performMessageSound()
            }, 1000)

            // TTS (음성 안내) - 간단한 메시지 그대로 읽기
            speakSimpleTTS(message)

            // 8초 후 메시지 제거 (간단한 알림이므로 조금 더 짧게)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                latestMessageState.value = null
                messageTypeState.value = MessageType.NONE
            }, 8000L)

            Log.d("WatchMainActivity", "✅ 간단 일정 알림 처리 완료: $message")

        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 간단 일정 알림 처리 오류: ${e.message}", e)
        }
    }

    /**
     * ✅ 새로운 함수: 간단한 TTS (복잡한 파싱 없이 바로 읽기)
     */
    private fun speakSimpleTTS(message: String) {
        try {
            if (tts != null) {
                tts?.language = Locale.KOREAN
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "simpleScheduleId")
                Log.d("WatchMainActivity", "🔊 간단 TTS 실행: $message")
            } else {
                pendingSpeak = message
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 간단 TTS 오류: ${e.message}", e)
        }
    }

    private fun performMessageVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 300, 100, 300, 100, 300)
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 300, 100, 300, 100, 300), -1)
                }
                Log.d("WatchMainActivity", "📳 메시지 진동 실행")
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 메시지 진동 오류: ${e.message}")
        }
    }

    private fun formatDangerAlertMessage(message: String): String {
        return message
            .split("\n") // 줄바꿈으로 분리
            .map { it.trim() } // 앞뒤 공백 제거
            .filter { it.isNotEmpty() } // 빈 줄 제거
            .joinToString("\n") // 다시 줄바꿈으로 연결
    }


    private fun performMessageSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            repeat(3) { i ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                }, i * 300L)
            }
            Log.d("WatchMainActivity", "🔊 메시지 소리 실행")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGenerator.release()
                } catch (e: Exception) {
                    Log.e("WatchMainActivity", "ToneGenerator 해제 오류: ${e.message}")
                }
            }, 2000)
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 메시지 소리 오류: ${e.message}")
        }
    }

    private fun showSimpleNotification(message: String, messageType: MessageType) {
        try {
            val channelId = "dementia_care_notify"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "DementiaCare 알림",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val title = when (messageType) {
                MessageType.DANGER -> "🚨 위험 알림"
                MessageType.SCHEDULE -> "📅 일정 알림"
                MessageType.NONE -> "DementiaCare"
                else -> "DementiaCare"
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d("WatchMainActivity", "🔔 워치 노티피케이션 생성: $title")
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 노티피케이션 생성 오류: ${e.message}", e)
        }
    }

    private fun parseScheduleMessage(message: String): String {
        try {
            val lines = message.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

            if (lines.size < 3) {
                return message
            }

            val title = lines[0]
            val dateTime = lines[1]
            val location = lines[2]

            val dateTimeParts = dateTime.split(" ")
            if (dateTimeParts.size != 2) {
                return message
            }

            val datePart = dateTimeParts[0]
            val timePart = dateTimeParts[1]

            val dateParts = datePart.split("-")
            if (dateParts.size != 3) {
                return message
            }

            val year = dateParts[0]
            val month = dateParts[1].toIntOrNull()?.toString()
            val day = dateParts[2].toIntOrNull()?.toString()

            val timeParts = timePart.split(":")
            if (timeParts.size != 2) {
                return message
            }

            val hour = timeParts[0].toIntOrNull()?.toString()
            val minute = timeParts[1].toIntOrNull()?.toString()

            val koreanMessage = "${location}에서 ${year}년 ${month}월 ${day}일 ${hour}시 ${minute}분 ${title} 약속이 등록되었습니다."

            Log.d("WatchMainActivity", "📝 파싱 결과: $koreanMessage")
            return koreanMessage

        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 메시지 파싱 오류: ${e.message}", e)
            return message
        }
    }

    private fun speakTTS(message: String) {
        try {
            val parsedMessage = parseScheduleMessage(message)

            if (tts != null) {
                tts?.language = Locale.KOREAN
                tts?.speak(parsedMessage, TextToSpeech.QUEUE_FLUSH, null, "msgId")
            } else {
                pendingSpeak = parsedMessage
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ TTS 오류: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && pendingSpeak != null) {
            tts?.language = Locale.KOREAN
            tts?.speak(pendingSpeak, TextToSpeech.QUEUE_FLUSH, null, "msgId")
            pendingSpeak = null
        }
    }
}

@Composable
fun WearApp(
    latestMessageState: State<String?>,
    messageTypeState: State<MessageType>
) {
    DomentiaCareTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // 상단에 시계 표시
            TimeText()

            // 중앙 컨텐츠: 기본 상태 vs 알림 상태
            if (latestMessageState.value != null) {
                // 알림이 있을 때: 타입에 따라 다른 색상으로 표시
                val (textColor, fontSize) = when (messageTypeState.value) {
                    MessageType.DANGER -> Pair(Color.Red, 16.sp)       // 🚨 위험 알림: 빨간색 20sp
                    MessageType.SCHEDULE -> Pair(Color.Green, 16.sp)   // 📅 일정 알림: 초록색 20sp
                    MessageType.NONE -> Pair(Color.White, 16.sp)       // 기본값 (사용되지 않음)
                    else -> Pair(Color.White, 16.sp)                   // 기본값
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    text = latestMessageState.value ?: ""
                )
            } else {
                // 🧡 기본 상태: DementiaCare 로고
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFF7F00), // 주황색
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    text = "DementiaCare"
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        remember { mutableStateOf(null) },
        remember { mutableStateOf(MessageType.NONE) }
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DangerAlertPreview() {
    WearApp(
        remember { mutableStateOf("안전 범위 이탈 경고\n김OO님이 설정된 안전범위를 벗어났습니다.\n즉시 확인 필요") },
        remember { mutableStateOf(MessageType.DANGER) }
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun ScheduleAlertPreview() {
    WearApp(
        remember { mutableStateOf("일정 제목 : Movie outing on Sunday\n일정 시간 : 2025년 06월 01일 12시 30분\n약속 장소 : AMC theater on main street") },
        remember { mutableStateOf(MessageType.SCHEDULE) }
    )
}