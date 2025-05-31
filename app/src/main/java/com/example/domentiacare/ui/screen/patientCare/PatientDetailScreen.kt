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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.domentiacare.data.model.CallRecordingViewModel
import com.example.domentiacare.data.remote.dto.Patient

@Composable
fun PatientDetailScreen(navController: NavController,
                        patient: Patient,
                        callRecordingViewModel: CallRecordingViewModel = viewModel()
) {
    val context = LocalContext.current

    // 미리 로딩 - 환자 상세 화면 진입 시 백그라운드로 통화 녹음 데이터 로딩
    LaunchedEffect(patient.patientId) {
        callRecordingViewModel.preloadPatientRecordings(patient.patientId.toString())
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
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
        // 환자 프로필 섹션
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "환자 아이콘",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = patient.patientName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                InfoRow(label = "나이", value = "${patient.age}세")
                InfoRow(label = "성별", value = "${patient.gender}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 기능 버튼들을 2x2 그리드로 배치
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 첫 번째 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "위치 보기",
                    icon = Icons.Filled.LocationOn,
                    backgroundColor = Color(0xFF4CAF50),
                    onClick = { navController.navigate("PatientLocationScreen/${patient.patientId}") }
                )

                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "일정 보기",
                    icon = Icons.Filled.Schedule,
                    backgroundColor = Color(0xFF2196F3),
                    onClick = { navController.navigate("schedule/${patient.patientId}") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "통화 녹음",
                    icon = Icons.Filled.VoiceChat,
                    backgroundColor = Color(0xFFFF9800),
                    onClick = { navController.navigate("CallLogScreen/${patient.patientId}") }
                )

                ActionButton(
                    modifier = Modifier.weight(1f),
                    text = "전화 걸기",
                    icon = Icons.Filled.Call,
                    backgroundColor = Color(0xFFF44336),
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CALL_PHONE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:${patient.phone}")
                            }
                            context.startActivity(intent)
                        } else {
                            launcher.launch(Manifest.permission.CALL_PHONE)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
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