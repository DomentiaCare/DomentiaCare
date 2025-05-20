package com.example.domentiacare.ui.screen.call

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.model.CallLogEntry
import com.example.domentiacare.data.util.queryCallLogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallLogViewModel(application: Application) : AndroidViewModel(application) {
    private val _callLogs = mutableStateOf<List<CallLogEntry>>(emptyList())
    val callLogs: State<List<CallLogEntry>> = _callLogs

    fun loadCallLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val logs = queryCallLogs(getApplication())
            _callLogs.value = logs
        }
    }
}