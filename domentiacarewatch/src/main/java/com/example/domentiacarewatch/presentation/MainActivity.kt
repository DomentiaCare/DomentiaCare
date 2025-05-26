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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener, TextToSpeech.OnInitListener {

    private val latestMessageState = mutableStateOf<String?>(null)
    private val testMessageState = mutableStateOf<String>("워치 앱 정상 작동중")

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
                WearApp("Android", latestMessageState, testMessageState)
            }

            // 5초 후 간단한 진동 테스트
            scheduleSimpleTest()

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

            testMessageState.value = "Always-On 모드 활성화"

        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ Always-On 설정 오류: ${e.message}", e)
            testMessageState.value = "Always-On 설정 실패"
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

                // WAKE_LOCK 권한은 manifest에서만 선언하면 됨 (dangerous permission 아님)
                Log.d("WatchMainActivity", "✅ WAKE_LOCK 권한은 매니페스트에서 처리")

                if (permissions.isNotEmpty()) {
                    Log.d("WatchMainActivity", "🔓 권한 요청: $permissions")
                    ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
                } else {
                    testMessageState.value = "모든 권한 확인됨"
                }
            } else {
                testMessageState.value = "Android 6.0 미만 - 권한 확인 생략"
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 권한 확인 오류: ${e.message}", e)
            testMessageState.value = "권한 확인 실패"
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
                    testMessageState.value = "권한 승인됨"
                } else {
                    Log.d("WatchMainActivity", "❌ 권한 거부됨")
                    testMessageState.value = "권한 거부됨"
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

            testMessageState.value = "앱 활성화됨"
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

    private fun scheduleSimpleTest() {
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                testMessageState.value = "5초 후 진동 테스트 시작..."
                performSimpleVibrationTest()
            }, 5000)
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 테스트 스케줄링 오류: ${e.message}", e)
        }
    }

    private fun performSimpleVibrationTest() {
        try {
            Log.d("WatchMainActivity", "🧪 간단한 진동 테스트 시작")
            val hasVibratePermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.VIBRATE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            Log.d("WatchMainActivity", "📋 VIBRATE 권한: $hasVibratePermission")
            if (!hasVibratePermission) {
                testMessageState.value = "VIBRATE 권한 없음"
                return
            }
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator == null) {
                Log.e("WatchMainActivity", "❌ Vibrator 서비스를 가져올 수 없음")
                testMessageState.value = "진동 서비스 없음"
                return
            }
            val hasVibrator = vibrator.hasVibrator()
            Log.d("WatchMainActivity", "📋 hasVibrator(): $hasVibrator")
            if (!hasVibrator) {
                Log.e("WatchMainActivity", "❌ 이 기기는 진동을 지원하지 않음")
                testMessageState.value = "진동 미지원 기기"
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hasAmplitudeControl = vibrator.hasAmplitudeControl()
                Log.d("WatchMainActivity", "📋 hasAmplitudeControl(): $hasAmplitudeControl")
            }
            Log.d("WatchMainActivity", "✅ 모든 진동 조건 확인됨")
            testMessageState.value = "진동 준비 완료"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
                Log.d("WatchMainActivity", "✅ VibrationEffect.createOneShot 실행")
                testMessageState.value = "진동 실행됨 (API 26+)"
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(2000)
                Log.d("WatchMainActivity", "✅ vibrate(long) 실행")
                testMessageState.value = "진동 실행됨 (레거시)"
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                performSimpleSoundTest()
            }, 3000)
        } catch (e: SecurityException) {
            Log.e("WatchMainActivity", "❌ 진동 보안 오류: ${e.message}")
            testMessageState.value = "진동 보안 오류: ${e.message}"
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 진동 테스트 오류: ${e.message}", e)
            testMessageState.value = "진동 오류: ${e.message}"
        }
    }

    private fun performSimpleSoundTest() {
        try {
            Log.d("WatchMainActivity", "🧪 간단한 소리 테스트 시작")
            testMessageState.value = "소리 테스트 시작"
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
            Log.d("WatchMainActivity", "✅ 0.5초 비프음 실행")
            testMessageState.value = "소리 테스트 실행됨"
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGenerator.release()
                } catch (e: Exception) {
                    Log.e("WatchMainActivity", "ToneGenerator 해제 오류: ${e.message}")
                }
            }, 1000)
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 소리 테스트 오류: ${e.message}", e)
            testMessageState.value = "소리 테스트 실패"
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        try {
            Log.d("WatchMainActivity", "📨 메시지 수신됨! - path: ${event.path}")
            if (event.path == "/schedule_notify") {
                val message = String(event.data)
                Log.d("WatchMainActivity", "📅 일정 알림 메시지: $message")
                runOnUiThread {
                    try {
                        // 1. 화면에 메시지 표시
                        latestMessageState.value = message
                        testMessageState.value = "알림 수신됨!"

                        // 2. 간단한 알림
                        showSimpleNotification(message)

                        // 3. 진동
                        performMessageVibration()

                        // 4. 소리 (1초 후)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            performMessageSound()
                        }, 1000)

                        // 5. TTS (음성 안내)
                        speakTTS(message)

                        Log.d("WatchMainActivity", "✅ 알림 처리 완료")
                    } catch (e: Exception) {
                        Log.e("WatchMainActivity", "❌ 알림 처리 오류: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 메시지 수신 오류: ${e.message}", e)
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

    private fun showSimpleNotification(message: String) {
        try {
            val channelId = "simple_schedule_notify"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "일정 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("📅 새 일정")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d("WatchMainActivity", "🔔 간단한 알림 생성")
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "❌ 알림 생성 오류: ${e.message}", e)
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
    greetingName: String,
    latestMessageState: State<String?>,
    testMessageState: State<String>
) {
    DomentiaCareTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = stringResource(R.string.hello_world, greetingName)
            )
            Text(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.secondary,
                text = testMessageState.value
            )
            latestMessageState.value?.let { message ->
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    text = "📅 $message"
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        "Preview Android",
        remember { mutableStateOf("테스트 메시지") },
        remember { mutableStateOf("앱 상태 정상") }
    )
}