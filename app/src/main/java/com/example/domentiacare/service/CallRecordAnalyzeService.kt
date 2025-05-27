package com.example.domentiacare.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.domentiacare.R
import com.example.domentiacare.service.whisper.WhisperWrapper
import com.example.domentiacare.MyApplication
import com.example.domentiacare.data.model.*
import com.example.domentiacare.data.local.RecordStorage
import com.example.domentiacare.data.util.UserPreferences
// 필요시 추가
import java.io.File
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.domentiacare.MainActivity
import com.example.domentiacare.data.util.convertM4aToWavForWhisper
import com.example.domentiacare.network.RecordApiService
import kotlin.random.Random
import kotlinx.coroutines.*

//Watch 서비스에서 사용하는 import
import com.example.domentiacare.service.watch.WatchMessageHelper

class CallRecordAnalyzeService : Service() {

    private var fileObserver: FileObserver? = null
    private val recordDir = "/sdcard/Recordings/Call/"
    private lateinit var recordStorage: RecordStorage
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("CallRecordAnalyzeService", "=== 서비스 시작 ===")
        Log.d("CallRecordAnalyzeService", "Record 시스템과 통합된 통화 녹음 감시 서비스 시작")

        // Record 저장소 초기화
        recordStorage = RecordStorage(applicationContext)

        startForegroundWithNotification("통화 녹음 감시중", "새 통화 녹음을 자동 분석합니다.")

        // 🔧 CLOSE_WRITE 이벤트로 변경 - 파일 쓰기 완료 시에만 처리
        fileObserver = object : FileObserver(recordDir, CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (event == CLOSE_WRITE && path != null) {
                    val newFilePath = "$recordDir/$path"
                    Log.d("CallRecordAnalyzeService", "📁 파일 쓰기 완료 감지: $newFilePath")

                    // 🆕 추가 검증 후 처리
                    Thread {
                        if (waitForFileCompletion(newFilePath)) {
                            handleNewRecordFileWithRecordSystem(newFilePath)
                        } else {
                            Log.e("CallRecordAnalyzeService", "❌ 파일 완성 대기 실패: $newFilePath")
                        }
                    }.start()
                }
            }
        }
        fileObserver?.startWatching()
        Log.d("CallRecordAnalyzeService", "✅ 파일 감시 시작: $recordDir")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CallRecordAnalyzeService", "=== 서비스 종료 ===")
        fileObserver?.stopWatching()
        serviceScope.cancel()
    }

    private fun startForegroundWithNotification(title: String, content: String) {
        val channelId = "call_record_analysis"
        val channelName = "통화 녹음 자동분석"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
                Log.d("CallRecordAnalyzeService", "⏳ 파일 대기 중: $filePath")
                Thread.sleep(1000)
                return@repeat
            }

            val currentSize = file.length()
            Log.d("CallRecordAnalyzeService", "📏 파일 크기 확인: $currentSize bytes")

            if (currentSize == lastSize && currentSize > 0) {
                stableCount++
                if (stableCount >= 3) { // 3초간 크기가 안정적이면 완성된 것으로 판단
                    Log.d("CallRecordAnalyzeService", "✅ 파일 완성 확인: $filePath (${currentSize} bytes)")
                    return true
                }
            } else {
                stableCount = 0
            }

            lastSize = currentSize
            Thread.sleep(1000)
        }

        Log.e("CallRecordAnalyzeService", "❌ 파일 완성 대기 타임아웃: $filePath")
        return false
    }

    /**
     * 🆕 Record 시스템과 통합된 새 파일 처리
     */
    private fun handleNewRecordFileWithRecordSystem(filePath: String) {
        serviceScope.launch {
            Log.d("CallRecordAnalyzeService", "=== Record 시스템 통합 처리 시작 ===")
            Log.d("CallRecordAnalyzeService", "파일: $filePath")

            try {
                val file = File(filePath)

                // 🆕 최종 파일 검증
                if (!file.exists() || file.length() == 0L) {
                    Log.e("CallRecordAnalyzeService", "❌ 유효하지 않은 파일: $filePath")
                    showErrorNotification("파일 오류", "오디오 파일이 유효하지 않습니다.")
                    return@launch
                }

                // 1. RecordingFile 객체 생성
                val recordingFile = RecordingFile(
                    name = file.name,
                    path = file.absolutePath,
                    lastModified = file.lastModified(),
                    size = file.length()
                )
                Log.d("CallRecordAnalyzeService", "📋 RecordingFile 생성: ${recordingFile.name}")

                // 2. 사용자 ID 가져오기
                val userId = UserPreferences.getUserId(applicationContext).let {
                    if (it > 0) it else 6L
                }
                Log.d("CallRecordAnalyzeService", "👤 사용자 ID: $userId")

                // 3. Record 생성 및 저장
                val record = recordingFile.toRecord(userId)
                val saveResult = recordStorage.saveRecord(record)

                if (saveResult.isFailure) {
                    Log.e("CallRecordAnalyzeService", "❌ Record 저장 실패: ${saveResult.exceptionOrNull()?.message}")
                    showErrorNotification("저장 오류", "Record 저장에 실패했습니다.")
                    return@launch
                }

                Log.d("CallRecordAnalyzeService", "✅ Record 저장 성공: ${record.localId}")

                // 4. 전체 파이프라인 실행 (Whisper → Llama → 파싱)
                Log.d("CallRecordAnalyzeService", "🚀 전체 파이프라인 시작")
                val pipelineSuccess = executeFullPipeline(record.localId)

                if (pipelineSuccess) {
                    Log.d("CallRecordAnalyzeService", "🎉 전체 파이프라인 성공!")

                    // 5. 최종 Record 확인
                    val finalRecord = recordStorage.getRecordById(record.localId)
                    if (finalRecord?.extractedSchedules?.isNotEmpty() == true) {
                        Log.d("CallRecordAnalyzeService", "📅 추출된 일정: ${finalRecord.extractedSchedules!!.size}개")

                        // 6. Record만 저장 (SimpleSchedule 내보내기 제거)
                        saveRecord(finalRecord)

                        // 2. 서버 전송 (최종 finalRecord로)
                        //    최초 생성이 이미 서버에 되어 있으면 updateRecord,
                        //    그렇지 않으면 createRecord를 사용(최초 1회만 create, 이후엔 update)
                        try {
                            // createRecord는 서버에 없을 때(처음), updateRecord는 이미 서버에 레코드가 있을 때 사용
                            // create 시도가 실패하면 update로 fallback 가능 (상황에 따라)
                            val apiResult = RecordApiService.createRecord(finalRecord, applicationContext)
                            if (apiResult.isSuccess) {
                                Log.d("CallRecordAnalyzeService", "✅==================== 서버에 최종 Record 동기화 성공")
                            } else {
                                Log.e("CallRecordAnalyzeService", "❌==================== 서버에 최종 Record 동기화 실패: ${apiResult.exceptionOrNull()}")
                            }
                        } catch (e: Exception) {
                            Log.e("CallRecordAnalyzeService", "❌ 서버 동기화 예외", e)
                        }

                        // 7. 성공 알림 표시
                        showSuccessNotification(finalRecord)
                    } else {
                        Log.w("CallRecordAnalyzeService", "⚠️ 추출된 일정이 없습니다.")
                        showErrorNotification("일정 추출 실패", "통화에서 일정을 찾을 수 없습니다.")
                    }
                } else {
                    Log.e("CallRecordAnalyzeService", "❌ 파이프라인 실행 실패")
                    showErrorNotification("처리 실패", "통화 분석 중 오류가 발생했습니다.")
                }

            } catch (e: Exception) {
                Log.e("CallRecordAnalyzeService", "❌ 처리 중 예외 발생", e)
                showErrorNotification("시스템 오류", e.message ?: "알 수 없는 오류")
            }

            Log.d("CallRecordAnalyzeService", "=== Record 시스템 통합 처리 완료 ===")
        }
    }

    /**
     * 전체 파이프라인 실행 (Whisper → Llama → 파싱)
     */
    private suspend fun executeFullPipeline(recordId: String): Boolean {
        return try {
            Log.d("CallRecordAnalyzeService", "🎙️ 1단계: Whisper 처리 시작")
            recordStorage.updateProcessStatus(recordId, transcriptStatus = ProcessStatus.PROCESSING)
            val whisperSuccess = processWithWhisper(recordId)

            if (!whisperSuccess) {
                Log.e("CallRecordAnalyzeService", "❌ Whisper 처리 실패")
                return false
            }

            Log.d("CallRecordAnalyzeService", "🧠 2단계: Llama 분석 시작")
            recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.PROCESSING)
            val llamaSuccess = processWithLlama(recordId)

            if (!llamaSuccess) {
                Log.e("CallRecordAnalyzeService", "❌ Llama 분석 실패")
                return false
            }

            Log.d("CallRecordAnalyzeService", "📅 3단계: 일정 파싱 시작")
            recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.PROCESSING)
            val parseSuccess = parseScheduleFromResult(recordId)

            if (!parseSuccess) {
                Log.e("CallRecordAnalyzeService", "❌ 일정 파싱 실패")
                return false
            }

            Log.d("CallRecordAnalyzeService", "✅ 전체 파이프라인 성공")
            true

        } catch (e: Exception) {
            Log.e("CallRecordAnalyzeService", "❌ 파이프라인 실행 중 예외", e)
            recordStorage.updateProcessStatus(
                recordId,
                transcriptStatus = ProcessStatus.FAILED,
                analysisStatus = ProcessStatus.FAILED,
                parseStatus = ProcessStatus.FAILED
            )
            false
        }
    }

    /**
     * Whisper 처리
     */
    private suspend fun processWithWhisper(recordId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d("CallRecordAnalyzeService", "🎙️ Whisper 처리 시작: $recordId")

        val record = recordStorage.getRecordById(recordId) ?: run {
            Log.e("CallRecordAnalyzeService", "❌ Record를 찾을 수 없음: $recordId")
            return@withContext false
        }

        var outputWavFile: File? = null

        try {
            var audioPath = record.path

            // M4A → WAV 변환 (필요시)
            if (audioPath.endsWith(".m4a", ignoreCase = true)) {
                val m4aFile = File(audioPath)
                outputWavFile = File(applicationContext.cacheDir, m4aFile.nameWithoutExtension + ".wav")

                Log.d("CallRecordAnalyzeService", "🔄 M4A → WAV 변환 시작")
                convertM4aToWavForWhisper(m4aFile, outputWavFile)

                if (!outputWavFile.exists() || outputWavFile.length() == 0L) {
                    Log.e("CallRecordAnalyzeService", "❌ WAV 변환 실패")
                    return@withContext false
                }

                audioPath = outputWavFile.absolutePath
                Log.d("CallRecordAnalyzeService", "✅ WAV 변환 완료: ${outputWavFile.length()} bytes")
            }

            // Whisper 실행
            val whisper = WhisperWrapper(applicationContext)
            whisper.copyModelFiles()
            whisper.initModel()

            val transcript = whisper.transcribeBlocking(audioPath)
            Log.d("CallRecordAnalyzeService", "📝 Whisper 결과: ${transcript.take(100)}...")

            if (transcript.isBlank()) {
                Log.e("CallRecordAnalyzeService", "❌ Whisper 결과가 비어있음")
                recordStorage.updateProcessStatus(recordId, transcriptStatus = ProcessStatus.FAILED)
                return@withContext false
            }

            // 결과 저장
            recordStorage.updateTranscript(recordId, transcript, ProcessStatus.COMPLETED)
            Log.d("CallRecordAnalyzeService", "✅ Whisper 처리 완료 및 저장")

            true

        } catch (e: Exception) {
            Log.e("CallRecordAnalyzeService", "❌ Whisper 처리 중 예외", e)
            recordStorage.updateProcessStatus(recordId, transcriptStatus = ProcessStatus.FAILED)
            false
        } finally {
            // WAV 파일 정리
            outputWavFile?.let { deleteWavFile(it) }
        }
    }

    /**
     * Llama 분석
     */
    private suspend fun processWithLlama(recordId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d("CallRecordAnalyzeService", "🧠 Llama 분석 시작: $recordId")

        val record = recordStorage.getRecordById(recordId) ?: run {
            Log.e("CallRecordAnalyzeService", "❌ Record를 찾을 수 없음: $recordId")
            return@withContext false
        }

        if (record.transcript.isNullOrEmpty()) {
            Log.e("CallRecordAnalyzeService", "❌ Transcript가 비어있음")
            recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.FAILED)
            return@withContext false
        }

        try {
            val llamaManager = MyApplication.llamaServiceManager
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
                "${record.transcript}"
            """.trimIndent()

            val result = llamaManager.sendQueryBlocking(prompt)
            Log.d("CallRecordAnalyzeService", "🧠 Llama 결과: $result")

            if (!isValidLlamaResponse(result)) {
                Log.e("CallRecordAnalyzeService", "❌ Llama 응답이 유효하지 않음")
                recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.FAILED)
                return@withContext false
            }

            // 결과 저장
            recordStorage.updateAnalysisResult(recordId, result, ProcessStatus.COMPLETED)
            Log.d("CallRecordAnalyzeService", "✅ Llama 분석 완료 및 저장")

            true

        } catch (e: Exception) {
            Log.e("CallRecordAnalyzeService", "❌ Llama 분석 중 예외", e)
            recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.FAILED)
            false
        }
    }

    /**
     * 일정 파싱
     */
    private suspend fun parseScheduleFromResult(recordId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d("CallRecordAnalyzeService", "📅 일정 파싱 시작: $recordId")

        val record = recordStorage.getRecordById(recordId) ?: run {
            Log.e("CallRecordAnalyzeService", "❌ Record를 찾을 수 없음: $recordId")
            return@withContext false
        }

        if (record.result.isNullOrEmpty()) {
            Log.e("CallRecordAnalyzeService", "❌ Llama 결과가 비어있음")
            recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.FAILED)
            return@withContext false
        }

        try {
            val extractedSchedules = parseLlamaScheduleResponseFull(
                record.result,
                recordId,
                record.userId
            )

            Log.d("CallRecordAnalyzeService", "📅 파싱 결과: ${extractedSchedules.size}개 일정")
            extractedSchedules.forEachIndexed { index, schedule ->
                Log.d("CallRecordAnalyzeService", "  ${index + 1}. ${schedule.title} (${schedule.startDate})")
            }

            if (extractedSchedules.isEmpty()) {
                Log.w("CallRecordAnalyzeService", "⚠️ 파싱된 일정이 없음")
                recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.FAILED)
                return@withContext false
            }

            // 결과 저장
            recordStorage.updateExtractedSchedules(recordId, extractedSchedules, ProcessStatus.COMPLETED)
            Log.d("CallRecordAnalyzeService", "✅ 일정 파싱 완료 및 저장")

            true

        } catch (e: Exception) {
            Log.e("CallRecordAnalyzeService", "❌ 일정 파싱 중 예외", e)
            recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.FAILED)
            false
        }
    }

    /**
     * Record만 저장
     */
    // local에 room으로 저장 - 박진호 할 일
    private suspend fun saveRecord(record: Record) = withContext(Dispatchers.IO) {
        Log.d("CallRecordAnalyzeService", "💾 Record 최종 저장")
        Log.d("CallRecordAnalyzeService", "  - Record ID: ${record.localId}")
        Log.d("CallRecordAnalyzeService", "  - 파일명: ${record.name}")
        Log.d("CallRecordAnalyzeService", "  - 추출된 일정 수: ${record.extractedSchedules?.size ?: 0}")

        record.extractedSchedules?.forEachIndexed { index, schedule ->
            Log.d("CallRecordAnalyzeService", "    📅 일정 ${index + 1}: ${schedule.title}")
            Log.d("CallRecordAnalyzeService", "      - 시작: ${schedule.startDate}")
            Log.d("CallRecordAnalyzeService", "      - 설명: ${schedule.description}")
        }

        // Record는 이미 각 단계에서 저장되었으므로 별도 저장 불필요
        Log.d("CallRecordAnalyzeService", "✅ Record 저장 완료 - 모든 데이터가 Record 시스템에 저장됨")
    }

    /**
     * 🆕 WAV 파일 삭제 함수
     */
    private fun deleteWavFile(wavFile: File) {
        try {
            if (wavFile.exists()) {
                val deleted = wavFile.delete()
                if (deleted) {
                    Log.d("CallRecordAnalyzeService", "✅ WAV 파일 삭제 성공: ${wavFile.absolutePath}")
                } else {
                    Log.w("CallRecordAnalyzeService", "⚠️ WAV 파일 삭제 실패: ${wavFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e("CallRecordAnalyzeService", "❌ WAV 파일 삭제 중 오류: ${e.message}", e)
        }
    }

    /**
     * 🆕 LLaMA 응답이 유효한지 검증
     */
    private fun isValidLlamaResponse(response: String): Boolean {
        try {
            // 1. 기본 형식 검증
            if (!response.contains("Summary:") || !response.contains("Schedule:")) {
                Log.d("CallRecordAnalyzeService", "❌ 기본 형식 검증 실패")
                return false
            }

            // 2. JSON 부분 추출
            val scheduleIndex = response.indexOf("Schedule:")
            if (scheduleIndex == -1) {
                Log.d("CallRecordAnalyzeService", "❌ Schedule 섹션 없음")
                return false
            }

            val jsonPart = response.substring(scheduleIndex + "Schedule:".length).trim()
            if (!jsonPart.startsWith("{") || !jsonPart.endsWith("}")) {
                Log.d("CallRecordAnalyzeService", "❌ JSON 형식 불완전: $jsonPart")
                return false
            }

            // 3. JSON 파싱 테스트
            val jsonObject = org.json.JSONObject(jsonPart)

            // 4. 필수 필드 존재 확인
            val hasRequired = jsonObject.has("date") &&
                    jsonObject.has("time") &&
                    jsonObject.has("place")

            if (!hasRequired) {
                Log.d("CallRecordAnalyzeService", "❌ 필수 필드 누락")
                return false
            }

            Log.d("CallRecordAnalyzeService", "✅ 유효한 응답 확인")
            return true

        } catch (e: Exception) {
            Log.d("CallRecordAnalyzeService", "❌ JSON 검증 실패: ${e.message}")
            return false
        }
    }

    /**
     * 성공 알림 표시 (Record 기반)
     */
    private fun showSuccessNotification(record: Record) {
        val channelId = "call_record_analysis"

        // 첫 번째 일정 정보 가져오기
        val firstSchedule = record.extractedSchedules?.firstOrNull()
        val scheduleCount = record.extractedSchedules?.size ?: 0

        // ===== 워치에도 메시지 전송 =====
        val watchMessage = """
        $summary
        $date $hour:$min
        $place
    """.trimIndent()

        Log.d("CallRecordAnalyzeService", "워치 메세지 전송: $watchMessage")
        WatchMessageHelper.sendMessageToWatch(
            context = this,
            path = "/schedule_notify",
            message = watchMessage
        )
        // ===========================

        // MainActivity로 이동하는 인텐트 생성
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra("target_screen", "record_detail") // Record 상세 화면으로 변경
            putExtra("record_id", record.localId)
            putExtra("notification_id", Random.nextInt())
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (firstSchedule != null) {
            "일정 자동 등록 완료"
        } else {
            "통화 분석 완료"
        }

        val contentText = if (firstSchedule != null) {
            "${firstSchedule.title} 외 ${scheduleCount}개"
        } else {
            "통화 내용이 분석되었습니다"
        }

        val bigText = if (firstSchedule != null) {
            buildString {
                append("새로운 일정이 통화에서 자동으로 분석되었습니다.\n\n")
                record.extractedSchedules?.forEachIndexed { index, schedule ->
                    append("📅 ${index + 1}. ${schedule.title}\n")
                    append("   ${schedule.startDate}\n")
                    if (schedule.description.isNotEmpty()) {
                        append("   ${schedule.description}\n")
                    }
                    append("\n")
                }
                append("클릭하여 확인하고 수정하세요.")
            }
        } else {
            "통화가 분석되었지만 일정 정보를 추출할 수 없었습니다.\n클릭하여 상세 내용을 확인하세요."
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(Random.nextInt(), notification)
    }

    /**
     * 오류 알림 표시
     */
    private fun showErrorNotification(title: String, message: String) {
        val channelId = "call_record_analysis"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("일정 등록 실패")
            .setContentText("$title: $message")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(Random.nextInt(), notification)
    }
}