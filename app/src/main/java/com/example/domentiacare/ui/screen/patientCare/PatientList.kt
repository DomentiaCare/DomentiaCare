package com.example.domentiacare.ui.screen.patientCare

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.domentiacare.data.model.PatientViewModel
import com.example.domentiacare.data.remote.dto.Patient
import com.example.domentiacare.ui.component.DMT_Button
import com.example.domentiacare.ui.component.DMT_GrayButton


@Composable
fun PatientList(navController: NavController
) {
    val viewModel: PatientViewModel = viewModel() // ← 지역변수처럼 선언
    val patients = viewModel.patientList
    val isLoading = viewModel.isLoading

    val context = LocalContext.current


    LaunchedEffect(Unit) {
        viewModel.loadPatients()
    }

    // ✅ 다이얼로그 표시 여부 관리
    var showDialog by remember { mutableStateOf(false) }

/*    val patients = listOf(
        Patient("김철수", 75, "치매 초기"),
        Patient("박영희", 80, "치매 중기"),
        Patient("이민수", 77, "치매 초기"),
        Patient("최지은", 82, "치매 말기"),
        Patient("김철수", 75, "치매 초기"),
        Patient("박영희", 80, "치매 중기"),
        Patient("이민수", 77, "치매 초기"),
        Patient("최지은", 82, "치매 말기")
    )*/

    //로딩
    if (isLoading) {
        // ✅ 로딩 UI 예시
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 환자 등록 버튼
//            Button(
//                onClick = {
//                    // TODO: 등록 화면 이동
//                    showDialog = true
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(48.dp)
//            ) {
//                Text(text = "환자 등록")
//            }
            DMT_Button(
                text = "환자 등록",
                onClick = {
                    showDialog = true;
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 환자 리스트
            LazyColumn(
                //verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(patients) { index, patient ->
                    PatientCard(patient, index){
                        navController.navigate("patientDetail/${patient.patientId}")
                    }
                    Divider()
                }
            }
            // ✅ 다이얼로그 컴포저블 호출
            if (showDialog) {
                RegisterPatientDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { phoneNumber ->
                        // TODO: 여기서 enteredId를 저장하거나 처리
                        println("입력한 전화번호: $phoneNumber")
                        viewModel.addPatient(phoneNumber) { success ->
                            if (success) {
                                showDialog = false
                                Toast.makeText(context, "환자 등록 완료", Toast.LENGTH_SHORT).show()
                                viewModel.loadPatients()
                            }
                            else{
                                Toast.makeText(context, "등록 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }


}

@Composable
fun PatientCard(patient: Patient, index: Int, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon: ImageVector = when (index % 3) {
                0 -> Icons.Filled.Face
                1 -> Icons.Filled.AccountCircle
                else -> Icons.Filled.Person
            }

            Icon(
                imageVector = icon,
                contentDescription = "Patient Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.patientName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "나이: ${patient.age}세",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = patient.gender,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (patient.isSameSigungu == true) "정상 범위" else "확인 요망",
                style = MaterialTheme.typography.bodyMedium,
                color = if (patient.isSameSigungu == true) MaterialTheme.colorScheme.secondary else Color.Red
            )
        }
    }
}


@Composable
fun RegisterPatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "환자 ID 입력")
        },
        text = {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("ID") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            DMT_Button(
                text = "등록",
                onClick = { onConfirm(phoneNumber) }
            )
        },
        dismissButton = {
            DMT_GrayButton(
                text = "취소",
                onClick = onDismiss
            )
        },
        containerColor = Color.White
    )
}