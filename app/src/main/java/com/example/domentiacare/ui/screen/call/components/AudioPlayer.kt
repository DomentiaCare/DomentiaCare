package com.example.domentiacare.ui.screen.call.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.domentiacare.ui.screen.call.theme.OrangeLight
import com.example.domentiacare.ui.screen.call.theme.OrangePrimary
import kotlinx.coroutines.delay

@Composable
fun ModernAudioPlayer(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    DisposableEffect(filePath) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                duration = this.duration
            }
            duration = mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            errorMessage = "오디오 파일을 열 수 없습니다: ${e.message}"
        }
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            if (!isSeeking) {
                position = mediaPlayer?.currentPosition ?: 0
                sliderPosition = position.toFloat()
            }
            delay(200)
        }
    }

    fun onSliderValueChange(value: Float) {
        isSeeking = true
        sliderPosition = value
    }

    fun onSliderValueChangeFinished() {
        isSeeking = false
        mediaPlayer?.seekTo(sliderPosition.toInt())
        position = sliderPosition.toInt()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                OrangeLight.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 재생/일시정지 버튼
                IconButton(
                    onClick = {
                        if (mediaPlayer != null) {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                            } else {
                                mediaPlayer?.start()
                            }
                            isPlaying = !isPlaying
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(OrangePrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "일시정지" else "재생",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD32F2F)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(position),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454E)
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454E)
                            )
                        }
                    }
                }
            }

            // 재생바
            if (duration > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderPosition.coerceIn(0f, duration.toFloat()),
                    onValueChange = { onSliderValueChange(it) },
                    onValueChangeFinished = { onSliderValueChangeFinished() },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = OrangePrimary,
                        activeTrackColor = OrangePrimary,
                        inactiveTrackColor = Color(0xFFE0E0E0)
                    )
                )
            }
        }
    }
}

fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}