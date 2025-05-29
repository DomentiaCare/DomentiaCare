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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domentiacare.data.local.CurrentUser

// 테마 색상 정의 (Home.kt와 동일)
object MyPageDesignTokens {
    val OrangePrimary = Color(0xFFED7D31)      // 메인 주황색
    val OrangeLight = Color(0xFFFFF4E6)        // 연한 주황색 배경
    val OrangeDark = Color(0xFFE67E00)         // 진한 주황색
    val WhiteBackground = Color(0xFFFFFFFF)    // 배경
    val WarmGray = Color(0xFFF8F8F8)           // 따뜻한 회색
    val TextPrimary = Color(0xFF2D2D2D)        // 진한 회색
    val TextSecondary = Color(0xFF757575)      // 중간 회색
    val TextLight = Color(0xFF9E9E9E)          // 연한 회색
    val CardShadow = Color(0x0A000000)         // 부드러운 그림자
    val Elevation2 = 4.dp
    val CornerRadius = 16.dp
}

@Composable
fun MyPageScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val user = CurrentUser.user
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MyPageDesignTokens.OrangeLight.copy(alpha = 0.3f),
                        MyPageDesignTokens.WhiteBackground,
                        MyPageDesignTokens.WarmGray
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 상단 프로필 섹션
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(
                        MyPageDesignTokens.Elevation2,
                        RoundedCornerShape(MyPageDesignTokens.CornerRadius)
                    ),
                shape = RoundedCornerShape(MyPageDesignTokens.CornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MyPageDesignTokens.WhiteBackground
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MyPageDesignTokens.OrangePrimary,
                                    MyPageDesignTokens.OrangeDark
                                )
                            )
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 프로필 이미지
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MyPageDesignTokens.WhiteBackground.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "프로필 이미지",
                                modifier = Modifier.size(64.dp),
                                tint = MyPageDesignTokens.WhiteBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 사용자 이름
                        Text(
                            text = user?.nickname ?: "사용자",
                            color = MyPageDesignTokens.WhiteBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 환영 메시지
                        Text(
                            text = "DementiaCare와 함께하세요",
                            color = MyPageDesignTokens.WhiteBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 사용자 기본 정보 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        MyPageDesignTokens.Elevation2,
                        RoundedCornerShape(MyPageDesignTokens.CornerRadius)
                    ),
                shape = RoundedCornerShape(MyPageDesignTokens.CornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MyPageDesignTokens.WhiteBackground
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "기본 정보",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MyPageDesignTokens.TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 닉네임
                    UserInfoItem(
                        icon = Icons.Default.Person,
                        label = "닉네임",
                        value = user?.nickname ?: "정보 없음"
                    )

                    Divider(
                        color = MyPageDesignTokens.WarmGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // 이메일
                    UserInfoItem(
                        icon = Icons.Default.Email,
                        label = "이메일",
                        value = user?.email ?: "정보 없음"
                    )

                    Divider(
                        color = MyPageDesignTokens.WarmGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // 전화번호 (있다면)
                    UserInfoItem(
                        icon = Icons.Default.Phone,
                        label = "전화번호",
                        value = user?.phone ?: "등록되지 않음"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 로그아웃 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showLogoutDialog = true }
                    .shadow(
                        MyPageDesignTokens.Elevation2,
                        RoundedCornerShape(MyPageDesignTokens.CornerRadius)
                    ),
                shape = RoundedCornerShape(MyPageDesignTokens.CornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MyPageDesignTokens.WhiteBackground
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color.Red.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "로그아웃",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "로그아웃",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MyPageDesignTokens.TextPrimary
                        )
                        Text(
                            text = "계정에서 안전하게 로그아웃",
                            style = MaterialTheme.typography.bodySmall,
                            color = MyPageDesignTokens.TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "더보기",
                        tint = MyPageDesignTokens.TextLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp)) // 하단 여백
        }
    }

    // 로그아웃 확인 다이얼로그
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "로그아웃",
                    fontWeight = FontWeight.Bold,
                    color = MyPageDesignTokens.TextPrimary
                )
            },
            text = {
                Text(
                    text = "정말 로그아웃 하시겠습니까?",
                    color = MyPageDesignTokens.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(
                        text = "로그아웃",
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        text = "취소",
                        color = MyPageDesignTokens.OrangePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            containerColor = MyPageDesignTokens.WhiteBackground,
            shape = RoundedCornerShape(MyPageDesignTokens.CornerRadius)
        )
    }
}

@Composable
private fun UserInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MyPageDesignTokens.OrangePrimary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MyPageDesignTokens.OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MyPageDesignTokens.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MyPageDesignTokens.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}