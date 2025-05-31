package com.example.domentiacare.ui.screen.call

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.model.CallLogEntry
import com.example.domentiacare.data.model.dateFormattedForShortFileName
import com.example.domentiacare.data.model.onlyDigits
import com.example.domentiacare.data.util.getCallRecordingFiles
import com.example.domentiacare.data.util.queryCallLogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallLogViewModel(application: Application) : AndroidViewModel(application) {
    private val _callLogs = mutableStateOf<List<CallLogEntry>>(emptyList())
    val callLogs: State<List<CallLogEntry>> = _callLogs

    fun loadCallLogs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val callLogsRaw = queryCallLogs(context)
            val recordings = getCallRecordingFiles(context)

            // 로그: 총 녹음 파일 수 확인
            Log.d("CallLogDebug", "🔍 녹음 파일 ${recordings.size}개 탐색 시작")

            // 파일명 기준 Map 구성
            val recordingMap = recordings.associateBy { it.nameWithoutExtension }

            val enrichedLogs = callLogsRaw
                .take(50)
                .mapIndexed { index, call ->
                    val dateKey = call.dateFormattedForShortFileName() // yyMMdd
                    val nameKey = call.name?.trim() ?: ""
                    val numberKey = call.number.onlyDigits()

                    // 로그: 현재 통화 항목 정보 출력
                    Log.d("CallLogDebug", "📞 [$index] ${call.name ?: call.number}, 날짜=$dateKey, 번호=$numberKey")

                    // 1. 이름 기반 매칭
                    val matchedFileByName = recordingMap.entries.find { (filename, _) ->
                        filename.contains(nameKey) && filename.contains(dateKey)
                    }

                    if (matchedFileByName != null) {
                        Log.d("CallLogDebug", "✅ 이름으로 매칭됨 → ${matchedFileByName.key}")
                    }

                    // 2. 이름 매칭 실패 시 번호 기반 매칭
                    val matchedFileByNumber = if (matchedFileByName == null) {
                        recordingMap.entries.find { (filename, _) ->
                            filename.contains(numberKey) && filename.contains(dateKey)
                        }
                    } else null

                    val matched = matchedFileByName?.value ?: matchedFileByNumber?.value

                    if (matched == null) {
                        Log.w("CallLogDebug", "❌ 매칭 실패 → 이름=$nameKey, 번호=$numberKey, 날짜=$dateKey")
                    }

                    call.copy(recordingPath = matched?.absolutePath)
                }

            _callLogs.value = enrichedLogs
        }
    }
}