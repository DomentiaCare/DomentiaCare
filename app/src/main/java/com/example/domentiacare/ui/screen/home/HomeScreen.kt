package com.example.domentiacare.ui.screen.home

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    var userInfo by remember { mutableStateOf<User?>(null) }
    var aiResponse by remember { mutableStateOf("AI 응답을 기다리고 있어요...") }

    val context = LocalContext.current
    val token = TokenManager.getToken()

    // 🔹 사용자 정보 가져오기
    LaunchedEffect(Unit) {
        token?.let {
            RetrofitClient.authApi.getMyInfo("Bearer $it")
                .enqueue(object : Callback<User> {
                    override fun onResponse(call: Call<User>, response: Response<User>) {
                        if (response.isSuccessful) {
                            userInfo = response.body()
                            Log.d("HomeScreen", "유저 정보 수신 성공: ${userInfo?.nickname}")
                        } else {
                            Log.e("HomeScreen", "유저 정보 실패: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<User>, t: Throwable) {
                        Log.e("HomeScreen", "서버 연결 실패", t)
                    }
                })
        } ?: Log.e("HomeScreen", "토큰 없음")
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = userInfo?.let { "안녕하세요, ${it.nickname}님!" } ?: "로딩 중...",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        val prompt = "Tell me a joke"
                        try {
                            // 📤 프롬프트를 txt 파일로 저장
                            val promptFile = File(context.getExternalFilesDir(null), "prompt.txt")
                            promptFile.writeText(prompt)

                            // ⏳ 3초 후 결과 파일 읽기 시도
                            Handler(Looper.getMainLooper()).postDelayed({
                                try {
                                    val responseFile = File(context.getExternalFilesDir(null), "ai_output.txt")
                                    aiResponse = responseFile.readText()
                                } catch (e: Exception) {
                                    aiResponse = "⚠️ 결과를 읽는 데 실패했어요. Termux 실행 확인!"
                                }
                            }, 3000)

                        } catch (e: Exception) {
                            aiResponse = "⚠️ 프롬프트 저장 실패: ${e.message}"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("AI에게 물어보기")
                }

                Text(
                    text = aiResponse,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray)
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // 🔹 로그아웃 버튼
            Button(
                onClick = {
                    TokenManager.clearToken()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("로그아웃")
            }
        }
    }
}
