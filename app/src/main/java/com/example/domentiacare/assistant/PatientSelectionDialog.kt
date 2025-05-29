package com.example.domentiacare.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domentiacare.data.remote.dto.Patient

// 커스텀 색상 정의
private val PrimaryOrange = Color(0xFFED7D31)
private val LightCream = Color(0xFFFDF3E7)
private val DarkOrange = Color(0xFFD6691A)
private val SoftGray = Color(0xFFF5F5F5)
private val ErrorRed = Color(0xFFE53E3E)

@Composable
fun PatientSelectionDialog(
    patients: List<Patient>,
    onPatientSelected: (Patient) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 헤더 섹션 (고정)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryOrange, DarkOrange)
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "환자 선택",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "전화를 걸 환자를 선택하세요",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 환자 목록 (스크롤 가능)
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(patients) { index, patient ->
                            var isPressed by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = if (isPressed) 6.dp else 2.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        isPressed = true
                                        onPatientSelected(patient)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (patient.phone.isNotBlank()) LightCream else SoftGray
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 환자 프로필 아이콘 (크기 조정)
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                if (patient.phone.isNotBlank()) PrimaryOrange else ErrorRed,
                                                CircleShape
                                            )
                                            .border(
                                                width = 2.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (patient.phone.isNotBlank())
                                                Icons.Default.Person else Icons.Default.Warning,
                                            contentDescription = "환자",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // 환자 정보
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = patient.patientName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2D3748),
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        if (patient.phone.isNotBlank()) {
                                            Surface(
                                                color = PrimaryOrange.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = patient.phone,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = DarkOrange,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 3.dp
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        } else {
                                            Surface(
                                                color = ErrorRed.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "전화번호 없음",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = ErrorRed,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 3.dp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // 액션 아이콘
                                    if (patient.phone.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    PrimaryOrange.copy(alpha = 0.1f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "전화",
                                                tint = PrimaryOrange,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 마지막 아이템 하단 여백
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // 버튼 영역 (고정)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "취소",
                                color = Color(0xFF718096),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}