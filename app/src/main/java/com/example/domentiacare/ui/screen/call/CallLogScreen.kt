package com.example.domentiacare.ui.screen.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class CallLog(
    val name: String,
    val type: String, // "발신", "수신", "부재중"
    val time: String, // 예: "어제", "오전 11:15", "4월 20일"
    val isSaved : Boolean = false // 저장 여부
)

@Composable
fun CallLogScreen(navController: NavController) {
    val callLogs = listOf(
        CallLog("밤지성", "발신", "오전 11:15", true),
        CallLog("김민서", "부재중", "어제", true),
        CallLog("010-1234-5678", "발신", "어제", false),
        CallLog("최준혁", "수신", "4월 22일", true),
        CallLog("010-9876-5432", "발신", "4월 20일", true),
        CallLog("이서우", "수신", "4월 19일", false),
        CallLog("김민서", "부재중", "어제", true),
        CallLog("010-1234-5678", "발신", "어제", false),
        CallLog("최준혁", "수신", "4월 22일", true),
        CallLog("010-9876-5432", "발신", "4월 20일", true),
        CallLog("이서우", "수신", "4월 19일", false),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(callLogs) { call ->
            CallLogItem(call, navController)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CallLogItem(call: CallLog, navController: NavController) {
    val backgroundColor = when (call.type) {
        "부재중" -> Color(0xFFFFCDD2) // red
        else -> Color(0xFFC8E6C9) // green
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            navController.navigate("CallDetailScreen")
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
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = call.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "${call.type} ${call.time}", fontSize = 13.sp, color = Color.Gray)
            }
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "통화 아이콘",
                tint = if(call.isSaved) Color(0xFFF49000) else Color.LightGray, // 초록색 또는 회색
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
