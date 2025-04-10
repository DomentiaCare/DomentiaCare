package com.example.domentiacare.ui.screen.navigate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class NavigateScreen {
    @Composable
    fun RouteFinderScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 상단: 출발지 / 도착지 입력
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = "", // 출발지 상태로 변경 필요
                    onValueChange = { /* 출발지 상태 업데이트 */ },
                    label = { Text("출발지") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = "", // 도착지 상태로 변경 필요
                    onValueChange = { /* 도착지 상태 업데이트 */ },
                    label = { Text("도착지") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 지도: 중앙
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
//                AndroidView(
//                    factory = { context ->
//                        NaverMapView(context).apply {
//                            getMapAsync { naverMap ->
//                                // 지도 설정, 마커 표시, 경로 그리기 등 처리
//                            }
//                        }
//                    },
//                    modifier = Modifier.fillMaxSize()
//                )
            }
        }
    }

}