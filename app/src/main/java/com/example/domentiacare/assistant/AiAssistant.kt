package com.example.domentiacare.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import com.example.domentiacare.MyApplication
import com.example.domentiacare.service.androidtts.TTSServiceManager
import kotlinx.coroutines.*
import java.util.*

class AIAssistant(
    private val context: Context,
    private val onScheduleAction: (action: String, details: String) -> Unit,
    private val onStateChanged: (() -> Unit)? = null // 🆕 상태 변경 콜백 추가
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isRecognizing = false
    private var isAnalyzing = false // 🆕 Llama 분석 중 상태 추가
    private var isRetrying = false
    private var isTTSPlaying = false
    private var pendingSpeechRecognition = false


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
            //tts정상작동 테스트 (이종범)
            //speakKorean("음성 서비스가 준비되었습니다.")
        }
    }

    /**
     * Activate AI Assistant (when floating button is clicked)
     */
    fun activateAssistant() {
        Log.d("AIAssistant", "🔧 activateAssistant() entry: isRecognizing=$isRecognizing, isListening=$isListening, isTTSPlaying=$isTTSPlaying")

        when {
            isRecognizing -> {
                // Currently recognizing → stop recognition
                Log.d("AIAssistant", "🛑 Recognition in progress - stopping recognition")
                stopSpeechRecognition()
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
                    // Start speech recognition in TTS completion callback
                    Log.d("AIAssistant", "🔊 TTS completed - starting speech recognition")
                    if (pendingSpeechRecognition && isListening && !isRecognizing) {
                        startSpeechRecognitionSafe()
                    }
                    pendingSpeechRecognition = false
                    // 🆕 TTS 완료 후에도 UI 업데이트
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
        if (isTTSPlaying) {
            Log.w("AIAssistant", "⚠️ TTS is playing, postponing speech recognition start")
            // Wait briefly and retry after TTS completion
            CoroutineScope(Dispatchers.Main).launch {
                delay(500) // Retry after 0.5 seconds
                if (!isTTSPlaying && isListening && !isRecognizing) {
                    startSpeechRecognition()
                }
            }
            return
        }

        // Ensure we're on the main thread
        CoroutineScope(Dispatchers.Main).launch {
            startSpeechRecognition()
        }
    }

    /**
     * Start STT (Speech-to-Text) - Must be called from Main Thread
     */
    private fun startSpeechRecognition() {
        if (isRecognizing) {
            Log.d("AIAssistant", "⚠️ Already recognizing speech")
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
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val resultText = matches?.firstOrNull() ?: "No result"

                    Log.d("AIAssistant", "✅ STT result: '$resultText'")

                    // STT 완료, 분석 시작 상태로 변경
                    isRecognizing = false
                    isAnalyzing = true
                    Log.d("AIAssistant", "🔄 State changed: isRecognizing=false, isAnalyzing=true")

                    // 🆕 UI에 상태 변경 알림
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
                                        delay(1000) // Wait 1 additional second
                                        if (isListening && !isRecognizing && !isTTSPlaying) {
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
                    // 🆕 음성 입력 시작 시에도 UI 업데이트
                    onStateChanged?.invoke()
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d("AIAssistant", "🛑 Speech input ended")
                    // onResults가 호출되지 않을 경우를 대비해 여기서는 상태 변경하지 않음
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
            isAnalyzing = false // 🆕 중지 시 분석 상태도 초기화
            Log.d("AIAssistant", "🛑 Speech recognition stopped")
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Failed to stop speech recognition: ${e.message}", e)
            resetState()
        }
    }

    /**
     * Analyze English commands with Llama
     */
    private fun analyzeWithLlama(userInput: String) {
        Log.d("AIAssistant", "🧠 Llama analysis started: '$userInput'")

        speakKorean("명령을 분석하고 있습니다.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
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

                val llamaResponse = llamaManager.sendQueryBlocking(prompt)
                Log.d("AIAssistant", "🔤 Llama keyword response: '$llamaResponse'")

                withContext(Dispatchers.Main) {
                    executeCommand(llamaResponse, userInput)
                }

            } catch (e: Exception) {
                Log.e("AIAssistant", "❌ Llama analysis failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    speakKorean("명령을 분석할 수 없습니다. 다시 시도해주세요.")
                    resetState()
                }
            }
        }
    }

    /**
     * Execute command based on Llama keyword response
     */

    //해당 함수에서 각 명령어에 맞게 화면 이동 또는 내용이 구현되어야함.
    private fun executeCommand(llamaResponse: String, originalQuestion: String) {
        // 분석 완료
        isAnalyzing = false
        Log.d("AIAssistant", "🔄 State changed: isAnalyzing=false (analysis completed)")

        // 🆕 UI에 상태 변경 알림
        onStateChanged?.invoke()

        try {
            val keyword = llamaResponse.trim().uppercase()
            Log.d("AIAssistant", "🎯 Command to execute: '$keyword' (original: '$originalQuestion')")

            when {
                keyword.contains("TOMORROW") -> {
                    speakKorean("내일 일정을 확인해드리겠습니다.")
                    onScheduleAction("check", "tomorrow") // -> 이건 의미 있는지 모르겠음 (이종범)
                    //여기에 내일 일정을 등록하는 내용 삽입하기 (이종범)
                }

                keyword.contains("TODAY") -> {
                    speakKorean("오늘 일정을 확인해드리겠습니다.")
                    onScheduleAction("check", "today")
                    //여기에 오늘 일정을 등록하는 내용 삽입하기 (이종범)
                }

                keyword.contains("CALL_CAREGIVER") -> {
                    speakKorean("보호자에게 전화드리겠습니다.")
                    makePhoneCall("caregiver", "caregiver")
                    //여기에 보호자 전화번호로 전화하는 내용 삽입하기 (이종범)
                }

                keyword.contains("CALL_PATIENT") -> {
                    speakKorean("환자에게 전화드리겠습니다.")
                    makePhoneCall("patient", "patient")
                    //여기에 환자 전화번호로 전화하는 내용 삽입하기 (이종범)
                }

                keyword.contains("FIND_PATIENT") -> {
                    speakKorean("환자 위치를 확인해드리겠습니다.")
                    onScheduleAction("find", "patient")
                    //여기에 환자 위치로 길안내 하는 내용 삽입하기 (이종범)
                }

                keyword.contains("FIND_CAREGIVER") -> {
                    speakKorean("보호자 위치를 확인해드리겠습니다.")
                    onScheduleAction("find", "caregiver")
                    //여기에 보호자 위치로 길안내 하는 내용 삽입하기 (이종범)
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

        onStateChanged?.invoke() // 🆕 상태 변경 알림
    }

    /**
     * Make phone call
     */
    private fun makePhoneCall(contactKey: String, contactName: String) {
        try {
            val phoneNumber = contacts[contactKey]
            if (phoneNumber != null) {
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
                Log.d("AIAssistant", "📞 Making call: $contactName ($phoneNumber)")
            } else {
                speakKorean("연락처 정보를 찾을 수 없습니다.")
                Log.e("AIAssistant", "❌ Contact not found: $contactKey")
            }
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Failed to make phone call: ${e.message}", e)
            speakKorean("전화를 걸 수 없습니다.")
        }
    }

    /**
     * Safe TTS voice output (with state management)
     */
    private fun speakKorean(text: String, onComplete: (() -> Unit)? = null) {
        isTTSPlaying = true
        Log.d("AIAssistant", "🔊 TTS started: '$text', isTTSPlaying=true")

        TTSServiceManager.speak(text) {
            // TTS completion callback
            isTTSPlaying = false
            Log.d("AIAssistant", "🔊 TTS completed: '$text', isTTSPlaying=false")
            onComplete?.invoke()
        }
    }

    /**
     * Stop assistant
     */
    fun stopListening() {
        Log.d("AIAssistant", "🛑 Assistant stopped by user request")

        if (isRecognizing) {
            stopSpeechRecognition()
        }

        speakKorean("음성 인식을 중지했습니다.")
        resetState()
    }

    /**
     * Reset state
     */
    private fun resetState() {
        isListening = false
        isRecognizing = false
        isAnalyzing = false // 🆕 분석 상태도 초기화
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

        // 🆕 UI에 상태 변경 알림
        onStateChanged?.invoke()
    }

    /**
     * Check assistant state
     */
    fun isActive(): Boolean = isListening || isRecognizing || isAnalyzing // 🆕 분석 중도 활성 상태

    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean {
        val result = isRecognizing
        Log.d("AIAssistant", "🔍 isCurrentlyRecording(): $result")
        return result
    }

    /**
     * 🆕 Check if currently analyzing with Llama
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
     * Clean up resources
     */
    fun destroy() {
        Log.d("AIAssistant", "🧹 AI Assistant resource cleanup")

        stopListening()
        TTSServiceManager.shutdown()

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Failed to clean up SpeechRecognizer: ${e.message}", e)
        }
    }
}