// data/model/RecordViewModel.kt
package com.example.domentiacare.data.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.local.RecordStorage
import com.example.domentiacare.data.local.SimpleLocalStorage
import com.example.domentiacare.data.local.SimpleSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val recordStorage = RecordStorage(application)
    private val simpleLocalStorage = SimpleLocalStorage(application)

    private val _records = MutableStateFlow<List<Record>>(emptyList())
    val records: StateFlow<List<Record>> = _records

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    init {
        loadAllData()
    }

    // 모든 데이터 로드
    fun loadAllData() {
        viewModelScope.launch {
            Log.d("RecordViewModel", "=== 데이터 로드 시작 ===")
            val records = recordStorage.getAllRecords()
            _records.value = records
            Log.d("RecordViewModel", "✅ Record 총 ${records.size}개 로드 완료")

            records.forEach { record ->
                Log.d("RecordViewModel", "📋 Record: ${record.name}")
                Log.d("RecordViewModel", "  - ID: ${record.localId}")
                Log.d("RecordViewModel", "  - Transcript 상태: ${record.transcriptStatus}")
                Log.d("RecordViewModel", "  - Analysis 상태: ${record.analysisStatus}")
                Log.d("RecordViewModel", "  - Parse 상태: ${record.parseStatus}")
                Log.d("RecordViewModel", "  - 추출된 일정 개수: ${record.extractedSchedules?.size ?: 0}")

                record.extractedSchedules?.forEachIndexed { index, schedule ->
                    Log.d("RecordViewModel", "    📅 일정 ${index + 1}: ${schedule.title}")
                    Log.d("RecordViewModel", "      - 시작: ${schedule.startDate}")
                    Log.d("RecordViewModel", "      - 설명: ${schedule.description}")
                }
            }
            Log.d("RecordViewModel", "=== 데이터 로드 완료 ===")
        }
    }

    // 통화 녹음 파일들을 스캔하여 새로운 Record 생성
    fun scanAndImportRecordings(userId: Long) {
        viewModelScope.launch {
            Log.d("RecordViewModel", "=== 녹음 파일 스캔 시작 ===")
            Log.d("RecordViewModel", "사용자 ID: $userId")

            val dir = File("/sdcard/Recordings/Call/")
            Log.d("RecordViewModel", "스캔 경로: ${dir.absolutePath}")

            if (dir.exists() && dir.isDirectory) {
                Log.d("RecordViewModel", "✅ 디렉토리 존재 확인")

                val audioFiles = dir.listFiles { file ->
                    file.isFile && (file.extension.equals("m4a", true) ||
                            file.extension.equals("wav", true) ||
                            file.extension.equals("mp3", true))
                }?.map { file ->
                    RecordingFile(
                        name = file.name,
                        path = file.absolutePath,
                        lastModified = file.lastModified(),
                        size = file.length()
                    )
                } ?: emptyList()

                Log.d("RecordViewModel", "📁 발견된 오디오 파일: ${audioFiles.size}개")
                audioFiles.forEach { file ->
                    Log.d("RecordViewModel", "  - ${file.name} (${file.size} bytes)")
                }

                // 기존 Record들과 비교하여 새로운 파일만 추가
                val existingRecords = recordStorage.getAllRecords()
                val existingPaths = existingRecords.map { it.path }.toSet()
                Log.d("RecordViewModel", "📋 기존 Record: ${existingRecords.size}개")

                val newRecordings = audioFiles.filter { it.path !in existingPaths }
                Log.d("RecordViewModel", "🆕 새로운 파일: ${newRecordings.size}개")

                newRecordings.forEach { recording ->
                    Log.d("RecordViewModel", "📥 새 Record 생성: ${recording.name}")
                    val record = recording.toRecord(userId)
                    val result = recordStorage.saveRecord(record)
                    if (result.isSuccess) {
                        Log.d("RecordViewModel", "✅ Record 저장 성공: ${record.localId}")
                    } else {
                        Log.e("RecordViewModel", "❌ Record 저장 실패: ${result.exceptionOrNull()?.message}")
                    }
                }

                loadAllData()
            } else {
                Log.w("RecordViewModel", "⚠️ 디렉토리가 존재하지 않음: ${dir.absolutePath}")
            }
            Log.d("RecordViewModel", "=== 녹음 파일 스캔 완료 ===")
        }
    }

    // Whisper 처리 시작
    fun startWhisperProcessing(recordId: String) {
        viewModelScope.launch {
            recordStorage.updateProcessStatus(
                recordId,
                transcriptStatus = ProcessStatus.PROCESSING
            )
            processWithWhisper(recordId)
            loadAllData()
        }
    }

    // Llama 분석 시작
    fun startLlamaAnalysis(recordId: String) {
        viewModelScope.launch {
            recordStorage.updateProcessStatus(
                recordId,
                analysisStatus = ProcessStatus.PROCESSING
            )
            processWithLlama(recordId)
            loadAllData()
        }
    }

    // 일정 파싱 시작
    fun startScheduleParsing(recordId: String) {
        viewModelScope.launch {
            recordStorage.updateProcessStatus(
                recordId,
                parseStatus = ProcessStatus.PROCESSING
            )
            parseScheduleFromResult(recordId)
            loadAllData()
        }
    }

    // 전체 파이프라인 실행 (Whisper -> Llama -> Parse)
    fun processFullPipeline(recordId: String) {
        viewModelScope.launch {
            Log.d("RecordViewModel", "=== 전체 파이프라인 시작 ===")
            Log.d("RecordViewModel", "처리할 Record ID: $recordId")

            _isProcessing.value = true

            try {
                // 1. Whisper 처리
                Log.d("RecordViewModel", "🎙️ 1단계: Whisper 처리 시작")
                recordStorage.updateProcessStatus(recordId, transcriptStatus = ProcessStatus.PROCESSING)
                processWithWhisper(recordId)
                Log.d("RecordViewModel", "✅ 1단계: Whisper 처리 완료")

                // 2. Llama 분석
                Log.d("RecordViewModel", "🧠 2단계: Llama 분석 시작")
                recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.PROCESSING)
                processWithLlama(recordId)
                Log.d("RecordViewModel", "✅ 2단계: Llama 분석 완료")

                // 3. 일정 파싱
                Log.d("RecordViewModel", "📅 3단계: 일정 파싱 시작")
                recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.PROCESSING)
                parseScheduleFromResult(recordId)
                Log.d("RecordViewModel", "✅ 3단계: 일정 파싱 완료")

                // 최종 결과 확인
                val finalRecord = recordStorage.getRecordById(recordId)
                Log.d("RecordViewModel", "🎉 전체 파이프라인 성공!")
                Log.d("RecordViewModel", "  - 최종 상태: T:${finalRecord?.transcriptStatus}, A:${finalRecord?.analysisStatus}, P:${finalRecord?.parseStatus}")
                Log.d("RecordViewModel", "  - 추출된 일정 수: ${finalRecord?.extractedSchedules?.size ?: 0}")

            } catch (e: Exception) {
                Log.e("RecordViewModel", "❌ 파이프라인 실행 중 오류: ${e.message}", e)
                // 에러 처리
                recordStorage.updateProcessStatus(
                    recordId,
                    transcriptStatus = ProcessStatus.FAILED,
                    analysisStatus = ProcessStatus.FAILED,
                    parseStatus = ProcessStatus.FAILED
                )
            } finally {
                _isProcessing.value = false
                loadAllData()
                Log.d("RecordViewModel", "=== 전체 파이프라인 종료 ===")
            }
        }
    }

    // 실제 Whisper 처리 (구현 필요)
    private suspend fun processWithWhisper(recordId: String) {
        Log.d("RecordViewModel", "🎙️ Whisper 처리 시작: $recordId")
        val record = recordStorage.getRecordById(recordId) ?: run {
            Log.e("RecordViewModel", "❌ Record를 찾을 수 없음: $recordId")
            return
        }

        Log.d("RecordViewModel", "📁 처리할 파일: ${record.path}")
        Log.d("RecordViewModel", "📏 파일 크기: ${record.size} bytes")

        try {
            // TODO: 실제 Whisper 온-디바이스 처리 로직
            val transcript = "이것은 Whisper로 변환된 텍스트입니다. 실제 구현에서는 ${record.path} 파일을 처리합니다. 내일 오후 2시에 병원에 가야 해요. 정기 검진 받으러 갑니다."

            Log.d("RecordViewModel", "📝 Whisper 변환 결과:")
            Log.d("RecordViewModel", "  길이: ${transcript.length}자")
            Log.d("RecordViewModel", "  내용: ${transcript.take(100)}${if (transcript.length > 100) "..." else ""}")

            recordStorage.updateTranscript(recordId, transcript, ProcessStatus.COMPLETED)
            Log.d("RecordViewModel", "✅ Whisper 처리 완료 및 저장 성공")

        } catch (e: Exception) {
            Log.e("RecordViewModel", "❌ Whisper 처리 실패: ${e.message}", e)
            recordStorage.updateProcessStatus(recordId, transcriptStatus = ProcessStatus.FAILED)
        }
    }

    // 실제 Llama 처리 (구현 필요)
    private suspend fun processWithLlama(recordId: String) {
        Log.d("RecordViewModel", "🧠 Llama 분석 시작: $recordId")
        val record = recordStorage.getRecordById(recordId) ?: run {
            Log.e("RecordViewModel", "❌ Record를 찾을 수 없음: $recordId")
            return
        }

        if (record.transcript.isNullOrEmpty()) {
            Log.e("RecordViewModel", "❌ Transcript가 비어있음. Llama 분석 불가")
            recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.FAILED)
            return
        }

        Log.d("RecordViewModel", "📝 입력 Transcript:")
        Log.d("RecordViewModel", "  ${record.transcript}")

        try {
            // TODO: 실제 Llama 온-디바이스 처리 로직
            val analysisResult = """
            [
                {
                    "title": "병원 진료",
                    "description": "정기 검진 예약",
                    "startDate": "2025-05-28T14:00:00.000Z",
                    "endDate": "2025-05-28T15:00:00.000Z"
                },
                {
                    "title": "약국 방문",
                    "description": "처방전 조제",
                    "startDate": "2025-05-28T15:30:00.000Z",
                    "endDate": "2025-05-28T16:00:00.000Z"
                }
            ]
            """.trimIndent()

            Log.d("RecordViewModel", "🧠 Llama 분석 결과:")
            Log.d("RecordViewModel", "  결과 길이: ${analysisResult.length}자")
            Log.d("RecordViewModel", "  분석 내용:")
            Log.d("RecordViewModel", analysisResult)

            recordStorage.updateAnalysisResult(recordId, analysisResult, ProcessStatus.COMPLETED)
            Log.d("RecordViewModel", "✅ Llama 분석 완료 및 저장 성공")

        } catch (e: Exception) {
            Log.e("RecordViewModel", "❌ Llama 분석 실패: ${e.message}", e)
            recordStorage.updateProcessStatus(recordId, analysisStatus = ProcessStatus.FAILED)
        }
    }

    // 일정 파싱 처리 (CallDetailScreen의 파싱 함수 활용)
    private suspend fun parseScheduleFromResult(recordId: String) {
        Log.d("RecordViewModel", "📅 일정 파싱 시작: $recordId")
        val record = recordStorage.getRecordById(recordId) ?: run {
            Log.e("RecordViewModel", "❌ Record를 찾을 수 없음: $recordId")
            return
        }

        if (record.result.isNullOrEmpty()) {
            Log.e("RecordViewModel", "❌ Llama 결과가 비어있음. 파싱 불가")
            recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.FAILED)
            return
        }

        Log.d("RecordViewModel", "🧠 파싱할 Llama 결과:")
        Log.d("RecordViewModel", record.result)

        try {
            // CallDetailScreen의 parseLlamaScheduleResponseFull 함수를 사용하여 파싱
            val extractedSchedules = parseLlamaScheduleResponseFull(
                record.result,
                recordId,
                record.userId
            )

            Log.d("RecordViewModel", "📅 파싱 결과:")
            Log.d("RecordViewModel", "  추출된 일정 수: ${extractedSchedules.size}")

            extractedSchedules.forEachIndexed { index, schedule ->
                Log.d("RecordViewModel", "  📅 일정 ${index + 1}:")
                Log.d("RecordViewModel", "    - ID: ${schedule.localId}")
                Log.d("RecordViewModel", "    - 제목: ${schedule.title}")
                Log.d("RecordViewModel", "    - 설명: ${schedule.description}")
                Log.d("RecordViewModel", "    - 시작: ${schedule.startDate}")
                Log.d("RecordViewModel", "    - 종료: ${schedule.endDate}")
                Log.d("RecordViewModel", "    - AI 생성: ${schedule.isAi}")
                Log.d("RecordViewModel", "    - 동기화 상태: ${schedule.syncStatus}")
            }

            recordStorage.updateExtractedSchedules(recordId, extractedSchedules, ProcessStatus.COMPLETED)
            Log.d("RecordViewModel", "✅ 일정 파싱 완료 및 저장 성공")

            // 저장 후 검증
            val updatedRecord = recordStorage.getRecordById(recordId)
            Log.d("RecordViewModel", "🔍 저장 검증:")
            Log.d("RecordViewModel", "  저장된 일정 수: ${updatedRecord?.extractedSchedules?.size ?: 0}")
            Log.d("RecordViewModel", "  파싱 상태: ${updatedRecord?.parseStatus}")

        } catch (e: Exception) {
            Log.e("RecordViewModel", "❌ 일정 파싱 실패: ${e.message}", e)
            recordStorage.updateProcessStatus(recordId, parseStatus = ProcessStatus.FAILED)
        }
    }

    // 특정 Record의 추출된 일정들 조회
    fun getExtractedSchedulesForRecord(recordId: String): List<SimpleSchedule> {
        return _records.value
            .find { it.localId == recordId }
            ?.extractedSchedules ?: emptyList()
    }

    // 추출된 일정들을 기존 SimpleSchedule 시스템에 저장
    fun exportExtractedSchedulesToSimpleSchedule(recordId: String, scheduleIndices: List<Int>? = null) {
        viewModelScope.launch {
            Log.d("RecordViewModel", "=== 일정 내보내기 시작 ===")
            Log.d("RecordViewModel", "Record ID: $recordId")

            val record = recordStorage.getRecordById(recordId) ?: run {
                Log.e("RecordViewModel", "❌ Record를 찾을 수 없음: $recordId")
                return@launch
            }

            val extractedSchedules = record.extractedSchedules ?: run {
                Log.w("RecordViewModel", "⚠️ 추출된 일정이 없음")
                return@launch
            }

            Log.d("RecordViewModel", "📅 총 추출된 일정: ${extractedSchedules.size}개")

            val schedulesToExport = if (scheduleIndices != null) {
                Log.d("RecordViewModel", "📋 선택된 인덱스: $scheduleIndices")
                scheduleIndices.mapNotNull { index ->
                    extractedSchedules.getOrNull(index)
                }
            } else {
                Log.d("RecordViewModel", "📋 모든 일정 내보내기")
                extractedSchedules // 모든 일정 내보내기
            }

            Log.d("RecordViewModel", "💾 내보낼 일정: ${schedulesToExport.size}개")

            var successCount = 0
            var failCount = 0

            schedulesToExport.forEach { schedule ->
                try {
                    Log.d("RecordViewModel", "📥 일정 저장 시도: ${schedule.title}")
                    val result = simpleLocalStorage.saveSchedule(schedule)

                    if (result.isSuccess) {
                        successCount++
                        Log.d("RecordViewModel", "✅ 일정 저장 성공: ${schedule.title}")
                    } else {
                        failCount++
                        Log.e("RecordViewModel", "❌ 일정 저장 실패: ${schedule.title}, 오류: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    failCount++
                    Log.e("RecordViewModel", "❌ 일정 저장 예외: ${schedule.title}", e)
                }
            }

            Log.d("RecordViewModel", "📊 내보내기 결과:")
            Log.d("RecordViewModel", "  ✅ 성공: ${successCount}개")
            Log.d("RecordViewModel", "  ❌ 실패: ${failCount}개")
            Log.d("RecordViewModel", "=== 일정 내보내기 완료 ===")
        }
    }

    // 모든 Record에서 추출된 일정들 조회
    fun getAllExtractedSchedules(): List<Pair<String, SimpleSchedule>> {
        return _records.value
            .mapNotNull { record ->
                record.extractedSchedules?.map { schedule ->
                    record.localId to schedule
                }
            }
            .flatten()
    }

    // 처리 대기중인 Record들 조회
    fun getPendingRecords(): List<Record> {
        return _records.value.filter {
            it.transcriptStatus == ProcessStatus.PENDING ||
                    it.analysisStatus == ProcessStatus.PENDING ||
                    it.parseStatus == ProcessStatus.PENDING
        }
    }

    // 🔧 테스트용 함수들

    // 전체 데이터 상태 로그 출력
    fun logAllData() {
        Log.d("RecordViewModel", "=== 전체 데이터 상태 로그 ===")
        val records = _records.value
        Log.d("RecordViewModel", "📊 전체 통계:")
        Log.d("RecordViewModel", "  - 총 Record 수: ${records.size}")
        Log.d("RecordViewModel", "  - 처리 대기중: ${getPendingRecords().size}")
        Log.d("RecordViewModel", "  - 처리 중: ${records.count { it.transcriptStatus == ProcessStatus.PROCESSING || it.analysisStatus == ProcessStatus.PROCESSING || it.parseStatus == ProcessStatus.PROCESSING }}")
        Log.d("RecordViewModel", "  - 완료: ${records.count { it.transcriptStatus == ProcessStatus.COMPLETED && it.analysisStatus == ProcessStatus.COMPLETED && it.parseStatus == ProcessStatus.COMPLETED }}")
        Log.d("RecordViewModel", "  - 실패: ${records.count { it.transcriptStatus == ProcessStatus.FAILED || it.analysisStatus == ProcessStatus.FAILED || it.parseStatus == ProcessStatus.FAILED }}")

        val totalSchedules = records.sumOf { it.extractedSchedules?.size ?: 0 }
        Log.d("RecordViewModel", "  - 총 추출된 일정: ${totalSchedules}개")
        Log.d("RecordViewModel", "================================")
    }

    // 특정 Record 상세 로그
    fun logRecordDetails(recordId: String) {
        Log.d("RecordViewModel", "=== Record 상세 정보 ===")
        val record = _records.value.find { it.localId == recordId }
        if (record != null) {
            Log.d("RecordViewModel", "📋 Record: ${record.name}")
            Log.d("RecordViewModel", "  - ID: ${record.localId}")
            Log.d("RecordViewModel", "  - 사용자: ${record.userId}")
            Log.d("RecordViewModel", "  - 파일: ${record.path}")
            Log.d("RecordViewModel", "  - 크기: ${record.size} bytes")
            Log.d("RecordViewModel", "  - 상태: T:${record.transcriptStatus}, A:${record.analysisStatus}, P:${record.parseStatus}")
            Log.d("RecordViewModel", "  - Transcript: ${record.transcript?.length ?: 0}자")
            Log.d("RecordViewModel", "  - Analysis: ${record.result?.length ?: 0}자")
            Log.d("RecordViewModel", "  - 일정 수: ${record.extractedSchedules?.size ?: 0}")

            record.extractedSchedules?.forEachIndexed { index, schedule ->
                Log.d("RecordViewModel", "    📅 일정 ${index + 1}: ${schedule.title} (${schedule.startDate})")
            }
        } else {
            Log.w("RecordViewModel", "⚠️ Record를 찾을 수 없음: $recordId")
        }
        Log.d("RecordViewModel", "========================")
    }

    // JSON 직렬화 테스트
    fun testJsonSerialization() {
        viewModelScope.launch {
            Log.d("RecordViewModel", "=== JSON 직렬화 테스트 ===")
            val records = recordStorage.getAllRecords()

            records.forEach { record ->
                Log.d("RecordViewModel", "🧪 Record: ${record.name}")
                Log.d("RecordViewModel", "  - 일정 수: ${record.extractedSchedules?.size ?: 0}")

                // JSON으로 변환 후 다시 파싱 테스트
                try {
                    val gson = com.google.gson.Gson()
                    val json = gson.toJson(record)
                    val parsedRecord = gson.fromJson(json, Record::class.java)

                    Log.d("RecordViewModel", "  ✅ JSON 직렬화/역직렬화 성공")
                    Log.d("RecordViewModel", "  - 원본 일정 수: ${record.extractedSchedules?.size ?: 0}")
                    Log.d("RecordViewModel", "  - 복원 일정 수: ${parsedRecord.extractedSchedules?.size ?: 0}")

                } catch (e: Exception) {
                    Log.e("RecordViewModel", "  ❌ JSON 처리 실패: ${e.message}")
                }
            }
            Log.d("RecordViewModel", "========================")
        }
    }
}