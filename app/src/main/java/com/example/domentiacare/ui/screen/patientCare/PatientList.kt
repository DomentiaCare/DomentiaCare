package com.example.domentiacare.ui.screen.patientCare

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.domentiacare.data.model.PatientViewModel
import com.example.domentiacare.data.remote.dto.Patient
import com.example.domentiacare.ui.component.DMT_Button
import com.example.domentiacare.ui.component.DMT_GrayButton

// 테마 색상 정의
val PrimaryOrange = Color(0xFFED7D31)
val LightOrange = Color(0xFFFDF3E7)
val DarkOrange = Color(0xFFD66D2A)
val SurfaceColor = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF666666)
val BackgroundGray = Color(0xFFF8F4F0)

@Composable
fun PatientList(navController: NavController) {
    val viewModel: PatientViewModel = viewModel()
    val patients = viewModel.patientList
    val isLoading = viewModel.isLoading
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPatients()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        BackgroundGray
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 헤더 섹션
            HeaderSection(
                patientCount = patients.size,
                onAddClick = { showDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 컨텐츠 영역
            when {
                isLoading -> LoadingSection()
                patients.isEmpty() -> EmptySection()
                else -> PatientListSection(
                    patients = patients,
                    onPatientClick = { patient ->
                        navController.navigate("patientDetail/${patient.patientId}")
                    }
                )
            }
        }

        // 다이얼로그
        if (showDialog) {
            RegisterPatientDialog(
                onDismiss = { showDialog = false },
                onConfirm = { phoneNumber ->
                    viewModel.addPatient(phoneNumber) { success ->
                        if (success) {
                            showDialog = false
                            Toast.makeText(context, "환자 등록 완료", Toast.LENGTH_SHORT).show()
                            viewModel.loadPatients()
                        } else {
                            Toast.makeText(context, "등록 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun HeaderSection(
    patientCount: Int,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 상단 그라데이션 바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryOrange, DarkOrange)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 제목 영역
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "환자 관리",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "등록된 환자들을 체계적으로 관리합니다",
                        fontSize = 13.sp,
                        color = OnSurfaceLight
                    )
                }

                // 통계 및 버튼
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 환자 수 표시
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightOrange
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = patientCount.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange
                            )
                            Text(
                                text = "총 환자",
                                fontSize = 10.sp,
                                color = OnSurfaceLight
                            )
                        }
                    }

                    // 추가 버튼
                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = PrimaryOrange,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "환자 추가",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingSection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = PrimaryOrange,
                strokeWidth = 3.dp,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "환자 목록을 불러오는 중...",
                color = OnSurfaceLight,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun EmptySection() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = LightOrange,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    fontSize = 24.sp
                )
            }

            Text(
                text = "등록된 환자가 없습니다",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryOrange,
                textAlign = TextAlign.Center
            )

            Text(
                text = "우측 상단의 + 버튼을 눌러\n첫 번째 환자를 등록해보세요",
                fontSize = 13.sp,
                color = OnSurfaceLight,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PatientListSection(
    patients: List<Patient>,
    onPatientClick: (Patient) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(patients) { index, patient ->
            PatientCard(
                patient = patient,
                index = index,
                onClick = { onPatientClick(patient) }
            )
        }
    }
}

@Composable
fun PatientCard(
    patient: Patient,
    index: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 환자 아바타
            PatientAvatar(
                patient = patient,
                index = index
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 환자 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.patientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip(
                        text = "${patient.age}세",
                        backgroundColor = LightOrange.copy(alpha = 0.7f)
                    )
                    InfoChip(
                        text = patient.gender,
                        backgroundColor = LightOrange.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 상태 표시
            StatusBadge(
                isNormal = patient.isSameSigungu == true
            )
        }
    }
}

@Composable
fun PatientAvatar(
    patient: Patient,
    index: Int
) {
    val icon: ImageVector = when (index % 3) {
        0 -> Icons.Filled.Face
        1 -> Icons.Filled.AccountCircle
        else -> Icons.Filled.Person
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = LightOrange,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "환자 아바타",
            tint = PrimaryOrange,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun InfoChip(
    text: String,
    backgroundColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryOrange
        )
    }
}

@Composable
fun StatusBadge(
    isNormal: Boolean
) {
    val (backgroundColor, textColor, text) = if (isNormal) {
        Triple(Color(0xFFE8F5E8), Color(0xFF2E7D32), "정상")
    } else {
        Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "확인요망")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
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
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = LightOrange,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "환자 등록",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "등록하실 환자의 전화번호를 입력해주세요",
                    fontSize = 13.sp,
                    color = OnSurfaceLight
                )
            }
        },
        text = {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = {
                    Text("전화번호")
                },
                placeholder = {
                    Text(
                        text = "01012345678",
                        color = OnSurfaceLight.copy(alpha = 0.6f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    focusedLabelColor = PrimaryOrange,
                    cursorColor = PrimaryOrange
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            DMT_Button(
                text = "등록",
                onClick = {
                    if (phoneNumber.isNotBlank()) {
                        onConfirm(phoneNumber)
                    }
                }
            )
        },
        dismissButton = {
            DMT_GrayButton(
                text = "취소",
                onClick = onDismiss
            )
        }
    )
}