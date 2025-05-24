package com.example.domentiacare.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.domentiacare.R
import com.example.domentiacare.service.whisper.WhisperWrapper
import com.example.domentiacare.MyApplication
import com.example.domentiacare.ui.screen.call.parseLlamaScheduleResponseFull
// 필요시 추가
import android.media.MediaPlayer
import android.os.Environment
import java.io.File
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.domentiacare.MainActivity
import com.example.domentiacare.data.util.convertM4aToWavForWhisper
import kotlin.random.Random

class CallRecordAnalyzeService : Service() {

    private var fileObserver: FileObserver? = null
    private val recordDir = "/sdcard/Recordings/Call/"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("CallRecordAnalyzeService", "서비스 onCreate - 통화 녹음 감시 서비스 시작")
        startForegroundWithNotification("통화 녹음 감시중", "새 통화 녹음을 자동 분석합니다.")

        // 🔧 CLOSE_WRITE 이벤트로 변경 - 파일 쓰기 완료 시에만 처리
        fileObserver = object : FileObserver(recordDir, CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (event == CLOSE_WRITE && path != null) {
                    val newFilePath = "$recordDir/$path"
                    Log.d("CallRecordAnalyzeService", "파일 쓰기 완료 감지: $newFilePath")

                    // 🆕 추가 검증 후 처리
                    Thread {
                        if (waitForFileCompletion(newFilePath)) {
                            handleNewRecordFile(newFilePath)
                        } else {
                            Log.e("CallRecordAnalyzeService", "파일 완성 대기 실패: $newFilePath")
                        }
                    }.start()
                }
            }
        }
        fileObserver?.startWatching()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CallRecordAnalyzeService", "서비스 onDestroy - 통화 녹음 감시 서비스 종료")
        fileObserver?.stopWatching()
    }

    private fun startForegroundWithNotification(title: String, content: String) {
        val channelId = "call_record_analysis"
        val channelName = "통화 녹음 자동분석"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }
        val intent = Intent(this, MainActivity::class.java) // 알림 클릭시 이동
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 알맞은 아이콘 지정
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        startForeground(2, notification)
    }

    /**
     * 🆕 파일이 완전히 쓰여질 때까지 대기
     */
    private fun waitForFileCompletion(filePath: String): Boolean {
        val file = File(filePath)
        var lastSize = 0L
        var stableCount = 0
        val maxWaitTime = 30 // 최대 30초 대기

        repeat(maxWaitTime) {
            if (!file.exists()) {
                Log.d("CallRecordAnalyzeService", "파일이 존재하지 않음: $filePath")
                Thread.sleep(1000)
                return@repeat
            }

            val currentSize = file.length()
            Log.d("CallRecordAnalyzeService", "파일 크기 확인: $currentSize bytes")

            if (currentSize == lastSize && currentSize > 0) {
                stableCount++
                if (stableCount >= 3) { // 3초간 크기가 안정적이면 완성된 것으로 판단
                    Log.d("CallRecordAnalyzeService", "파일 완성 확인: $filePath (${currentSize} bytes)")
                    return true
                }
            } else {
                stableCount = 0
            }

            lastSize = currentSize
            Thread.sleep(1000)
        }

        Log.e("CallRecordAnalyzeService", "파일 완성 대기 타임아웃: $filePath")
        return false
    }

    private fun handleNewRecordFile(filePath: String) {
        // **여기서 Whisper → Llama → 일정 등록 파이프라인 자동 실행**
        Thread {
            try {
                val file = File(filePath)

                // 🆕 최종 파일 검증
                if (!file.exists() || file.length() == 0L) {
                    Log.e("CallRecordAnalyzeService", "유효하지 않은 파일: $filePath")
                    showResultNotification("일정 등록 실패", "", "", "", "오디오 파일 오류")
                    return@Thread
                }

                Log.d("CallRecordAnalyzeService", "파일 처리 시작: $filePath (${file.length()} bytes)")

                // 0. m4a -> wav파일 변환
                var audioPath = filePath
                if (audioPath.endsWith(".m4a", ignoreCase = true)) {
                    val m4aFile = File(audioPath)
                    val outputWavFile = File(applicationContext.cacheDir, m4aFile.nameWithoutExtension + ".wav")

                    Log.d("CallRecordAnalyzeService", "M4A → WAV 변환 시작")
                    convertM4aToWavForWhisper(m4aFile, outputWavFile)

                    if (!outputWavFile.exists() || outputWavFile.length() == 0L) {
                        Log.e("CallRecordAnalyzeService", "WAV 변환 실패")
                        showResultNotification("일정 등록 실패", "", "", "", "오디오 변환 실패")
                        return@Thread
                    }

                    audioPath = outputWavFile.absolutePath
                    Log.d("CallRecordAnalyzeService", "WAV 변환 완료: $audioPath (${outputWavFile.length()} bytes)")
                }

                // 1. Whisper 변환
                Log.d("CallRecordAnalyzeService", "Whisper 변환 시작")
                val context = applicationContext
                val whisper = WhisperWrapper(context)
                whisper.copyModelFiles()
                whisper.initModel()

                val transcript = whisper.transcribeBlocking(audioPath)
                Log.d("CallRecordAnalyzeService", "Whisper 변환 완료: $transcript")

                if (transcript.isBlank()) {
                    Log.e("CallRecordAnalyzeService", "Whisper 변환 결과가 비어있음")
                    showResultNotification("일정 등록 실패", "", "", "", "음성 인식 실패")
                    return@Thread
                }

                // 2. Llama 분석
                Log.d("CallRecordAnalyzeService", "Llama 분석 시작")
                val llamaManager = MyApplication.llamaServiceManager
                val prompt = """
                    Please analyze the following phone conversation and extract schedule information.
                    Output only two sections in the following format. **Do NOT use Markdown or any formatting.**
                    Summary: [A representative title for the schedule, extracted from the conversation.]
                    Schedule: {"date": "...", "time": "...", "place": "..."}
                    
                    Phone conversation:
                    "$transcript"
                """.trimIndent()

                val result = llamaManager.sendQueryBlocking(prompt)
                Log.d("CallRecordAnalyzeService", "Llama 분석 완료: $result")

                // 3. 파싱 및 일정 등록
                if (isValidLlamaResponse(result)) {
                    val (summary, date, hour, min, place) = parseLlamaScheduleResponseFull(result)
                    showResultNotification(summary, date, hour, min, place)
                } else {
                    Log.d("CallRecordAnalyzeService", "Llama 응답이 완전하지 않음: $result")
                    showResultNotification("일정 등록 실패", "", "", "", "LLaMA 응답 불완전")
                }

            } catch (e: Exception) {
                Log.e("CallRecordAnalyzeService", "처리 중 오류 발생", e)
                showResultNotification("일정 등록 실패", "", "", "", e.message ?: "알 수 없는 오류")
            }
        }.start()
    }

    /**
     * 🆕 LLaMA 응답이 유효한지 검증
     */
    private fun isValidLlamaResponse(response: String): Boolean {
        try {
            // 1. 기본 형식 검증
            if (!response.contains("Summary:") || !response.contains("Schedule:")) {
                Log.d("CallRecordAnalyzeService", "기본 형식 검증 실패")
                return false
            }

            // 2. JSON 부분 추출
            val scheduleIndex = response.indexOf("Schedule:")
            if (scheduleIndex == -1) {
                Log.d("CallRecordAnalyzeService", "Schedule 섹션 없음")
                return false
            }

            val jsonPart = response.substring(scheduleIndex + "Schedule:".length).trim()
            if (!jsonPart.startsWith("{") || !jsonPart.endsWith("}")) {
                Log.d("CallRecordAnalyzeService", "JSON 형식 불완전: $jsonPart")
                return false
            }

            // 3. JSON 파싱 테스트
            val jsonObject = org.json.JSONObject(jsonPart)

            // 4. 필수 필드 존재 확인
            val hasRequired = jsonObject.has("date") &&
                    jsonObject.has("time") &&
                    jsonObject.has("place")

            if (!hasRequired) {
                Log.d("CallRecordAnalyzeService", "필수 필드 누락")
                return false
            }

            Log.d("CallRecordAnalyzeService", "유효한 응답 확인: $jsonPart")
            return true

        } catch (e: Exception) {
            Log.d("CallRecordAnalyzeService", "JSON 검증 실패: ${e.message}")
            return false
        }
    }

    private fun showResultNotification(summary: String, date: String, hour: String, min: String, place: String) {
        val channelId = "call_record_analysis"
        val manager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("일정 자동 등록 결과")
            .setContentText("$summary ($date $hour:$min @ $place)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        manager.notify(Random.nextInt(), notification)
    }
}