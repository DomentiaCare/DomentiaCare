package com.example.domentiacare.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.example.domentiacare.MyApplication
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.service.androidtts.TTSServiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIAssistant(
    private val context: Context,
    private val onScheduleAction: (action: String, details: String) -> Unit,
    private val onStateChanged: (() -> Unit)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isRecognizing = false
    private var isAnalyzing = false
    private var isRetrying = false
    private var isTTSPlaying = false
    private var pendingSpeechRecognition = false

    // 🆕 강제 중지를 위한 변수들 추가
    private var currentAnalysisJob: Job? = null // Llama 분석 Job 추적
    private var isForceStopping = false // 강제 중지 중인지 확인
    private var isDestroyed = false // 어시스턴트가 파괴되었는지 확인

    //해당 내용은 보호자나 환자의 전화번호로 대체해야함. (DB에서 정보가져와서 연결해야할 부분)
    private val contacts = mapOf(
        "caregiver" to "010-1234-5678",
        "patient" to "010-9876-5432",
        "hospital" to "02-123-4567",
        "clinic" to "02-234-5678",
        "pharmacy" to "02-345-6789",
        "emergency" to "119"
    )

    init {
        TTSServiceManager.init(context) {
            Log.d("AIAssistant", "TTS initialization completed")
        }
    }

    /**
     * 🆕 강제 중지 함수 - 모든 동작을 즉시 중단
     */
    fun forceStop(showMessage: Boolean = true) {
        Log.d("AIAssistant", "🛑 forceStop() called - 모든 동작 강제 중지")

        // 강제 중지 플래그 설정
        isForceStopping = true

        try {
            // 1. Llama 분석 Job 취소
            currentAnalysisJob?.cancel()
            currentAnalysisJob = null
            Log.d("AIAssistant", "✅ Llama 분석 Job 취소됨")

            // 2. 음성 인식 즉시 중지
            speechRecognizer?.let { recognizer ->
                try {
                    recognizer.stopListening()
                    recognizer.cancel() // 🆕 cancel() 추가로 더 강력한 중지
                    Log.d("AIAssistant", "✅ 음성 인식 강제 중지됨")
                } catch (e: Exception) {
                    Log.e("AIAssistant", "❌ 음성 인식 중지 실패: ${e.message}")
                }
            }

            // 3. TTS 즉시 중지
            TTSServiceManager.stop()
            isTTSPlaying = false
            Log.d("AIAssistant", "✅ TTS 강제 중지됨")

            // 4. 모든 상태 즉시 초기화
            resetStateImmediately()

            // 5. 사용자에게 알림 (선택적)
            if (showMessage) {
                // TTS로 알림 대신 Toast 사용 (더 빠른 피드백)
                Toast.makeText(context, "AI 어시스턴트가 중지되었습니다", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ 강제 중지 중 오류: ${e.message}", e)
        } finally {
            // 강제 중지 플래그 해제
            isForceStopping = false
            Log.d("AIAssistant", "🏁 강제 중지 완료")
        }
    }

    /**
     * 🆕 즉시 상태 초기화 (기존 resetState()와 다르게 TTS 기다리지 않음)
     */
    private fun resetStateImmediately() {
        isListening = false
        isRecognizing = false
        isAnalyzing = false
        isRetrying = false
        pendingSpeechRecognition = false
        // isTTSPlaying은 위에서 이미 false로 설정됨

        // SpeechRecognizer 정리
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ SpeechRecognizer 정리 실패: ${e.message}")
        }

        Log.d("AIAssistant", "🔄 상태 즉시 초기화 완료")

        // UI 상태 업데이트
        onStateChanged?.invoke()
    }

    /**
     * Activate AI Assistant (when floating button is clicked)
     */
    fun activateAssistant() {
        // 🆕 파괴된 상태이거나 강제 중지 중이면 무시
        if (isDestroyed || isForceStopping) {
            Log.w("AIAssistant", "⚠️ 어시스턴트가 파괴되었거나 강제 중지 중입니다")
            return
        }

        Log.d("AIAssistant", "🔧 activateAssistant() entry: isRecognizing=$isRecognizing, isListening=$isListening, isTTSPlaying=$isTTSPlaying")

        when {
            isRecognizing || isAnalyzing -> {
                // 🆕 현재 인식 중이거나 분석 중 → 강제 중지
                Log.d("AIAssistant", "🛑 Recognition/Analysis in progress - 강제 중지")
                forceStop(showMessage = false) // 메시지 없이 조용히 중지
            }
            isListening -> {
                // Waiting → start speech recognition (only when TTS is not playing)
                Log.d("AIAssistant", "🎤 Waiting state detected - attempting to start speech recognition")
                startSpeechRecognitionSafe()
            }
            else -> {
                Log.d("AIAssistant", "🚀 Inactive state detected - starting activation")
                // Inactive → activate and start speech recognition
                isListening = true
                Log.d("AIAssistant", "✅ isListening = true set")

                // Set to start speech recognition after TTS completion
                pendingSpeechRecognition = true
                speakKorean("네, 말씀하세요.") {
                    // 🆕 강제 중지 중이면 음성 인식 시작하지 않음
                    if (!isForceStopping && pendingSpeechRecognition && isListening && !isRecognizing) {
                        Log.d("AIAssistant", "🔊 TTS completed - starting speech recognition")
                        startSpeechRecognitionSafe()
                    }
                    pendingSpeechRecognition = false
                    onStateChanged?.invoke()
                }
            }
        }

        Log.d("AIAssistant", "🏁 activateAssistant() completed: isRecognizing=$isRecognizing, isListening=$isListening, isTTSPlaying=$isTTSPlaying")
    }

    /**
     * Safe speech recognition start (checking TTS playback)
     */
    private fun startSpeechRecognitionSafe() {
        // 🆕 강제 중지 중이거나 파괴된 상태면 시작하지 않음
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - 음성 인식 시작 취소")
            return
        }

        if (isTTSPlaying) {
            Log.w("AIAssistant", "⚠️ TTS is playing, postponing speech recognition start")
            // Wait briefly and retry after TTS completion
            CoroutineScope(Dispatchers.Main).launch {
                delay(500)
                if (!isForceStopping && !isDestroyed && !isTTSPlaying && isListening && !isRecognizing) {
                    startSpeechRecognition()
                }
            }
            return
        }

        // Ensure we're on the main thread
        CoroutineScope(Dispatchers.Main).launch {
            if (!isForceStopping && !isDestroyed) {
                startSpeechRecognition()
            }
        }
    }

    /**
     * Start STT (Speech-to-Text) - Must be called from Main Thread
     */
    private fun startSpeechRecognition() {
        // 🆕 추가 안전 검사
        if (isRecognizing || isForceStopping || isDestroyed) {
            Log.d("AIAssistant", "⚠️ 음성 인식 시작 불가 - isRecognizing=$isRecognizing, isForceStopping=$isForceStopping, isDestroyed=$isDestroyed")
            return
        }

        if (isTTSPlaying) {
            Log.w("AIAssistant", "⚠️ Cannot start speech recognition while TTS is playing")
            return
        }

        // Check STT availability
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("AIAssistant", "❌ STT not available on this device")
            speakKorean("이 기기에서는 음성 인식을 사용할 수 없습니다.")
            resetState()
            return
        }

        try {
            // Clean up existing SpeechRecognizer
            speechRecognizer?.destroy()

            // Create Intent for English STT
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // English
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) // Prefer online for better accuracy

            // Create SpeechRecognizer (must be on main thread)
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    // 🆕 강제 중지 중이면 결과 무시
                    if (isForceStopping || isDestroyed) {
                        Log.d("AIAssistant", "⚠️ 강제 중지 중 - STT 결과 무시")
                        return
                    }

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val resultText = matches?.firstOrNull() ?: "No result"

                    Log.d("AIAssistant", "✅ STT result: '$resultText'")

                    // STT 완료, 분석 시작 상태로 변경
                    isRecognizing = false
                    isAnalyzing = true
                    Log.d("AIAssistant", "🔄 State changed: isRecognizing=false, isAnalyzing=true")

                    onStateChanged?.invoke()

                    if (resultText != "No result" && resultText.trim().length >= 2) {
                        // Proceed with Llama analysis
                        analyzeWithLlama(resultText.trim())
                    } else {
                        Log.w("AIAssistant", "⚠️ Invalid STT result")
                        speakKorean("음성을 다시 말씀해주세요.")
                        resetState()
                    }
                }

                override fun onError(error: Int) {
                    // 🆕 강제 중지 중이면 에러 무시
                    if (isForceStopping || isDestroyed) {
                        Log.d("AIAssistant", "⚠️ 강제 중지 중 - STT 에러 무시")
                        return
                    }

                    Log.e("AIAssistant", "❌ STT error occurred: $error")

                    // STT 오류 시 상태 정리
                    isRecognizing = false
                    isAnalyzing = false

                    when (error) {
                        7 -> { // ERROR_NO_MATCH
                            if (!isRetrying) {
                                isRetrying = true
                                speakKorean("음성을 인식하지 못했습니다. 다시 시도합니다.") {
                                    // Retry after TTS completion
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(1000)
                                        if (!isForceStopping && !isDestroyed && isListening && !isRecognizing && !isTTSPlaying) {
                                            startSpeechRecognition()
                                        }
                                        isRetrying = false
                                    }
                                }
                            } else {
                                speakKorean("음성 인식에 실패했습니다.")
                                resetState()
                            }
                        }
                        9 -> { // ERROR_INSUFFICIENT_PERMISSIONS
                            speakKorean("마이크 권한이 필요합니다.")
                            resetState()
                        }
                        else -> {
                            speakKorean("음성 인식 오류가 발생했습니다.")
                            resetState()
                        }
                    }
                }

                // Required methods
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("AIAssistant", "🎤 Speech recognition ready")
                }
                override fun onBeginningOfSpeech() {
                    Log.d("AIAssistant", "🗣️ Speech input started")
                    onStateChanged?.invoke()
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d("AIAssistant", "🛑 Speech input ended")
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            // Start listening
            speechRecognizer?.startListening(intent)
            isRecognizing = true

            Log.d("AIAssistant", "🎤 STT started (English mode)")

        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ STT start failed: ${e.message}", e)
            speakKorean("음성 인식을 시작할 수 없습니다.")
            resetState()
        }
    }

    private fun stopSpeechRecognition() {
        try {
            speechRecognizer?.stopListening()
            isRecognizing = false
            isAnalyzing = false
            Log.d("AIAssistant", "🛑 Speech recognition stopped")
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Failed to stop speech recognition: ${e.message}", e)
            resetState()
        }
    }

    /**
     * 🆕 수정된 Analyze English commands with Llama - Job 추적 추가
     */
    private fun analyzeWithLlama(userInput: String) {
        // 🆕 강제 중지 중이거나 파괴된 상태면 분석하지 않음
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - Llama 분석 취소")
            return
        }

        Log.d("AIAssistant", "🧠 Llama analysis started: '$userInput'")

        speakKorean("명령을 분석하고 있습니다.")

        // 🆕 현재 분석 Job을 추적하여 필요시 취소할 수 있도록 함
        currentAnalysisJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🆕 Job이 취소되었는지 확인
                if (!isActive || isForceStopping || isDestroyed) {
                    Log.d("AIAssistant", "⚠️ Llama 분석 Job 취소됨")
                    return@launch
                }

                val llamaManager = MyApplication.llamaServiceManager

                // English-focused prompt
                val prompt = """
                    User Input: "$userInput"
                    
                    Analyze the English input above and respond with ONLY ONE of these keywords:
                    
                    TOMORROW - tomorrow, tomorrow's schedule, what's tomorrow, tomorrow plan
                    TODAY - today, today's schedule, what's today, today plan, what am I doing today
                    CALL_CAREGIVER - call caregiver, call family, call guardian, call mom, call dad, phone caregiver
                    CALL_PATIENT - call patient, phone patient, call elderly
                    FIND_PATIENT - find patient, locate patient, where is patient, patient location
                    FIND_CAREGIVER - find caregiver, locate caregiver, where is caregiver, caregiver location
                    UNKNOWN - everything else not mentioned above
                    
                    IMPORTANT: Respond with ONLY the keyword. No additional text.
                    
                    Examples:
                    - "What's tomorrow's plan?" → TOMORROW
                    - "Tell me today's schedule" → TODAY  
                    - "Call my caregiver" → CALL_CAREGIVER
                    - "Where is my dad?" → FIND_CAREGIVER
                    - "Find the patient" → FIND_PATIENT
                    - "What's the weather?" → UNKNOWN
                """.trimIndent()

                // 🆕 다시 한 번 취소 확인
                if (!isActive || isForceStopping || isDestroyed) {
                    Log.d("AIAssistant", "⚠️ Llama 쿼리 전 Job 취소됨")
                    return@launch
                }

                val llamaResponse = llamaManager.sendQueryBlocking(prompt)
                Log.d("AIAssistant", "🔤 Llama keyword response: '$llamaResponse'")

                // 🆕 결과 처리 전 마지막 취소 확인
                if (isActive && !isForceStopping && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        executeCommand(llamaResponse, userInput)
                    }
                } else {
                    Log.d("AIAssistant", "⚠️ Llama 분석 완료 후 Job 취소됨")
                }

            } catch (e: CancellationException) {
                Log.d("AIAssistant", "✅ Llama 분석 Job이 정상적으로 취소됨")
                // CancellationException은 정상적인 취소이므로 별도 처리 불필요
            } catch (e: Exception) {
                Log.e("AIAssistant", "❌ Llama analysis failed: ${e.message}", e)
                if (!isForceStopping && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        speakKorean("명령을 분석할 수 없습니다. 다시 시도해주세요.")
                        resetState()
                    }
                }
            } finally {
                // Job 추적 해제
                currentAnalysisJob = null
            }
        }
    }

    /**
     * Execute command based on Llama keyword response
     */
    private fun executeCommand(llamaResponse: String, originalQuestion: String) {
        // 🆕 강제 중지 중이거나 파괴된 상태면 명령 실행하지 않음
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - 명령 실행 취소")
            return
        }

        // 분석 완료
        isAnalyzing = false
        Log.d("AIAssistant", "🔄 State changed: isAnalyzing=false (analysis completed)")

        onStateChanged?.invoke()

        try {
            val keyword = llamaResponse.trim().uppercase()
            Log.d("AIAssistant", "🎯 Command to execute: '$keyword' (original: '$originalQuestion')")
//박진호
            when {
                keyword.contains("TOMORROW") -> {
                    speakKorean("내일 일정을 확인해드리겠습니다.")
                    onScheduleAction("check", "tomorrow")
                    // -> 
                }

                keyword.contains("TODAY") -> {
                    speakKorean("오늘 일정을 확인해드리겠습니다.")
                    onScheduleAction("check", "today")
                }

                keyword.contains("CALL_CAREGIVER") -> {
                    speakKorean("보호자에게 전화드리겠습니다.")
                    callMethod(CurrentUser.user?.managerPhone ?: "010-5067-5629") // 보호자 전화번호로 대체 필요

                }

                keyword.contains("CALL_PATIENT") -> {
                    speakKorean("환자에게 전화드리겠습니다.")
                    //makePhoneCall("patient", "patient")
                    // 해당 부분에 callMethod 함수 호출 전에 DB에서 환자 리스트를 가져와서 선택을 해야함.
                    //
                }

                keyword.contains("FIND_PATIENT") -> {
                    speakKorean("환자 위치를 확인해드리겠습니다.")
                    onScheduleAction("find", "patient")
                }

                keyword.contains("FIND_CAREGIVER") -> {
                    speakKorean("보호자 위치를 확인해드리겠습니다.")
                    onScheduleAction("find", "caregiver")
                }

                else -> {
                    Log.d("AIAssistant", "⚠️ Unsupported command: '$keyword'")
                    speakKorean("죄송합니다. 아직 지원하지 않는 명령입니다. 일정 확인, 전화 걸기, 또는 위치 찾기를 시도해보세요.")
                }
            }

        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Command execution failed: ${e.message}", e)
            speakKorean("명령 실행 중 오류가 발생했습니다.")
        }

        resetState()
        onStateChanged?.invoke()
    }


    /**
     * 🆕 TTS에 중지 기능 추가된 Safe TTS voice output
     */
    private fun speakKorean(text: String, onComplete: (() -> Unit)? = null) {
        // 🆕 강제 중지 중이거나 파괴된 상태면 TTS 실행하지 않음
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - TTS 취소")
            onComplete?.invoke() // 콜백은 실행해서 대기 상태가 무한 대기하지 않도록 함
            return
        }

        isTTSPlaying = true
        Log.d("AIAssistant", "🔊 TTS started: '$text', isTTSPlaying=true")

        TTSServiceManager.speak(text) {
            // TTS completion callback
            isTTSPlaying = false
            Log.d("AIAssistant", "🔊 TTS completed: '$text', isTTSPlaying=false")

            // 🆕 TTS 완료 후에도 강제 중지 상태 확인
            if (!isForceStopping && !isDestroyed) {
                onComplete?.invoke()
            }
        }
    }

    /**
     * 🆕 수정된 Stop assistant - 강제 중지 사용
     */
    fun stopListening() {
        Log.d("AIAssistant", "🛑 Assistant stopped by user request")
        forceStop(showMessage = true) // 메시지와 함께 강제 중지
    }

    /**
     * Reset state
     */
    private fun resetState() {
        isListening = false
        isRecognizing = false
        isAnalyzing = false
        isRetrying = false
        pendingSpeechRecognition = false
        // isTTSPlaying is only set to false in TTS completion callback

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Failed to clean up SpeechRecognizer: ${e.message}", e)
        }

        Log.d("AIAssistant", "🔄 Assistant state reset")
        onStateChanged?.invoke()
    }

    /**
     * Check assistant state
     */
    fun isActive(): Boolean = isListening || isRecognizing || isAnalyzing

    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean {
        val result = isRecognizing
        Log.d("AIAssistant", "🔍 isCurrentlyRecording(): $result")
        return result
    }

    /**
     * Check if currently analyzing with Llama
     */
    fun isCurrentlyAnalyzing(): Boolean {
        val result = isAnalyzing
        Log.d("AIAssistant", "🔍 isCurrentlyAnalyzing(): $result")
        return result
    }

    /**
     * Check if waiting (activated but not recognizing)
     */
    fun isWaiting(): Boolean {
        val result = isListening && !isRecognizing && !isAnalyzing
        Log.d("AIAssistant", "🔍 isWaiting(): $result (isListening=$isListening, isRecognizing=$isRecognizing, isAnalyzing=$isAnalyzing)")
        return result
    }

    /**
     * Check if TTS is currently playing
     */
    fun isTTSCurrentlyPlaying(): Boolean = isTTSPlaying

    /**
     * 🆕 강제 중지 중인지 확인
     */
    fun isForceStopping(): Boolean = isForceStopping

    /**
     * 🆕 수정된 Clean up resources - 강제 중지 포함
     */
    fun destroy() {
        Log.d("AIAssistant", "🧹 AI Assistant resource cleanup")

        // 파괴 상태 표시
        isDestroyed = true

        // 강제 중지 (메시지 없이)
        forceStop(showMessage = false)

        // TTS 서비스 종료
        TTSServiceManager.shutdown()

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Failed to clean up SpeechRecognizer: ${e.message}", e)
        }

        Log.d("AIAssistant", "✅ AI Assistant 완전히 파괴됨")
    }



    fun callMethod(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 있으면 바로 전화 걸기
            makeCall(phoneNumber)
        } else {
            // 권한이 없으면 설정 화면으로 이동
            showPermissionSettings()
        }
    }

    private fun makeCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d("SimplePhoneCallManager", "📞 전화 걸기: $phoneNumber")
        } catch (e: Exception) {
            Log.e("SimplePhoneCallManager", "❌ 전화 걸기 실패: ${e.message}")
            Toast.makeText(context, "전화를 걸 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionSettings() {
        Toast.makeText(context, "전화 권한이 필요합니다. 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show()

        try {
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
        } catch (e: Exception) {
            Log.e("SimplePhoneCallManager", "❌ 설정 화면 열기 실패: ${e.message}")
        }
    }
}