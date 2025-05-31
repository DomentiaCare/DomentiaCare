package com.example.domentiacare.ui.screen.call.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domentiacare.ui.screen.call.theme.OrangePrimary

@Composable
fun ScheduleAnalysisSection(
    memo: String,
    onMemoChange: (String) -> Unit,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit = {}, // 사용하지 않음
    transcript: String
) {
    SectionCard(
        title = "일정 내용",
        icon = Icons.Default.EventNote
        // actionButton 제거 - 버튼 없음
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAnalyzing) {
                // 분석 중 상태 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = OrangePrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI가 일정을 분석하고 있습니다...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
                }
            } else if (transcript.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "💡 음성 변환 완료 후 자동으로 일정이 분석됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = memo,
                onValueChange = onMemoChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (isAnalyzing) "AI가 분석 중입니다..."
                        else "AI가 분석한 일정 내용이 여기에 표시됩니다",
                        color = Color(0xFF9E9E9E)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = OrangePrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                minLines = 2,
                maxLines = 4,
                readOnly = isAnalyzing
            )
        }
    }
}