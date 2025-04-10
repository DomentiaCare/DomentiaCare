package com.example.domentiacare.ui.screen.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun HomeScreen() {
    var userInfo by remember { mutableStateOf<User?>(null) }
    val token = TokenManager.getToken()
    Log.d("HomeScreen", "보내는 토큰: Bearer $token")
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
                .fillMaxSize()
        ) {
            Text(
                text = userInfo?.let { "안녕하세요, ${it.nickname}님!" } ?: "로딩 중...",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}