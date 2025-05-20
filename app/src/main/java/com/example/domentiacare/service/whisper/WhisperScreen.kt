package com.example.domentiacare.service.whisper

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun WhisperScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val whisper = remember { WhisperWrapper(context) }

    var result by remember { mutableStateOf("") }
    val resultText = remember { mutableStateOf("결과 대기 중...") }

    var isTranscribing by remember { mutableStateOf(false) }
    var loopTesting by remember { mutableStateOf(false) } // Optional switch

    // 모델 초기화 (한 번만)
    LaunchedEffect(Unit) {
        whisper.copyModelFiles() // assets → filesDir 복사
        whisper.copyWaveFile() // assets → filesDir 복사
        whisper.initModel()      // WhisperWrapper에서 loadModel 수행

    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Button(onClick = {
            if (!isTranscribing) {
                result = "STT 시작..."

                val wavePath = File(context.filesDir, "english_test_3_bili.wav").absolutePath
                isTranscribing = true

                coroutineScope.launch(Dispatchers.IO) {
                    whisper.transcribe(
                        wavPath = wavePath,
                        onResult = {
                            resultText.value = it // ← 반드시 있어야 함
                            isTranscribing = false
                        },
                        onUpdate = {
                            Log.d("Whisper", it)

                        }
                    )

                    if (loopTesting) {
                        repeat(1000) { i ->
                            delay(500)
                            if (!whisper.isRunning()) {
                                whisper.transcribe(wavPath = wavePath, onResult = {
                                    Log.d("Whisper", "Loop result: $it")
                                }, onUpdate = {})
                            } else {
                                Log.d("Whisper", "Whisper already in progress")
                            }

                            // 15초 대기 또는 결과 수신 대기용
                            delay(15_000)
                        }
                    }
                }

            } else {
                Log.d("Whisper", "이미 진행 중입니다!")
                whisper.stop()
                isTranscribing = false
            }

        }) {
            Text("STT 실행")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(resultText.value)
    }
}
