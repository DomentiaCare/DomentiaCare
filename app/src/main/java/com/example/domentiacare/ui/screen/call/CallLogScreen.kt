package com.example.domentiacare.ui.screen.call

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.domentiacare.data.model.CallLogEntry
import com.example.domentiacare.data.model.CallRecordingViewModel
import com.example.domentiacare.data.model.RecordingFile
import com.example.domentiacare.data.util.convertM4aToWavForWhisper
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordLog(
    val name: String,
    val type: String,
    val time: String,
    val isSaved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    patientId: String? = null,
    viewModel: CallRecordingViewModel = viewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val recordings by viewModel.recordingFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // 주황색 컬러 정의
    val primaryOrange = Color(0xFFFF6B35)
    val lightOrange = Color(0xFFFFE5DB)
    val darkOrange = Color(0xFFBF2600)

    LaunchedEffect(patientId) {
        Log.d("CallLogScreen", "patientId: $patientId")
        viewModel.loadPatientRecordings(patientId)
    }

    // 전체 컨테이너를 흰색으로 감싸기
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)  // 전체 배경 명시적 흰색
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "통화 녹음",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1E)
                            )
                            Text(
                                text = "${recordings.size}개의 녹음",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF49454E)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = primaryOrange
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1C1B1E)
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            containerColor = Color.White  // 명시적으로 흰색 지정
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)  // LazyColumn 배경 명시적 흰색
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 에러 상태
                error?.let { errorMessage ->
                    item {
                        ErrorCard(errorMessage = errorMessage)
                    }
                }

                // 로딩 상태
                if (isLoading) {
                    items(5) {
                        ShimmerRecordingItem()
                    }
                } else {
                    // 빈 상태
                    if (recordings.isEmpty()) {
                        item {
                            EmptyStateCard()
                        }
                    } else {
                        // 녹음 파일 리스트
                        items(recordings) { file ->
                            RecordingLogItem(
                                file = file,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorCard(errorMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "오류가 발생했습니다",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD32F2F)
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F).copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    val primaryOrange = Color(0xFFFF6B35)
    val lightOrange = Color(0xFFFFE5DB)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = lightOrange,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.VoiceChat,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = primaryOrange
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "아직 통화 녹음이 없어요",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1E),
                textAlign = TextAlign.Center
            )

            Text(
                text = "통화가 녹음되면 여기에 표시됩니다.\n녹음 권한을 확인해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF49454E),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ShimmerRecordingItem() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun RecordingLogItem(
    file: RecordingFile,
    navController: NavController
) {
    val formattedDate = rememberFormattedDate(file.lastModified)
    val duration = rememberRecordingDuration(file.size)

    val primaryOrange = Color(0xFFFF6B35)
    val lightOrange = Color(0xFFFFE5DB)
    val secondaryOrange = Color(0xFFFF8C42)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val encodedPath = URLEncoder.encode(file.path, "utf-8")
                navController.navigate("CallDetailScreen/$encodedPath")
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // 아이콘 컨테이너
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = lightOrange,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = primaryOrange,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 텍스트 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name.removeSuffix(".m4a"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1B1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF49454E)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454E)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF49454E)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatSize(file.size)} • $duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454E)
                    )
                }
            }

            // 재생 버튼
            IconButton(
                onClick = {
                    val encodedPath = URLEncoder.encode(file.path, "utf-8")
                    navController.navigate("CallDetailScreen/$encodedPath")
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = secondaryOrange.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "재생",
                    tint = secondaryOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ), label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE0E0E0),
                Color(0xFFF0F0F0),
                Color(0xFFE0E0E0)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun rememberFormattedDate(timeMillis: Long): String {
    return remember(timeMillis) {
        val now = System.currentTimeMillis()
        val diff = now - timeMillis

        when {
            diff < 60 * 1000 -> "방금 전"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}분 전"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}시간 전"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}일 전"
            else -> {
                val sdf = SimpleDateFormat("MM월 dd일", Locale.getDefault())
                sdf.format(Date(timeMillis))
            }
        }
    }
}

@Composable
fun rememberRecordingDuration(fileSize: Long): String {
    return remember(fileSize) {
        // 대략적인 녹음 시간 계산 (평균 비트레이트 기준)
        val estimatedSeconds = (fileSize / 8000).toInt() // 대략적인 계산
        val minutes = estimatedSeconds / 60
        val seconds = estimatedSeconds % 60

        when {
            minutes == 0 -> "${seconds}초"
            minutes < 60 -> "${minutes}분 ${seconds}초"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                "${hours}시간 ${remainingMinutes}분"
            }
        }
    }
}

fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
    }
}