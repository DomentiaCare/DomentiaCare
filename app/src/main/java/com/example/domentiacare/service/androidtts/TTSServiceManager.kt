package com.example.domentiacare.service.androidtts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

object TTSServiceManager {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var selectedVoice: Voice? = null

    fun init(context: Context, onInit: (() -> Unit)? = null) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
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

    fun speak(text: String) {
        if (!isReady) {
            Log.e("TTS", "TTS 아직 초기화 안됨")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        selectedVoice = null
        Log.d("TTS", "TTS 종료")
    }
}
