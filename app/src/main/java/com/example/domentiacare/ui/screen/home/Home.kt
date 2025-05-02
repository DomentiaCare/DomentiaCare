package com.example.domentiacare.ui.screen.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domentiacare.ui.component.BottomNavBar
import com.example.domentiacare.ui.component.TopBar
import kotlinx.coroutines.CoroutineScope

@Composable
fun Home(navController: NavController,
         drawerState: DrawerState,
         scope: CoroutineScope
) {

    Scaffold(
        topBar = { TopBar(title = "DomenticaCare", drawerState = drawerState, scope = scope) },
        bottomBar = { BottomNavBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 정보 카드
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("안녕하세요, 박진호님", fontSize = 18.sp)
                        Text("오늘도 좋은 하루 보내세요!", fontSize = 14.sp, color = Color.Gray)
                    }

                    IconButton(onClick = { /* TODO: 클릭 동작 통화?? */ }) {
                        Icon(Icons.Default.Call, contentDescription = "통화")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Quick Access 텍스트
            Text("Quick Access", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))

            // 4. 2x2 버튼 그리드
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickAccessButton("길찾기", Icons.Default.Place, {})
                    QuickAccessButton("일정관리", Icons.Default.DateRange, {navController.navigate("schedule")})
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickAccessButton("환자관리", Icons.Default.Face, {navController.navigate("patientList")})
                    QuickAccessButton("설정", Icons.Default.Settings, {})
                }
            }
        }
    }}



@Composable
fun QuickAccessButton(label: String, icon: ImageVector, onClick: () -> Unit ) {
    Column(
        modifier = Modifier
            .size(160.dp) // 버튼 전체 크기
            .background(
                color = Color(0xFFE3F2FD), // 연한 파란색
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {onClick() }
            .padding(horizontal = 20.dp, vertical = 25.dp),

        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // 아이콘 배경 박스
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF2196F3), // 아이콘 진한 파란색
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 텍스트
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

