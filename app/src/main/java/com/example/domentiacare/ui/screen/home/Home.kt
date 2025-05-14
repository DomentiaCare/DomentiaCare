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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
fun Home(navController: NavController
) {
    val context = LocalContext.current
    var user by remember { mutableStateOf<User?>(null) }
    LaunchedEffect(Unit) {
        val jwt = TokenManager.getToken()
        Log.d("KakaoLogin", "저장되어있던 JWT: $jwt")
        if (!jwt.isNullOrEmpty()) {
            RetrofitClient.authApi.getMyInfo("Bearer $jwt")
                .enqueue(object : Callback<User> {
                    override fun onResponse(call: Call<User>, response: Response<User>) {
                        if (response.isSuccessful) {
                            user = response.body()
                            CurrentUser.user = user  //회원 정보를 로컬 싱글톤 객체에 저장
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

    ////////////////////////////////////////////////////////////////////////// 전화걸기

    val phoneNumber = "010-1234-4567" // 전화번호를 여기에 입력하세요.

    // 권한 요청 런처 만들기
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 허용되면 즉시 전화 걸기
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 정보 카드
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                        buildAnnotatedString {
                            append("안녕하세요, ")

                            withStyle(style = SpanStyle(color = Color(0xFFF49000))) {
                                append(user?.nickname ?: "사용자")
                            }

                            append(" 님")
                        },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("오늘도 좋은 하루 보내세요!", fontSize = 14.sp, color = Color.Gray)
                    }

                    IconButton(onClick = { /* TODO: 클릭 동작 통화?? */
                        // 현재 권한 상태 확인
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CALL_PHONE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            // 권한 있으면 바로 전화
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$phoneNumber")
                            }
                            context.startActivity(intent)
                        } else {
                            // 권한 없으면 요청
                            launcher.launch(Manifest.permission.CALL_PHONE)
                        }}) {
                        Icon(Icons.Default.Call, contentDescription = "통화")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Quick Access 텍스트
            Row { Spacer(modifier = Modifier.size(24.dp))
                Text("빠른 메뉴", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            // 4. 2x2 버튼 그리드
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickAccessButton("길찾기", Icons.Default.Place, {})
                    QuickAccessButton("일정관리", Icons.Default.DateRange, {navController.navigate("schedule")})
                }
                Spacer(modifier = Modifier.height(40.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickAccessButton("환자관리", Icons.Default.Face, {navController.navigate("patientList")})
                    QuickAccessButton("통화목록", Icons.Default.History, {})
                }
            }
        }
    }



@Composable
fun QuickAccessButton(label: String, icon: ImageVector, onClick: () -> Unit ) {
    Column(
        modifier = Modifier
            .size(160.dp) // 버튼 전체 크기
            .shadow(6.dp, shape = RoundedCornerShape(20.dp)) // ✅ 그림자
            .background(
                color = Color.White, // 연한 파란색
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {onClick() }
            .padding(horizontal = 20.dp, vertical = 25.dp),

        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 아이콘 배경 박스
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFF49000), // 아이콘 진한 파란색
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 텍스트
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

