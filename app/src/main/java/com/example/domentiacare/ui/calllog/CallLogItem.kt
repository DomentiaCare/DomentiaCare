package com.example.domentiacare.ui.calllog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domentiacare.data.model.CallLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.CallLog
import androidx.compose.runtime.remember

@Composable
fun CallLogItem(entry: CallLogEntry) {
    val dateStr = remember(entry.date) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(entry.date))
    }
    val typeLabel = when (entry.type) {
        CallLog.Calls.INCOMING_TYPE -> "수신"
        CallLog.Calls.OUTGOING_TYPE -> "발신"
        CallLog.Calls.MISSED_TYPE   -> "부재중"
        else                        -> "기타"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = entry.name ?: entry.number,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$typeLabel · $dateStr · ${entry.duration}초",
            style = MaterialTheme.typography.bodySmall
        )
    }
}