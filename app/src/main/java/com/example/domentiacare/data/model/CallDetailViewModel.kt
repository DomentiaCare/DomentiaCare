package com.example.domentiacare.data.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.repository.CallRepository
import com.example.domentiacare.data.repository.CallRepositoryImpl
import com.example.domentiacare.data.util.convertM4aToWavForWhisper
import com.example.domentiacare.service.whisper.WhisperWrapper
import com.example.domentiacare.ui.screen.call.RecordLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Composable 함수는 상태를 표시하고 이벤트를 전달하는 역할만 담당
class CallDetailViewModel : ViewModel() {
    // 내부에서 직접 초기화
    private val repository: CallRepository = object : CallRepository {
        override fun isNetworkAvailable(): Boolean {
            return false // 기본적으로 네트워크 연결 없음으로 설정
        }

        override suspend fun analyzeLlama(transcript: String): String {
            // 실제 Llama 분석 대신 간단한 텍스트 반환
            return "일정: ${transcript.take(30)}... 에 대한 분석 결과입니다."
        }

        override suspend fun saveWavFileLocally(wavFile: File) {
            // 로그만 출력하고 실제 저장은 하지 않음
            android.util.Log.d("CallDetailViewModel", "WAV 파일 저장: ${wavFile.absolutePath}")
        }

        override suspend fun saveLlamaDataLocally(prompt: String, response: String) {
            // 로그만 출력하고 실제 저장은 하지 않음
            android.util.Log.d("CallDetailViewModel", "Llama 데이터 저장: $prompt, $response")
        }

        override suspend fun saveCallMetadataLocally(
            fileName: String,
            transcript: String,
            memo: String,
            dateTime: LocalDateTime
        ) {
            // 로그만 출력하고 실제 저장은 하지 않음
            android.util.Log.d("CallDetailViewModel", "통화 메타데이터 저장: $fileName, $memo, $dateTime")
        }

        override suspend fun sendDataToServer(
            wavFile: File,
            transcript: String,
            memo: String,
            dateTime: LocalDateTime
        ) {
            // 로그만 출력하고 실제 서버 전송은 하지 않음
            android.util.Log.d("CallDetailViewModel", "서버로 데이터 전송: ${wavFile.absolutePath}, $memo")
        }

        override suspend fun clearLocalTempFiles() {
            // 로그만 출력하고 실제 파일 삭제는 하지 않음
            android.util.Log.d("CallDetailViewModel", "임시 파일 삭제")
        }
    }
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()

    data class CallDetailUiState(
        val recordingFile: RecordingFile? = null,
        val recordLog: RecordLog? = null,
        val transcript: String = "",
        val memo: String = "",
        val selectedDateTime: LocalDateTime = LocalDateTime.now(),
        val isLoading: Boolean = false,
        val isDataConnected: Boolean = false,
        val saveStatus: SaveStatus = SaveStatus.NONE
    )

    enum class SaveStatus { NONE, SAVING, SUCCESS, ERROR }

    // 상태 관리
    private val _uiState = MutableStateFlow(CallDetailUiState())
    val uiState: StateFlow<CallDetailUiState> = _uiState.asStateFlow()

    // 파일 경로로 초기화
    fun loadRecordingFile(filePath: String) {
        viewModelScope.launch(dispatchers.io) {
            try {
                val file = File(filePath)
                val recordingFile = RecordingFile(
                    name = file.name,
                    path = filePath,
                    lastModified = file.lastModified(),
                    size = file.length()
                )

                val recordLog = RecordLog(
                    name = file.name.substringBeforeLast('.'),
                    type = "발신", // 실제 타입 추출 로직으로 대체
                    time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(file.lastModified()))
                )

                _uiState.update {
                    it.copy(
                        recordingFile = recordingFile,
                        recordLog = recordLog,
                        isDataConnected = repository.isNetworkAvailable()
                    )
                }
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }

    // Whisper 모델로 텍스트 변환
    fun transcribeAudio(context: Context) {
        val currentFile = _uiState.value.recordingFile ?: return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(dispatchers.io) {
            try {
                // 1. wav 변환
                val wavFile = convertToWav(currentFile.path)
                Log.d("CallDetailViewModel", "WAV 파일 생성: ${wavFile.absolutePath}")
                Log.d("CallDetailViewModel", "WAV 파일 크기: ${wavFile.length()} bytes")

                // 2. Whisper 모델 실행
                val whisper = WhisperWrapper(context)
                whisper.copyModelFiles()
                whisper.initModel()

                // 3. 콜백을 사용하지 말고 직접 호출
                launch(Dispatchers.Main) {
                    whisper.transcribe(
                        wavPath = wavFile.absolutePath,
                        onResult = { result ->
                            Log.d("CallDetailViewModel", "Whisper 결과: '$result'")
                            _uiState.update {
                                it.copy(
                                    transcript = if (result.isBlank()) "음성을 인식할 수 없습니다." else result,
                                    isLoading = false
                                )
                            }
                        },
                        onUpdate = { logLine ->
                            Log.d("CallDetailViewModel", "Whisper 진행: $logLine")
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("CallDetailViewModel", "Transcription 오류", e)
                _uiState.update {
                    it.copy(
                        transcript = "오류: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    // Llama 모델로 일정 분석
    fun analyzeSchedule() {
        val transcript = _uiState.value.transcript
        if (transcript.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(dispatchers.io) {
            try {
                // Llama 모델 호출 예시
                val llamaResult = repository.analyzeLlama(transcript)
                _uiState.update {
                    it.copy(
                        memo = llamaResult,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                // 에러 처리
            }
        }
    }

    // 메모 업데이트
    fun updateMemo(memo: String) {
        _uiState.update { it.copy(memo = memo) }
    }

    // 날짜 시간 업데이트
    fun updateDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val newDateTime = LocalDateTime.of(year, month, day, hour, minute)
        _uiState.update { it.copy(selectedDateTime = newDateTime) }
    }

    // 저장 처리
    fun saveData() {
        val currentState = _uiState.value
        val recordingFile = currentState.recordingFile ?: return

        _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }

        viewModelScope.launch(dispatchers.io) {
            try {
                // 1. 항상 내부 저장
                val wavFile = File(convertToWav(recordingFile.path).absolutePath)
                val llamaPrompt = "일정 분석: ${currentState.transcript}"
                val llamaResponse = currentState.memo

                // 내부 저장 함수 호출
                repository.saveWavFileLocally(wavFile)
                repository.saveLlamaDataLocally(llamaPrompt, llamaResponse)
                repository.saveCallMetadataLocally(
                    recordingFile.name,
                    currentState.transcript,
                    currentState.memo,
                    currentState.selectedDateTime
                )

                // 2. 데이터 연결 확인 후 서버 전송
                if (currentState.isDataConnected) {
                    // 서버 전송
                    repository.sendDataToServer(
                        wavFile,
                        currentState.transcript,
                        currentState.memo,
                        currentState.selectedDateTime
                    )

                    // 성공적으로 전송 후 임시 저장 파일 삭제
                    repository.clearLocalTempFiles()
                }

                _uiState.update { it.copy(saveStatus = SaveStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(saveStatus = SaveStatus.ERROR) }
                // 에러 처리
            }
        }
    }

    // wav 변환 유틸리티 함수
    private fun convertToWav(m4aPath: String): File {
        val m4aFile = File(m4aPath)
        val outputDir = File("/sdcard/Recordings/wav/")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputWavFile = File(outputDir, m4aFile.nameWithoutExtension + ".wav")
        convertM4aToWavForWhisper(m4aFile, outputWavFile)
        return outputWavFile
    }
}

interface CoroutineDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

// 2. 기본 구현체
class DefaultCoroutineDispatchers : CoroutineDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}

// CallDetailViewModel 내부에 임시 구현체 추가
private val repository: CallRepository = object : CallRepository {
    override fun isNetworkAvailable(): Boolean {
        return false // 기본적으로 네트워크 연결 없음으로 설정
    }

    override suspend fun analyzeLlama(transcript: String): String {
        // 실제 Llama 분석 대신 간단한 텍스트 반환
        return "일정: ${transcript.take(30)}... 에 대한 분석 결과입니다."
    }

    override suspend fun saveWavFileLocally(wavFile: File) {
        // 로그만 출력하고 실제 저장은 하지 않음
        android.util.Log.d("CallDetailViewModel", "WAV 파일 저장: ${wavFile.absolutePath}")
    }

    override suspend fun saveLlamaDataLocally(prompt: String, response: String) {
        // 로그만 출력하고 실제 저장은 하지 않음
        android.util.Log.d("CallDetailViewModel", "Llama 데이터 저장: $prompt, $response")
    }

    override suspend fun saveCallMetadataLocally(
        fileName: String,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    ) {
        // 로그만 출력하고 실제 저장은 하지 않음
        android.util.Log.d("CallDetailViewModel", "통화 메타데이터 저장: $fileName, $memo, $dateTime")
    }

    override suspend fun sendDataToServer(
        wavFile: File,
        transcript: String,
        memo: String,
        dateTime: LocalDateTime
    ) {
        // 로그만 출력하고 실제 서버 전송은 하지 않음
        android.util.Log.d("CallDetailViewModel", "서버로 데이터 전송: ${wavFile.absolutePath}, $memo")
    }

    override suspend fun clearLocalTempFiles() {
        // 로그만 출력하고 실제 파일 삭제는 하지 않음
        android.util.Log.d("CallDetailViewModel", "임시 파일 삭제")
    }
}