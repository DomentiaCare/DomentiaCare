package com.example.domentiacare.ui.screen.call.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*

@Composable
fun AudioPlayerSection(filePath: String) {
    SectionCard(
        title = "통화 녹음",
        icon = Icons.Default.GraphicEq
    ) {
        ModernAudioPlayer(filePath = filePath)
    }
}