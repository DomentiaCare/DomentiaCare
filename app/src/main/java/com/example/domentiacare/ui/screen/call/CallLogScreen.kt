package com.example.domentiacare.ui.screen.call

import PythonConverter.convertM4aFileToWav
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.domentiacare.data.model.CallLogEntry
import com.example.domentiacare.data.model.CallRecordingViewModel
import com.example.domentiacare.data.model.RecordingFile
import com.example.domentiacare.data.util.convertM4aToWav
import com.example.domentiacare.data.util.getCallRecordingFiles
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallLog(
    val name: String,
    val type: String, // "발신", "수신", "부재중"
    val time: String, // 예: "어제", "오전 11:15", "4월 20일"
    val isSaved : Boolean = false // 저장 여부
)

@Composable
fun CallLogScreen(viewModel: CallRecordingViewModel = viewModel(),
                  navController: NavController) {
    //val logs by viewModel.callLogs
    val context = LocalContext.current
    val recordings by viewModel.recordingFiles.collectAsState()

    // 2) 최초 진입 시 한 번만 loadCallLogs() 실행
    LaunchedEffect(Unit) {
        viewModel.loadRecordings()
    }

    // 3) 실제 LazyColumn에 items(logs) 사용
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {

        // ✅ 상단 고정 아이템 (예: 헤더, 설명 등)
        item {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = Color(0xFFF1F1F1),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                onClick = {

                    //
                    Log.d("CallLogScreen", "상단 고정 아이템 클릭됨")
                }
            ) {
                Text(
                    text = "wav 파일을 Whisper에 전송 테스트",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
//        items(logs) { call ->
//            CallLogItem(call, navController)
//            Spacer(modifier = Modifier.height(8.dp))
//        }
        items(recordings) { file ->
            RecordingLogItem(file = file, navController = navController)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

@Composable
fun RecordingLogItem(
    file: RecordingFile,
    navController: NavController
) {
    val context = LocalContext.current
    val backgroundColor = Color(0xFFE3F2FD) // 파란 계열 배경(원하는 색상 변경 가능)
    val formattedDate = rememberFormattedDate(file.lastModified)

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 상세 화면 등으로 이동하거나 변환 처리
                // navController.navigate("RecordingDetailScreen/${file.path}")
                // 변환 함수 호출 (파일 전체 객체 넘김)
                val m4aFile = File(file.path)
                val outputWavName = m4aFile.nameWithoutExtension + ".wav"
                convertM4aFileToWav(context, m4aFile, outputWavName)
                Log.d("RecordingLogItem", "파일 클릭됨: ${file.path}")
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(backgroundColor, shape = MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = Color(0xFF1976D2) // 파란색
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "수정일: $formattedDate · 크기: ${formatSize(file.size)}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// 파일 날짜 포맷
@Composable
fun rememberFormattedDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

// 파일 용량 포맷 (KB, MB)
fun formatSize(size: Long): String {
    if (size < 1024) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}


@Composable
fun CallLogItem(call: CallLogEntry, navController: NavController) {
    val backgroundColor : Color = when (call.type) {
        3 -> {
            Color(0xFFFFCDD2) // 부재중, red
        }

        2 -> {
            Color(0xFFBBDEFB) // 발신, blue
        }

        1 -> {
            Color(0xFFC8E6C9) // 수신, green
        }

        else -> {
            //투명 색상
            Color.Transparent
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
//            Log.d("CallLogItem", "👉 클릭된 파일: ${call.recordingPath ?: "녹음 파일 없음"}")
//            call.recordingPath?.let { inputPath ->
//                val inputFile = File(inputPath)
//                val outputFile = File(
//                    inputFile.parentFile,
//                    inputFile.nameWithoutExtension + ".wav"
//                )
//
//                convertM4aToWav(inputFile, outputFile)
//                Log.d("CallLogItem", "변환 완료: ${outputFile.absolutePath}")
//
//                // 이후 필요한 작업 예시:
//                 navController.navigate("CallDetailScreen/${outputFile.absolutePath}")
//            } ?: run {
//                Log.e("CallLogItem", "녹음 파일 없음")
//            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(backgroundColor, shape = MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                // 전화 아이콘
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp)) // 가로 간격

            // 이름
            Column(modifier = Modifier.weight(1f)) {
                call.name?.let { Text(text = it, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Text(text = "${call.callTypeText}    통화시간: ${call.formattedDuration}", fontSize = 13.sp, color = Color.Gray)
            }
//            Icon(
//                imageVector = Icons.Default.DateRange,
//                contentDescription = "달력 아이콘",
//                tint = if(call.isSaved) Color(0xFFF49000) else Color.LightGray, // 초록색 또는 회색
//                modifier = Modifier.size(24.dp)
//            )
        }
    }
}