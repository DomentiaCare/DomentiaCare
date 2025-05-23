package com.example.domentiacare.data.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.repository.CallRepository
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

// Composable 함수는 상태를 표시하고 이벤트를 전달하는 역할만 담당
class CallDetailViewModel(
    private val repository: CallRepository,
    private val dispatchers: CoroutineDispatchers = DefaultCoroutineDispatchers()
) : ViewModel() {
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

                // 2. Whisper 모델 실행
                val whisper = WhisperWrapper(context)
                whisper.copyModelFiles()
                whisper.initModel()

                whisper.transcribe(
                    wavPath = wavFile.absolutePath,
                    onResult = { result ->
                        _uiState.update {
                            it.copy(
                                transcript = result,
                                isLoading = false
                            )
                        }
                    },
                    onUpdate = { logLine ->
                        // 중간 로그 처리
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                // 에러 처리
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