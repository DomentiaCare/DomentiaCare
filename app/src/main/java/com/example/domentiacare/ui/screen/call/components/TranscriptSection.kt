package com.example.domentiacare.ui.screen.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domentiacare.ui.screen.call.theme.OrangePrimary

@Composable
fun TranscriptSection(
    transcript: String,
    isLoading: Boolean,
    onTranscribe: () -> Unit = {} // 사용하지 않음
) {
    SectionCard(
        title = "통화 텍스트",
        icon = Icons.Default.TextFields
        // actionButton 제거 - 버튼 없음
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 200.dp)
                .background(
                    Color(0xFFF8F9FA),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "음성을 텍스트로 변환 중...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
                }
            } else if (transcript.isBlank()) {
                Text(
                    text = "음성이 자동으로 텍스트로 변환됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1C1B1E),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
    }
}