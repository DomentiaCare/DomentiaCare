package com.example.domentiacare.service.whisper

import android.content.Context
import android.util.Log
import java.io.File
class WhisperWrapper(private val context: Context) {

    private val whisper = Whisper(context)

    fun copyModelFiles() {
        listOf("whisper-tiny.en.tflite", "filters_vocab_en.bin").forEach { file ->
            val dest = File(context.filesDir, file)
            if (!dest.exists()) {
                context.assets.open(file).use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    fun initModel() {
        val modelPath = File(context.filesDir, "whisper-tiny.en.tflite")
        val vocabPath = File(context.filesDir, "filters_vocab_en.bin")
        whisper.loadModel(modelPath.absolutePath, vocabPath.absolutePath, false)
    }

    fun transcribe(wavPath: String, onResult: (String) -> Unit, onUpdate: (String) -> Unit) {
        whisper.setFilePath(wavPath)
        whisper.setAction(Whisper.ACTION_TRANSCRIBE)
        whisper.setListener(object : Whisper.WhisperListener {
            override fun onUpdateReceived(message: String) {
                onUpdate(message)
            }

            override fun onResultReceived(result: String) {
                Log.d("Whisper", "Transcribed Result: $result")
                onResult(result) // ← 여기서 Compose 상태 업데이트해야 함
            }
        })
        whisper.start()
    }

    fun stop() = whisper.stop()

    fun isRunning(): Boolean = whisper.isInProgress

    fun copyWaveFile() {
        val dest = File(context.filesDir, "english_test_3_bili.wav")
        if (!dest.exists()) {
            context.assets.open("english_test_3_bili.wav").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

}
