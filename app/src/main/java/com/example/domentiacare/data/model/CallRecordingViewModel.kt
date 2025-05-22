package com.example.domentiacare.data.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class RecordingFile(
    val name: String,
    val path: String,
    val lastModified: Long,
    val size: Long
)

class CallRecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val _recordingFiles = MutableStateFlow<List<RecordingFile>>(emptyList())
    val recordingFiles: StateFlow<List<RecordingFile>> = _recordingFiles

    fun loadRecordings() {
        viewModelScope.launch {
            val dir = File("/sdcard/Recordings/Call/")
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { file ->
                    file.isFile && (file.extension.equals("m4a", true) || file.extension.equals("wav", true) || file.extension.equals("mp3", true))
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
}
