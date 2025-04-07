package com.example.domentiacare.ui.screen.login

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // ✅ 여기에 callback 정의!
    val kakaoCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e("KakaoLogin", "로그인 실패", error)
            Toast.makeText(context, "로그인 실패", Toast.LENGTH_SHORT).show()
        } else if (token != null) {
            Log.d("KakaoLogin", "로그인 성공 - token: ${token.accessToken}")
            Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
            onLoginSuccess(token.accessToken)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (activity == null) return@Button

                    // ✅ callback은 위에서 선언한 변수 사용
                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                        UserApiClient.instance.loginWithKakaoTalk(activity, callback = kakaoCallback)
                    } else {
                        UserApiClient.instance.loginWithKakaoAccount(activity, callback = kakaoCallback)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFAE100),
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("카카오로 로그인")
            }
        }
    }
}

