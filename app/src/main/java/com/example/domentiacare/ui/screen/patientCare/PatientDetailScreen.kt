package com.example.domentiacare.ui.screen.patientCare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.domentiacare.data.remote.dto.Patient
import com.example.domentiacare.ui.component.DMT_Button

@Composable
fun PatientDetailScreen(navController: NavController,
                        patient: Patient
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 허용되면 즉시 전화 걸기
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${patient.phone}")
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }


    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "환자 아이콘",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 이름
            Text(
                text = patient.patientName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 정보 카드
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(label = "주소", value = "${patient.addressDetail1}")
                    InfoRow(label = "나이", value = "${patient.age}세" )
                    InfoRow(label = "성별", value = "${patient.gender}")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 버튼 2개
            Button(
                onClick = { navController.navigate("PatientLocationScreen/${patient.patientId}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("위치 보기")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("schedule") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("일정 보기")
            }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
                onClick = { /* TODO: 클릭 동작 통화?? */
                    // 현재 권한 상태 확인
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // 권한 있으면 바로 전화
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:${patient.phone}")
                        }
                        context.startActivity(intent)
                    } else {
                        // 권한 없으면 요청
                        launcher.launch(Manifest.permission.CALL_PHONE)
                    }},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("전화 걸기")
            }

            Text(text = "안녕하세요, 반가워요!", style = MaterialTheme.typography.bodyLarge)
            Text(text = "안녕하세요, 반가워요!", style = MaterialTheme.typography.bodyMedium)

            DMT_Button(
                text = "보라색 버튼",
                onClick = { /* ... */ },
                containerColor = Color(0xFF8E24AA) // 보라색 배경
            )

            DMT_Button(
                text = "기본 버튼",
                onClick = { /* ... */ }
            )
        }
    }



@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
