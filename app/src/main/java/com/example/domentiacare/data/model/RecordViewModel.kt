//// data/model/RecordViewModel.kt
//package com.example.domentiacare.data.model
//
//import android.app.Application
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.domentiacare.data.local.RecordStorage
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import java.io.File
//
//class RecordViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val recordStorage = RecordStorage(application)
//
//    private val _records = MutableStateFlow<List<Record>>(emptyList())
//    val records: StateFlow<List<Record>> = _records
//
//    private val _parsedSchedules = MutableStateFlow<List<ParsedScheduleData>>(emptyList())
//    val parsedSchedules: StateFlow<List<ParsedScheduleData>> = _parsedSchedules
//
//    private val _isProcessing = MutableStateFlow(false)
//    val isProcessing: StateFlow<Boolean> = _isProcessing
//
//    init {
//        loadAllData()
//    }
//
//    // 모든 데이터 로드
//    fun loadAllData() {
//        viewModelScope.launch {
//            _records.value = recordStorage.getAllRecords()
//            _parsedSchedules.value = recordStorage.getAllParsedSchedules()
//        }
//    }
//
//    // 통화 녹음 파일들을 스캔하여 새로운 Record 생성
//    fun scanAndImportRecordings(userId: Long) {
//        viewModelScope.launch {
//            val dir = File("/sdcard/Recordings/Call/")
//            if (dir.exists() && dir.isDirectory) {
//                val audioFiles = dir.listFiles { file ->
//                    file.isFile && (file.extension.equals("m4a", true) ||
//                            file.extension.equals("wav", true) ||
//                            file.extension.equals("mp3", true))
//                }?.map { file ->
//                    RecordingFile(
//                        name = file.name,
//                        path = file.absolutePath,
//                        lastModified = file.lastModified(),
//                        size = file.length()
//                    )
//                } ?: emptyList()
//
//                // 기존 Record들과 비교하여 새로운 파일만 추가
//                val existingRecords = recordStorage.getAllRecords()
//                val existingPaths = existingRecords.map { it.path }.toSet()
//
//                val newRecordings = audioFiles.filter { it.path !in existingPaths }
//
//                newRecordings.forEach { recording ->
//                    val record = recording.toRecord(userId)
//                    recordStorage.saveRecord(record)
//                }
//
//                loadAllData()
//            }
//        }
//    }
//
//    // Whisper 처리 시작
//    fun startWhisperProcessing(recordId: String) {
//        viewModelScope.launch {
//            recordStorage.updateProcessStatus(
//                recordId,
//                transcriptStatus = ProcessStatus.PROCESSING
//            )
//
//            // 실제 Whisper 처리 로직 호출
//            processWithWhisper(recordId)
//
//            loadAllData()
//        }
//    }
//
//    // Llama 분석 시작
//    fun startLlamaAnalysis(recordId: String) {
//        viewModelScope.launch {
//            recordStorage.updateProcessStatus(
//                recordId,
//                analysisStatus = ProcessStatus.PROCESSING
//            )
//
//            // 실제 Llama 분석 로직 호출
//            processWithLlama(recordId)
//
//            loadAllData()
//        }
//    }
//
//    // 일정 파싱 시작
//    fun startScheduleParsing(recordId: String) {
//        viewModelScope.launch {
//            recordStorage.updateProcessStatus(
//                recordId,
//                parseStatus = ProcessStatus.PROCESSING
//            )
//
//            // 실제 파싱 로직 호출
//            parseScheduleFromResult(recordId)
//
//            loadAllData()
//        }
//    }
//
//    // 전체 파이프라인 실행 (Whisper -> Llama -> Parse)
//    fun processFullPipeline(recordId: String) {
//        viewModelScope.launch {
//            _isProcessing.value = true
//
//            try {
//                // 1. Whisper 처리
//                recordStorage.updateProcessStatus(recordId, transcriptStatus = ProcessStatus.PROCESSING)
//                processWithWhisper(recordId)
//
//                // 2. Llama 분석
//                recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.PROCESSING)
//                processWithLlama(recordId)
//
//                // 3. 일정 파싱
//                recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.PROCESSING)
//                parseScheduleFromResult(recordId)
//
//            } catch (e: Exception) {
//                // 에러 처리
//                recordStorage.updateProcessStatus(
//                    recordId,
//                    transcriptStatus = ProcessStatus.FAILED,
//                    analysisStatus = ProcessStatus.FAILED,
//                    parseStatus = ProcessStatus.FAILED
//                )
//            } finally {
//                _isProcessing.value = false
//                loadAllData()
//            }
//        }
//    }
//
//    // 실제 Whisper 처리 (구현 필요)
//    private suspend fun processWithWhisper(recordId: String) {
//        // TODO: 실제 Whisper 온-디바이스 처리 로직
//        val record = recordStorage.getRecordById(recordId) ?: return
//
//        try {
//            // 예시: 실제로는 Whisper 모델을 통해 음성을 텍스트로 변환
//            val transcript = "이것은 Whisper로 변환된 텍스트입니다. 실제 구현에서는 ${record.path} 파일을 처리합니다."
//
//            recordStorage.updateTranscript(recordId, transcript, ProcessStatus.COMPLETED)
//        } catch (e: Exception) {
//            recordStorage.updateProcessStatus(recordId, transcriptStatus = ProcessStatus.FAILED)
//        }
//    }
//
//    // 실제 Llama 처리 (구현 필요)
//    private suspend fun processWithLlama(recordId: String) {
//        // TODO: 실제 Llama 온-디바이스 처리 로직
//        val record = recordStorage.getRecordById(recordId) ?: return
//
//        if (record.transcript.isNullOrEmpty()) {
//            recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.FAILED)
//            return
//        }
//
//        try {
//            // 예시: 실제로는 Llama 모델을 통해 일정 정보 추출
//            val analysisResult = """
//            [
//                {
//                    "title": "병원 진료",
//                    "description": "정기 검진 예약",
//                    "startDate": "2025-05-28 14:00",
//                    "endDate": "2025-05-28 15:00"
//                },
//                {
//                    "title": "약국 방문",
//                    "description": "처방전 조제",
//                    "startDate": "2025-05-28 15:30",
//                    "endDate": "2025-05-28 16:00"
//                }
//            ]
//            """.trimIndent()
//
//            recordStorage.updateAnalysisResult(recordId, analysisResult, ProcessStatus.COMPLETED)
//        } catch (e: Exception) {
//            recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.FAILED)
//        }
//    }
//
//    // 일정 파싱 처리
//    private suspend fun parseScheduleFromResult(recordId: String) {
//        val record = recordStorage.getRecordById(recordId) ?: return
//
//        if (record.result.isNullOrEmpty()) {
//            recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.FAILED)
//            return
//        }
//
//        try {
//            val parsedSchedules = parseLlamaScheduleResponseFull(
//                record.result,
//                recordId,
//                record.userId
//            )
//
//            recordStorage.updateParsedSchedules(recordId, parsedSchedules, ProcessStatus.COMPLETED)
//        } catch (e: Exception) {
//            recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.FAILED)
//        }
//    }
//
//    // 특정 Record의 파싱된 일정들 조회
//    fun getParsedSchedulesForRecord(recordId: String): List<ParsedScheduleData> {
//        return _parsedSchedules.value.filter { it.recordLocalId == recordId }
//    }
//
//    // 파싱된 일정을 SimpleSchedule로 변환하여 기존 시스템에 저장
//    fun exportParsedSchedulesToSimpleSchedule(scheduleLocalIds: List<String>) {
//        viewModelScope.launch {
//            val simpleLocalStorage = com.example.domentiacare.data.local.SimpleLocalStorage(getApplication())
//
//            _parsedSchedules.value
//                .filter { it.localId in scheduleLocalIds }
//                .forEach { parsedSchedule ->
//                    val simpleSchedule = parsedSchedule.toSimpleSchedule()
//                    simpleLocalStorage.saveSchedule(simpleSchedule)
//                }
//        }
//    }
//
//    // 처리 대기중인 Record들 조회
//    fun getPendingRecords(): List<Record> {
//        return _records.value.filter {
//            it.transcriptStatus == ProcessStatus.PENDING ||
//                    it.analysisStatus == ProcessStatus.PENDING ||
//                    it.parseStatus == ProcessStatus.PENDING
//        }
//    }
//}