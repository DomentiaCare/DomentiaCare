package com.example.domentiacare.ui.calllog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domentiacare.ui.screen.call.CallLogViewModel

@Composable
fun CallLogScreen(viewModel: CallLogViewModel) {
    val logs by viewModel.callLogs

    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("통화 기록이 없습니다.")
        }
    } else {
        LazyColumn {
            items(logs) { entry ->
                CallLogItem(entry)
                Divider(thickness = 1.dp)
            }
        }
    }
}