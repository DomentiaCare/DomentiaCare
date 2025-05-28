package com.example.domentiacare.data.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.network.RecordResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class RecordingFile(
    val name: String,
    val path: String,
    val lastModified: Long,
    val size: Long
)

// RecordResponse를 RecordingFile로 변환하는 확장 함수
fun RecordResponse.toRecordingFile(): RecordingFile {
    return RecordingFile(
        name = this.fileName,
        path = this.audioUrl?.takeIf { it.isNotBlank() } ?: "", // 서버의 오디오 URL
        lastModified = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(this.createdAt)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        },
        size = this.fileSize ?: 0L
    )
}

class CallRecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val _recordingFiles = MutableStateFlow<List<RecordingFile>>(emptyList())
    val recordingFiles: StateFlow<List<RecordingFile>> = _recordingFiles

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 캐시 관리
    private val recordingsCache = mutableMapOf<String, List<RecordingFile>>()
    private val cacheTimestamp = mutableMapOf<String, Long>()
    private val CACHE_DURATION = 5 * 60 * 1000L // 5분

    // 전체 녹음(로컬) 캐시를 위한 특별한 키
    private val ALL_RECORDINGS_KEY = "ALL_RECORDINGS"

    /**
     * 로컬 녹음 파일 로딩 (기존 기능 유지)
     */
    fun loadRecordings() {
        viewModelScope.launch {
            val dir = File("/sdcard/Recordings/Call/")
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { file ->
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
                }?.sortedByDescending { it.lastModified } ?: emptyList()
                _recordingFiles.value = files
            } else {
                _recordingFiles.value = emptyList()
            }
        }
    }

    /**
     * 환자별 통화 녹음 로딩 (서버에서)
     */
    fun loadPatientRecordings(patientId: String?) {
        // patientId가 null이면 전체 녹음 로딩
        if (patientId == null || patientId.isEmpty()) {
            loadRecordings() // Room 녹음 로딩
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 캐시 확인
                val now = System.currentTimeMillis()
                val lastUpdate = cacheTimestamp[patientId] ?: 0

                if (now - lastUpdate < CACHE_DURATION && recordingsCache.containsKey(patientId)) {
                    _recordingFiles.value = recordingsCache[patientId]!!
                    _isLoading.value = false
                    Log.d("CallRecordingViewModel", "캐시에서 로딩: $patientId")
                    return@launch
                }

                // API 호출
                Log.d("CallRecordingViewModel", "서버에서 로딩 시작: $patientId")
                val response = RetrofitClient.authApi.getPatientRecords(patientId)

                if (response.isSuccessful) {
                    val records = response.body() ?: emptyList()
                    val recordingFiles = records.map { it.toRecordingFile() }

                    // 캐시 업데이트
                    recordingsCache[patientId] = recordingFiles
                    cacheTimestamp[patientId] = now

                    _recordingFiles.value = recordingFiles
                    Log.d("CallRecordingViewModel", "로딩 완료: ${recordingFiles.size}개")
                } else {
                    _error.value = "서버 오류: ${response.code()}"
                    Log.e("CallRecordingViewModel", "서버 응답 오류: ${response.code()}")
                }

            } catch (e: Exception) {
                _error.value = "통화 녹음을 불러오는데 실패했습니다: ${e.message}"
                Log.e("CallRecordingViewModel", "로딩 실패", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 미리 로딩용
     */
    fun preloadPatientRecordings(patientId: String) {
        viewModelScope.launch {
            try {
                // 이미 캐시에 있으면 스킵
                val now = System.currentTimeMillis()
                val lastUpdate = cacheTimestamp[patientId] ?: 0

                if (now - lastUpdate < CACHE_DURATION && recordingsCache.containsKey(patientId)) {
                    Log.d("CallRecordingViewModel", "미리 로딩 스킵 (캐시 존재): $patientId")
                    return@launch
                }

                Log.d("CallRecordingViewModel", "미리 로딩 시작: $patientId")
                val response = RetrofitClient.authApi.getPatientRecords(patientId)

                if (response.isSuccessful) {
                    val records = response.body() ?: emptyList()
                    val recordingFiles = records.map { it.toRecordingFile() }

                    recordingsCache[patientId] = recordingFiles
                    cacheTimestamp[patientId] = now

                    Log.d("CallRecordingViewModel", "미리 로딩 완료: ${recordingFiles.size}개")
                }

            } catch (e: Exception) {
                // 미리 로딩은 실패해도 조용히 처리
                Log.d("CallRecordingViewModel", "미리 로딩 실패: ${e.message}")
            }
        }
    }

    /**
     * 캐시 클리어
     */
    fun clearCache() {
        recordingsCache.clear()
        cacheTimestamp.clear()
        Log.d("CallRecordingViewModel", "캐시 클리어 완료")
    }

    /**
     * 특정 환자 캐시만 클리어
     */
    fun clearPatientCache(patientId: String?) {
        if (patientId != null && patientId.isNotEmpty()) {
            recordingsCache.remove(patientId)
            cacheTimestamp.remove(patientId)
            Log.d("CallRecordingViewModel", "환자 캐시 클리어: $patientId")
        }
    }

    /**
     * 전체 녹음 캐시만 클리어
     */
    fun clearAllRecordingsCache() {
        recordingsCache.remove(ALL_RECORDINGS_KEY)
        cacheTimestamp.remove(ALL_RECORDINGS_KEY)
        Log.d("CallRecordingViewModel", "전체 녹음 캐시 클리어")
    }
}