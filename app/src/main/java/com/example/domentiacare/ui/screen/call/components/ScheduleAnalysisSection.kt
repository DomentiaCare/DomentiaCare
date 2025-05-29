package com.example.domentiacare.ui.screen.call.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domentiacare.ui.screen.call.theme.OrangePrimary

@Composable
fun ScheduleAnalysisSection(
    memo: String,
    onMemoChange: (String) -> Unit,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit
) {
    SectionCard(
        title = "일정 내용",
        icon = Icons.Default.EventNote
    ) {
        OutlinedTextField(
            value = memo,
            onValueChange = onMemoChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("예: 재통화 예정", color = Color(0xFF9E9E9E)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = OrangePrimary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            minLines = 2,
            maxLines = 4
        )
    }
}