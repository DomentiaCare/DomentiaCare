package com.example.domentiacare.ui.screen.MyPage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domentiacare.ui.component.DMT_MenuItem
import com.example.domentiacare.ui.component.MyAppButton

@Composable
fun MyPageScreen(navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        // 상단 프로필 섹션
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF49000))
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("사용자 이름", color = Color.White, fontSize = 20.sp)
                Text("www.mysite.com", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "계정: 25541190   역할: 소유자",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 메뉴 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                DMT_MenuItem(icon = Icons.Default.Settings
                    , title = "웹사이트 기본 정보")
                Divider()
                DMT_MenuItem(icon = Icons.Default.Storage, title = "백엔드 관리")
                Divider()
                DMT_MenuItem(
                    icon = Icons.Default.Star,
                    title = "초대 혜택",
                    trailingText = "300포인트 보상"
                )
                Divider()
                DMT_MenuItem(
                    icon = Icons.Default.Language,
                    title = "언어",
                    trailingText = "한국어"
                )
            }
        }
//
//        Spacer(modifier = Modifier.height(16.dp))

//        // 초대 혜택 & 언어
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp),
//            shape = RoundedCornerShape(8.dp),
//            colors = CardDefaults.cardColors(containerColor = Color.White)
//        ) {
//            Column {
//                DMT_MenuItem(
//                    icon = Icons.Default.Star,
//                    title = "초대 혜택",
//                    trailingText = "300포인트 보상"
//                )
//                Divider()
//                DMT_MenuItem(
//                    icon = Icons.Default.Language,
//                    title = "언어",
//                    trailingText = "한국어"
//                )
//            }
//        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp), // 아래 여백 설정 가능
            contentAlignment = Alignment.BottomCenter
        ) {
            // 하단 버튼
            MyAppButton(
                onClick = { /* TODO: 웹사이트 전환 기능 */ },
                text = "웹사이트 전환"
            )
        }

    }
}