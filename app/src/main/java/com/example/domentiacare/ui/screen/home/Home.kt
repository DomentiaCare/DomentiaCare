package com.example.domentiacare.ui.screen.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

// 디자인 시스템 색상 정의 - 어르신 친화적으로 개선
object DesignTokens {
    val OrangePrimary = Color(0xFFF49000)   // 메인 주황색
    val OrangeLight = Color(0xFFFFF4E6)     // 연한 주황색
    val OrangeDark = Color(0xFFE67E00)      // 진한 주황색
    val WhiteBackground = Color(0xFFFFFFFF) // 배경
    val WarmGray = Color(0xFFF8F8F8)        // 따뜻한 회색 배경
    val SurfaceVariant = Color(0xFFF5F5F5)
    val TextPrimary = Color(0xFF2D2D2D)     // 진한 회색
    val TextSecondary = Color(0xFF757575)   // 중간 회색
    val TextLight = Color(0xFF9E9E9E)       // 연한 회색
    val Success = Color(0xFF4CAF50)         // 성공/건강 그린
    val Error = Color(0xFFE53E3E)          // 에러 레드
    val Info = Color(0xFF2196F3)           // 정보 블루

    val CardShadow = Color(0x0A000000)     // 부드러운 그림자
    val OrangeShadow = Color(0x20F49000)   // 주황 그림자
    val Elevation1 = 2.dp
    val Elevation2 = 4.dp
    val Elevation3 = 8.dp

    val CornerRadius = 16.dp
    val CornerRadiusLarge = 24.dp
}

@Composable
fun Home(navController: NavController) {
    val context = LocalContext.current
    var user by remember { mutableStateOf<User?>(null) }
    val scrollState = rememberScrollState()
    var managerPhone by remember { mutableStateOf<String?>(null) } // 매니저 전화번호 상태 추가

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    LaunchedEffect(Unit) {
        val jwt = TokenManager.getToken()
        Log.d("KakaoLogin", "저장되어있던 JWT: $jwt")
        if (!jwt.isNullOrEmpty()) {
            RetrofitClient.authApi.getMyInfo("Bearer $jwt")
                .enqueue(object : Callback<User> {
                    override fun onResponse(call: Call<User>, response: Response<User>) {
                        if (response.isSuccessful) {
                            user = response.body()
                            CurrentUser.user = user
                            Log.d("KakaoLogin", "사용자 정보 로드 완료: ${user}")
                        } else {
                            Log.e("KakaoLogin", "사용자 정보 요청 실패: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<User>, t: Throwable) {
                        Log.e("Home", "서버 요청 실패", t)
                    }
                })
        }
    }

    LaunchedEffect(user) {
        user?.let { userData ->
            try {
                // managerId가 따로 있다면 그것을 사용, 현재는 user.id를 사용
                val managerId = userData.managerId // 또는 userData.managerId가 있다면 그것 사용
                val response = RetrofitClient.authApi.getProtectorPhone(managerId)
                if (response.isSuccessful) {
                    val phoneNumber = response.body()?.string()
                    managerPhone = phoneNumber
                    val updatedUser = userData.copy(managerPhone = phoneNumber ?: "")
                    CurrentUser.user = updatedUser
                    Log.d("Home", "매니저 전화번호 로드 완료: $managerPhone")
                } else {
                    Log.e("Home", "매니저 전화번호 요청 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Home", "매니저 전화번호 요청 중 오류", e)
            }
        }
    }

    val phoneNumber = "010-1234-4567"
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$managerPhone")
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DesignTokens.OrangeLight.copy(alpha = 0.3f),
                        DesignTokens.WhiteBackground,
                        DesignTokens.WarmGray
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp)) // 상단 여백 증가

            // ✅ navController를 HeaderSection에 전달
            HeaderSection(navController)

            Spacer(modifier = Modifier.height(24.dp)) // 더 큰 간격

            // 웰컴 카드
            WelcomeCard(
                user = user,
                onCallClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:$managerPhone")
                        }
                        context.startActivity(intent)
                    } else {
                        launcher.launch(Manifest.permission.CALL_PHONE)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp)) // 더 큰 간격

            // 빠른 메뉴 섹션
            QuickMenuSection(navController)

            Spacer(modifier = Modifier.height(16.dp)) // 하단 여백 증가
        }
    }
    // Home 함수 마지막 부분에 다이얼로그 추가 (기존 } 앞에)
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("앱 종료", fontWeight = FontWeight.Bold, color = DesignTokens.TextPrimary) },
            text = { Text("DementiaCare를 종료하시겠습니까?", color = DesignTokens.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    (context as? ComponentActivity)?.finish()
                }) {
                    Text("종료", color = DesignTokens.Error, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("취소", color = DesignTokens.OrangePrimary, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = DesignTokens.WhiteBackground,
            shape = RoundedCornerShape(DesignTokens.CornerRadius)
        )
    }
}

@Composable
private fun HeaderSection(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = getCurrentTimeGreeting(),
                style = MaterialTheme.typography.bodyLarge, // 더 큰 폰트
                color = DesignTokens.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "DementiaCare",
                style = MaterialTheme.typography.headlineLarge, // 더 큰 제목
                fontWeight = FontWeight.Bold,
                color = DesignTokens.TextPrimary
            )
        }

        // ✅ 알림 아이콘 제거, 프로필 아이콘만 유지하고 클릭 시 MyPageScreen으로 이동
        IconButton(
            onClick = {
                navController.navigate("MyPageScreen")
            },
            modifier = Modifier
                .size(52.dp) // 더 큰 버튼
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DesignTokens.OrangePrimary,
                            DesignTokens.OrangeDark
                        )
                    ),
                    shape = CircleShape
                )
                .shadow(
                    DesignTokens.Elevation2,
                    CircleShape,
                    spotColor = DesignTokens.OrangeShadow
                )
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "내 정보",
                tint = DesignTokens.WhiteBackground,
                modifier = Modifier.size(24.dp) // 더 큰 아이콘
            )
        }
    }
}

@Composable
private fun WelcomeCard(
    user: User?,
    onCallClick: () -> Unit
) {
    // 🔧 역할 체크 함수
    val isProtector = user?.role?.lowercase() in listOf("보호자", "protector", "guardian", "caregiver")

    Card(
        shape = RoundedCornerShape(DesignTokens.CornerRadiusLarge),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                DesignTokens.Elevation2,
                RoundedCornerShape(DesignTokens.CornerRadiusLarge),
                spotColor = DesignTokens.CardShadow
            ),
        colors = CardDefaults.cardColors(
            containerColor = DesignTokens.WhiteBackground
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            DesignTokens.WhiteBackground,
                            DesignTokens.OrangeLight.copy(alpha = 0.2f),
                            DesignTokens.OrangeLight.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    // 🔧 보호자인 경우 전체 너비 사용, 환자인 경우 weight 사용
                    modifier = if (isProtector) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.weight(1f)
                    }
                ) {
                    Text(
                        buildAnnotatedString {
                            append("안녕하세요, ")
                            withStyle(
                                style = SpanStyle(
                                    color = DesignTokens.OrangePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(user?.nickname ?: "사용자")
                            }
                            append(" 님")
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        // 🔧 역할에 따른 다른 메시지
                        if (isProtector) {
                            "보호자님, 오늘도 따뜻한 돌봄 감사합니다!"
                        } else {
                            "오늘도 건강한 하루 보내세요!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = DesignTokens.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 🔧 보호자가 아닌 경우에만 전화 버튼 표시
                if (!isProtector) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier
                            .size(64.dp) // 더 큰 터치 영역
                            .background(
                                Color.Transparent, // 투명 배경
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "전화걸기",
                            tint = DesignTokens.OrangePrimary, // 주황색 전화기 아이콘
                            modifier = Modifier.size(32.dp) // 더 큰 아이콘
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMenuSection(navController: NavController) {
    val currentUser = CurrentUser.user // 현재 사용자 정보 가져오기

    Text(
        "빠른 메뉴",
        style = MaterialTheme.typography.headlineSmall, // 더 큰 제목
        fontWeight = FontWeight.Bold,
        color = DesignTokens.TextPrimary,
        modifier = Modifier.padding(bottom = 20.dp) // 더 큰 간격
    )

    val menuItems = listOf(
        QuickMenuItem("길찾기", Icons.Default.Place, DesignTokens.Info, true) {
            navController.navigate("HomeNavigationScreen") // 길찾기 화면으로 이동
        },
        QuickMenuItem("일정관리", Icons.Default.DateRange, DesignTokens.OrangePrimary, true) {
            navController.navigate("schedule")
        },
        QuickMenuItem(
            "환자관리",
            Icons.Default.Face,
            DesignTokens.Success,
            // 🔧 보호자인 경우에만 활성화
            currentUser?.role == "보호자" || currentUser?.role == "PROTECTOR" || currentUser?.role == "protector"
        ) {
            if (currentUser?.role == "보호자" || currentUser?.role == "PROTECTOR" || currentUser?.role == "protector") {
                navController.navigate("patientList")
            }
        },
        QuickMenuItem("통화목록", Icons.Default.History, DesignTokens.Error, true) {
            navController.navigate("CallLogScreen")
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { // 더 큰 간격
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp) // 더 큰 간격
        ) {
            QuickAccessButton(
                menuItem = menuItems[0],
                modifier = Modifier.weight(1f)
            )
            QuickAccessButton(
                menuItem = menuItems[1],
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp) // 더 큰 간격
        ) {
            QuickAccessButton(
                menuItem = menuItems[2],
                modifier = Modifier.weight(1f)
            )
            QuickAccessButton(
                menuItem = menuItems[3],
                modifier = Modifier.weight(1f)
            )
        }
    }
}

data class QuickMenuItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun QuickAccessButton(
    menuItem: QuickMenuItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(DesignTokens.CornerRadius),
        modifier = modifier
            .height(160.dp) // 높이 증가로 텍스트 공간 확보
            .clickable(enabled = menuItem.isEnabled) {
                if (menuItem.isEnabled) {
                    menuItem.onClick()
                } else {
                    // 🔧 비활성화된 버튼 클릭 시 Toast 메시지
                    Toast.makeText(context, "보호자만 사용할 수 있는 기능입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .shadow(
                DesignTokens.Elevation2, // 더 뚜렷한 그림자
                RoundedCornerShape(DesignTokens.CornerRadius),
                spotColor = DesignTokens.CardShadow
            ),
        colors = CardDefaults.cardColors(
            // 🔧 비활성화 상태일 때 색상 변경
            containerColor = if (menuItem.isEnabled) {
                DesignTokens.WhiteBackground
            } else {
                DesignTokens.SurfaceVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // 적절한 패딩
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 아이콘 영역
            Box(
                modifier = Modifier
                    .size(64.dp) // 큰 아이콘 배경
                    .background(
                        // 🔧 비활성화 상태일 때 색상 조정
                        if (menuItem.isEnabled) {
                            menuItem.color.copy(alpha = 0.15f)
                        } else {
                            DesignTokens.TextLight.copy(alpha = 0.1f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = menuItem.icon,
                    contentDescription = menuItem.label,
                    // 🔧 비활성화 상태일 때 아이콘 색상 변경
                    tint = if (menuItem.isEnabled) {
                        menuItem.color
                    } else {
                        DesignTokens.TextLight
                    },
                    modifier = Modifier.size(32.dp) // 큰 아이콘
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 텍스트 영역 - 충분한 공간 확보
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp), // 텍스트를 위한 충분한 높이
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = menuItem.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    // 🔧 비활성화 상태일 때 텍스트 색상 변경
                    color = if (menuItem.isEnabled) {
                        DesignTokens.TextPrimary
                    } else {
                        DesignTokens.TextLight
                    },
                    maxLines = 2, // 최대 2줄까지 허용
                    lineHeight = 20.sp // 줄 간격 설정
                )
            }

            // 🆕 비활성화 상태 표시
            if (!menuItem.isEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "보호자 전용",
                    style = MaterialTheme.typography.labelSmall,
                    color = DesignTokens.TextLight,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun getCurrentTimeGreeting(): String {
    val hour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }

    return when (hour) {
        in 5..11 -> "좋은 아침이에요"
        in 12..17 -> "좋은 오후에요"
        in 18..21 -> "좋은 저녁이에요"
        else -> "안녕하세요"
    }
}