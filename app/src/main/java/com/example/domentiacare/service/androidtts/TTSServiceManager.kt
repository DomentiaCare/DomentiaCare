package com.example.domentiacare.service.androidtts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

object TTSServiceManager {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var selectedVoice: Voice? = null
    private var utteranceId = 0 // 발화 ID 관리
    private val completionCallbacks = mutableMapOf<String, () -> Unit>() // 완료 콜백 저장

    fun init(context: Context, onInit: (() -> Unit)? = null) {
        if (tts != null) return

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // 🆕 UtteranceProgressListener 설정
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("TTS", "🔊 TTS 시작: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("TTS", "✅ TTS 완료: $utteranceId")
                        utteranceId?.let { id ->
                            // 완료 콜백 실행
                            completionCallbacks[id]?.invoke()
                            completionCallbacks.remove(id) // 콜백 제거
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e("TTS", "❌ TTS 오류: $utteranceId")
                        utteranceId?.let { id ->
                            // 오류 시에도 콜백 실행 (상태 정리를 위해)
                            completionCallbacks[id]?.invoke()
                            completionCallbacks.remove(id)
                        }
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        Log.d("TTS", "🛑 TTS 중지: $utteranceId, interrupted: $interrupted")
                        utteranceId?.let { id ->
                            completionCallbacks[id]?.invoke()
                            completionCallbacks.remove(id)
                        }
                    }
                })

                // 한국어 음성 설정
                val koreanVoices = tts?.voices?.filter { it.locale.language == "ko" }?.toList() ?: emptyList()
                if (koreanVoices.size >= 2) {
                    selectedVoice = koreanVoices[1]
                    tts?.voice = selectedVoice
                    Log.d("TTS", "두 번째 한국어 voice 사용: ${selectedVoice?.name}")
                } else if (koreanVoices.isNotEmpty()) {
                    selectedVoice = koreanVoices[0]
                    tts?.voice = selectedVoice
                    Log.d("TTS", "한국어 voice가 1개뿐이어서 첫 번째 사용: ${selectedVoice?.name}")
                } else {
                    tts?.language = Locale.KOREAN
                    Log.d("TTS", "한국어 voice가 없어 Locale만 설정")
                }

                isReady = true
                onInit?.invoke()
                Log.d("TTS", "TTS 초기화 성공")
            } else {
                Log.e("TTS", "TTS 초기화 실패")
            }
        }
    }

    /**
     * 🆕 완료 콜백 없는 기본 speak 메서드 (기존 호환성 유지)
     */
    fun speak(text: String) {
        speak(text, null)
    }

    /**
     * 🆕 완료 콜백 포함 speak 메서드
     */
    fun speak(text: String, onComplete: (() -> Unit)?) {
        if (!isReady) {
            Log.e("TTS", "TTS 아직 초기화 안됨")
            onComplete?.invoke() // TTS가 준비되지 않았어도 콜백 호출
            return
        }

        val currentUtteranceId = "TTS_${++utteranceId}" // 고유 ID 생성

        // 완료 콜백이 있으면 저장
        onComplete?.let { callback ->
            completionCallbacks[currentUtteranceId] = callback
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentUtteranceId)

        if (result == TextToSpeech.ERROR) {
            Log.e("TTS", "❌ TTS speak 실패")
            // 에러 시 콜백 즉시 실행 및 제거
            onComplete?.invoke()
            completionCallbacks.remove(currentUtteranceId)
        } else {
            Log.d("TTS", "🔊 TTS speak 요청 성공: $currentUtteranceId, 텍스트: '${text.take(20)}${if(text.length > 20) "..." else ""}'")
        }
    }

    /**
     * 🆕 현재 TTS가 재생 중인지 확인
     */
    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    /**
     * 🆕 TTS 중지 (모든 대기 중인 콜백도 실행)
     */
    fun stop() {
        tts?.stop()

        // 중지 시 모든 대기 중인 콜백 실행
        completionCallbacks.values.forEach { it.invoke() }
        completionCallbacks.clear()

        Log.d("TTS", "🛑 TTS 중지 및 콜백 정리 완료")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        selectedVoice = null
        completionCallbacks.clear() // 콜백 맵 정리
        utteranceId = 0 // ID 초기화
        Log.d("TTS", "TTS 종료")
    }
}