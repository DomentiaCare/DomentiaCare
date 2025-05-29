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
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.domentiacare.MyApplication
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.Patient
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
    private val onPatientSelectionRequired: (List<Patient>) -> Unit,
    private val onNavigateToScreen: (route: String) -> Unit,
    private val onStateChanged: (() -> Unit)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isRecognizing = false
    private var isAnalyzing = false
    private var isRetrying = false
    private var isTTSPlaying = false
    private var pendingSpeechRecognition = false

    private var isWaitingForPatientSelection = false
    private var availablePatients = listOf<Patient>()

    private var currentAnalysisJob: Job? = null
    private var isForceStopping = false
    private var isDestroyed = false

    // 🆕 하드코딩된 연락처 제거 - 이제 동적으로 가져옴
    // private val contacts = mapOf(...) // 제거됨

    init {
        TTSServiceManager.init(context) {
            Log.d("AIAssistant", "TTS initialization completed")
        }
    }

    fun forceStop(showMessage: Boolean = true) {
        Log.d("AIAssistant", "🛑 forceStop() called - 모든 동작 강제 중지")

        isForceStopping = true

        try {
            currentAnalysisJob?.cancel()
            currentAnalysisJob = null
            Log.d("AIAssistant", "✅ Llama 분석 Job 취소됨")

            speechRecognizer?.let { recognizer ->
                try {
                    recognizer.stopListening()
                    recognizer.cancel()
                    Log.d("AIAssistant", "✅ 음성 인식 강제 중지됨")
                } catch (e: Exception) {
                    Log.e("AIAssistant", "❌ 음성 인식 중지 실패: ${e.message}")
                }
            }

            TTSServiceManager.stop()
            isTTSPlaying = false
            Log.d("AIAssistant", "✅ TTS 강제 중지됨")

            resetStateImmediately()

            if (showMessage) {
                Toast.makeText(context, "AI 어시스턴트가 중지되었습니다", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ 강제 중지 중 오류: ${e.message}", e)
        } finally {
            isForceStopping = false
            Log.d("AIAssistant", "🏁 강제 중지 완료")
        }
    }

    private fun resetStateImmediately() {
        isListening = false
        isRecognizing = false
        isAnalyzing = false
        isRetrying = false
        pendingSpeechRecognition = false
        isWaitingForPatientSelection = false
        availablePatients = emptyList()

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ SpeechRecognizer 정리 실패: ${e.message}")
        }

        Log.d("AIAssistant", "🔄 상태 즉시 초기화 완료")
        onStateChanged?.invoke()
    }

    fun activateAssistant() {
        if (isDestroyed || isForceStopping) {
            Log.w("AIAssistant", "⚠️ 어시스턴트가 파괴되었거나 강제 중지 중입니다")
            return
        }

        Log.d("AIAssistant", "🔧 activateAssistant() entry: isRecognizing=$isRecognizing, isListening=$isListening, isTTSPlaying=$isTTSPlaying")

        when {
            isRecognizing || isAnalyzing -> {
                Log.d("AIAssistant", "🛑 Recognition/Analysis in progress - 강제 중지")
                forceStop(showMessage = false)
            }
            isListening -> {
                Log.d("AIAssistant", "🎤 Waiting state detected - attempting to start speech recognition")
                startSpeechRecognitionSafe()
            }
            else -> {
                Log.d("AIAssistant", "🚀 Inactive state detected - starting activation")
                isListening = true
                Log.d("AIAssistant", "✅ isListening = true set")

                pendingSpeechRecognition = true
                speakKorean("네, 말씀하세요.") {
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

    private fun startSpeechRecognitionSafe() {
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - 음성 인식 시작 취소")
            return
        }

        if (isTTSPlaying) {
            Log.w("AIAssistant", "⚠️ TTS is playing, postponing speech recognition start")
            CoroutineScope(Dispatchers.Main).launch {
                delay(500)
                if (!isForceStopping && !isDestroyed && !isTTSPlaying && isListening && !isRecognizing) {
                    startSpeechRecognition()
                }
            }
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            if (!isForceStopping && !isDestroyed) {
                startSpeechRecognition()
            }
        }
    }

    private fun startSpeechRecognition() {
        if (isRecognizing || isForceStopping || isDestroyed) {
            Log.d("AIAssistant", "⚠️ 음성 인식 시작 불가 - isRecognizing=$isRecognizing, isForceStopping=$isForceStopping, isDestroyed=$isDestroyed")
            return
        }

        if (isTTSPlaying) {
            Log.w("AIAssistant", "⚠️ Cannot start speech recognition while TTS is playing")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("AIAssistant", "❌ STT not available on this device")
            speakKorean("이 기기에서는 음성 인식을 사용할 수 없습니다.")
            resetState()
            return
        }

        try {
            speechRecognizer?.destroy()

            val language = if (isWaitingForPatientSelection) "ko-KR" else "en-US"

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    if (isForceStopping || isDestroyed) {
                        Log.d("AIAssistant", "⚠️ 강제 중지 중 - STT 결과 무시")
                        return
                    }

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val resultText = matches?.firstOrNull() ?: "No result"

                    Log.d("AIAssistant", "✅ STT result: '$resultText'")

                    isRecognizing = false

                    if (resultText != "No result" && resultText.trim().length >= 2) {
                        if (isWaitingForPatientSelection) {
                            handlePatientSelection(resultText.trim())
                        } else {
                            isAnalyzing = true
                            Log.d("AIAssistant", "🔄 State changed: isRecognizing=false, isAnalyzing=true")
                            onStateChanged?.invoke()
                            analyzeWithLlama(resultText.trim())
                        }
                    } else {
                        Log.w("AIAssistant", "⚠️ Invalid STT result")
                        speakKorean("음성을 다시 말씀해주세요.")
                        resetState()
                    }
                }

                override fun onError(error: Int) {
                    if (isForceStopping || isDestroyed) {
                        Log.d("AIAssistant", "⚠️ 강제 중지 중 - STT 에러 무시")
                        return
                    }

                    Log.e("AIAssistant", "❌ STT error occurred: $error")

                    isRecognizing = false
                    isAnalyzing = false

                    when (error) {
                        7 -> {
                            if (!isRetrying) {
                                isRetrying = true
                                speakKorean("음성을 인식하지 못했습니다. 다시 시도합니다.") {
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
                        9 -> {
                            speakKorean("마이크 권한이 필요합니다.")
                            resetState()
                        }
                        else -> {
                            speakKorean("음성 인식 오류가 발생했습니다.")
                            resetState()
                        }
                    }
                }

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

            speechRecognizer?.startListening(intent)
            isRecognizing = true

            Log.d("AIAssistant", "🎤 STT started (${if (isWaitingForPatientSelection) "Korean" else "English"} mode)")

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

    private fun analyzeWithLlama(userInput: String) {
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - Llama 분석 취소")
            return
        }

        Log.d("AIAssistant", "🧠 Llama analysis started: '$userInput'")

        speakKorean("명령을 분석하고 있습니다.")

        currentAnalysisJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isActive || isForceStopping || isDestroyed) {
                    Log.d("AIAssistant", "⚠️ Llama 분석 Job 취소됨")
                    return@launch
                }

                val llamaManager = MyApplication.llamaServiceManager

                val prompt = """
                    User Input: "$userInput"
                    
                    Analyze the English input above and respond with ONLY ONE of these keywords:
                    
                    TOMORROW - tomorrow, tomorrow's schedule, what's tomorrow, tomorrow plan
                    TODAY - today, today's schedule, what's today, today plan, what am I doing today
                    CALL_CAREGIVER - call caregiver, call protector, call guardian, phone caregiver, phone guardian, phone 
                    CALL_PATIENT - call patient, phone patient, call elderly
                    FIND_PATIENT - find patient, locate patient, where is patient, patient location
                    FIND_CAREGIVER - find caregiver, locate caregiver, where is caregiver, caregiver location
                    HOME_NAVIGATE - home navigation, navigate home, go home, route to home, directions to home, want to go home
                    UNKNOWN - everything else not mentioned above
                    
                    IMPORTANT: Respond with ONLY the keyword. No additional text.
                    
                    Examples:
                    - "What's tomorrow's plan?" → TOMORROW
                    - "Tell me today's schedule" → TODAY  
                    - "Call my caregiver" → CALL_CAREGIVER
                    - "Where is my dad?" → FIND_CAREGIVER
                    - "Find the patient" → FIND_PATIENT
                    - "Navigate to home" -> HOME_NAVIGATE
                    - "What's the weather?" → UNKNOWN
                """.trimIndent()

                if (!isActive || isForceStopping || isDestroyed) {
                    Log.d("AIAssistant", "⚠️ Llama 쿼리 전 Job 취소됨")
                    return@launch
                }

                val llamaResponse = llamaManager.sendQueryBlocking(prompt)
                Log.d("AIAssistant", "🔤 Llama keyword response: '$llamaResponse'")

                if (isActive && !isForceStopping && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        executeCommand(llamaResponse, userInput)
                    }
                } else {
                    Log.d("AIAssistant", "⚠️ Llama 분석 완료 후 Job 취소됨")
                }

            } catch (e: CancellationException) {
                Log.d("AIAssistant", "✅ Llama 분석 Job이 정상적으로 취소됨")
            } catch (e: Exception) {
                Log.e("AIAssistant", "❌ Llama analysis failed: ${e.message}", e)
                if (!isForceStopping && !isDestroyed) {
                    withContext(Dispatchers.Main) {
                        speakKorean("명령을 분석할 수 없습니다. 다시 시도해주세요.")
                        resetState()
                    }
                }
            } finally {
                currentAnalysisJob = null
            }
        }
    }

    // 🆕 추가: setNavigationCallback 함수
    fun setNavigationCallback(callback: (String) -> Unit) {
        // 이미 생성자에서 onNavigateToScreen을 받으므로
        // 별도로 설정할 필요는 없지만, MainActivity에서 호출하므로 빈 함수로 제공
        Log.d("AIAssistant", "setNavigationCallback 호출됨 (이미 생성자에서 설정됨)")
    }

    /**
     * Execute command based on Llama keyword response
     */
    private fun executeCommand(llamaResponse: String, originalQuestion: String) {
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - 명령 실행 취소")
            return
        }

        isAnalyzing = false
        Log.d("AIAssistant", "🔄 State changed: isAnalyzing=false (analysis completed)")

        onStateChanged?.invoke()

        try {
            val keyword = llamaResponse.trim().uppercase()
            Log.d("AIAssistant", "🎯 Command to execute: '$keyword' (original: '$originalQuestion')")

            when {
                keyword.contains("TOMORROW") -> {
                    speakKorean("내일 일정을 확인해드리겠습니다.")
                    onScheduleAction("check", "tomorrow")
                }

                keyword.contains("TODAY") -> {
                    speakKorean("오늘 일정을 확인해드리겠습니다.")
                    onScheduleAction("check", "today")
                }

                keyword.contains("CALL_CAREGIVER") -> {
                    // 🆕 보호자 전화 걸기 - 실제 보호자 전화번호 가져오기
                    handleCallCaregiverRequest()
                }

                keyword.contains("CALL_PATIENT") -> {
                    handleCallPatientRequest()
                }

                keyword.contains("FIND_PATIENT") -> {
                    speakKorean("환자 위치를 확인해드리겠습니다.")
                    onScheduleAction("find", "patient")
                }

                keyword.contains("FIND_CAREGIVER") -> {
                    speakKorean("보호자 위치를 확인해드리겠습니다.")
                    onScheduleAction("find", "caregiver")
                }

                keyword.contains("HOME_NAVIGATE") -> {
                    speakKorean("집까지의 길을 안내해드리겠습니다.")
                    // navigate to HomeNavigationScreen
                    onNavigateToScreen("HomeNavigationScreen")
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

    // 🆕 보호자 전화 걸기 요청 처리
    private fun handleCallCaregiverRequest() {
        Log.d("AIAssistant", "📞 보호자 전화 요청 처리 시작")
        speakKorean("보호자 전화번호를 확인하고 있습니다.")

        // 보호자 전화번호 가져오기
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🆕 CurrentUser.user.id를 사용해서 보호자 전화번호 가져오기
                val response = RetrofitClient.authApi.getProtectorPhone(CurrentUser.user?.managerId ?: 0L)

                if (response.isSuccessful) {
                    val phoneNumber = response.body()?.string()?.trim()?.replace("\"", "")

                    withContext(Dispatchers.Main) {
                        if (!phoneNumber.isNullOrBlank()) {
                            speakKorean("보호자에게 전화를 걸겠습니다.")
                            makeDirectPhoneCall(phoneNumber, "보호자")
                        } else {
                            speakKorean("보호자 전화번호가 등록되어 있지 않습니다.")
                        }
                        resetState()
                    }
                } else {
                    Log.e("AIAssistant", "❌ 보호자 전화번호 조회 실패: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        speakKorean("보호자 전화번호를 가져올 수 없습니다.")
                        resetState()
                    }
                }

            } catch (e: Exception) {
                Log.e("AIAssistant", "❌ 보호자 전화번호 조회 실패: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    speakKorean("보호자 전화번호를 가져올 수 없습니다. 네트워크 연결을 확인해주세요.")
                    resetState()
                }
            }
        }
    }

    private fun handleCallPatientRequest() {
        Log.d("AIAssistant", "📞 환자 전화 요청 처리 시작")
        speakKorean("환자 목록을 확인하고 있습니다.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val patients = RetrofitClient.authApi.getPatients()
                Log.d("AIAssistant", "📋 환자 목록 조회 완료: ${patients.size}명")

                withContext(Dispatchers.Main) {
                    when {
                        patients.isEmpty() -> {
                            speakKorean("등록된 환자가 없습니다. 먼저 환자를 등록해주세요.")
                            resetState()
                        }
                        patients.size == 1 -> {
                            val patient = patients.first()
                            speakKorean("${patient.patientName}님에게 전화를 걸겠습니다.")
                            makePhoneCallToPatient(patient)
                            resetState()
                        }
                        else -> {
                            speakKorean("여러 명의 환자가 등록되어 있습니다. 화면에서 환자를 선택해주세요.")
                            onPatientSelectionRequired(patients)
                            resetState()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("AIAssistant", "❌ 환자 목록 조회 실패: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    speakKorean("환자 목록을 가져올 수 없습니다. 네트워크 연결을 확인해주세요.")
                    resetState()
                }
            }
        }
    }

    fun callPatient(patient: Patient) {
        Log.d("AIAssistant", "📞 선택된 환자에게 전화: ${patient.patientName}")
        makePhoneCallToPatient(patient)
    }

    private fun handlePatientSelection(spokenName: String) {
        Log.d("AIAssistant", "👤 환자 선택 처리: '$spokenName'")

        isWaitingForPatientSelection = false

        val selectedPatient = findPatientByName(spokenName, availablePatients)

        if (selectedPatient != null) {
            speakKorean("${selectedPatient.patientName}님에게 전화를 걸겠습니다.")
            makePhoneCallToPatient(selectedPatient)
        } else {
            speakKorean("말씀하신 환자를 찾을 수 없습니다. 다시 시도해주세요.")

            val patientNames = availablePatients.joinToString(", ") { it.patientName }
            speakKorean("등록된 환자는 ${patientNames}입니다. 환자 이름을 정확히 말씀해주세요.")

            isWaitingForPatientSelection = true
            isListening = true

            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                if (isWaitingForPatientSelection && !isForceStopping && !isDestroyed) {
                    startSpeechRecognitionSafe()
                }
            }
            return
        }

        availablePatients = emptyList()
        resetState()
    }

    private fun findPatientByName(spokenName: String, patients: List<Patient>): Patient? {
        val cleanSpokenName = spokenName.trim().replace(" ", "")

        patients.forEach { patient ->
            if (patient.patientName.replace(" ", "").equals(cleanSpokenName, ignoreCase = true)) {
                Log.d("AIAssistant", "✅ 정확한 이름 매칭: ${patient.patientName}")
                return patient
            }
        }

        patients.forEach { patient ->
            val patientName = patient.patientName.replace(" ", "")
            if (patientName.contains(cleanSpokenName, ignoreCase = true) ||
                cleanSpokenName.contains(patientName, ignoreCase = true)) {
                Log.d("AIAssistant", "✅ 부분 이름 매칭: ${patient.patientName}")
                return patient
            }
        }

        Log.w("AIAssistant", "❌ 매칭되는 환자를 찾을 수 없음: '$spokenName'")
        return null
    }

    private fun makePhoneCallToPatient(patient: Patient) {
        try {
            val phoneNumber = patient.phone
            if (phoneNumber.isNotBlank()) {
                Log.d("AIAssistant", "📞 환자 전화 시도: ${patient.patientName} ($phoneNumber)")
                makeDirectPhoneCall(phoneNumber, patient.patientName)
            } else {
                speakKorean("${patient.patientName}님의 전화번호가 등록되어 있지 않습니다.")
                Log.e("AIAssistant", "❌ 전화번호 없음: ${patient.patientName}")
            }
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ 환자 전화 실패: ${e.message}", e)
            speakKorean("환자에게 전화를 걸 수 없습니다.")
        }
    }

    // 🆕 바로 전화 걸기 함수 (ACTION_CALL 사용)
    private fun makeDirectPhoneCall(phoneNumber: String, contactName: String) {
        try {
            Log.d("AIAssistant", "📞 직접 전화 걸기: $contactName ($phoneNumber)")

            // 🆕 ACTION_CALL을 사용하여 바로 전화 걸기
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)

            Log.d("AIAssistant", "✅ 직접 전화 걸기 성공: $contactName ($phoneNumber)")

        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ 직접 전화 걸기 실패: ${e.message}", e)
            speakKorean("전화를 걸 수 없습니다.")
        }
    }

    private fun speakKorean(text: String, onComplete: (() -> Unit)? = null) {
        if (isForceStopping || isDestroyed) {
            Log.w("AIAssistant", "⚠️ 강제 중지 중이거나 파괴된 상태 - TTS 취소")
            onComplete?.invoke()
            return
        }

        isTTSPlaying = true
        Log.d("AIAssistant", "🔊 TTS started: '$text', isTTSPlaying=true")

        TTSServiceManager.speak(text) {
            isTTSPlaying = false
            Log.d("AIAssistant", "🔊 TTS completed: '$text', isTTSPlaying=false")

            if (!isForceStopping && !isDestroyed) {
                onComplete?.invoke()
            }
        }
    }

    fun stopListening() {
        Log.d("AIAssistant", "🛑 Assistant stopped by user request")
        forceStop(showMessage = true)
    }

    private fun resetState() {
        isListening = false
        isRecognizing = false
        isAnalyzing = false
        isRetrying = false
        pendingSpeechRecognition = false
        isWaitingForPatientSelection = false
        availablePatients = emptyList()

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("AIAssistant", "❌ Failed to clean up SpeechRecognizer: ${e.message}", e)
        }

        Log.d("AIAssistant", "🔄 Assistant state reset")
        onStateChanged?.invoke()
    }

    fun isActive(): Boolean = isListening || isRecognizing || isAnalyzing || isWaitingForPatientSelection

    fun isCurrentlyRecording(): Boolean {
        val result = isRecognizing
        Log.d("AIAssistant", "🔍 isCurrentlyRecording(): $result")
        return result
    }

    fun isCurrentlyAnalyzing(): Boolean {
        val result = isAnalyzing
        Log.d("AIAssistant", "🔍 isCurrentlyAnalyzing(): $result")
        return result
    }

    fun isWaiting(): Boolean {
        val result = (isListening && !isRecognizing && !isAnalyzing) || isWaitingForPatientSelection
        Log.d("AIAssistant", "🔍 isWaiting(): $result (isListening=$isListening, isRecognizing=$isRecognizing, isAnalyzing=$isAnalyzing, isWaitingForPatientSelection=$isWaitingForPatientSelection)")
        return result
    }

    fun isTTSCurrentlyPlaying(): Boolean = isTTSPlaying

    fun isForceStopping(): Boolean = isForceStopping

    fun isWaitingForPatientSelection(): Boolean = isWaitingForPatientSelection

    fun destroy() {
        Log.d("AIAssistant", "🧹 AI Assistant resource cleanup")

        isDestroyed = true

        forceStop(showMessage = false)

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