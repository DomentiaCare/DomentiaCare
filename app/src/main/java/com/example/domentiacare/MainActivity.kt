package com.example.domentiacare

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.ui.screen.home.HomeScreen
import com.example.domentiacare.ui.screen.login.LoginScreen
import com.example.domentiacare.ui.screen.navigate.NavigateScreen
import com.example.domentiacare.ui.theme.DomentiaCareTheme

class MainActivity : ComponentActivity() {

    private val IS_DEV_MODE = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DomentiaCareTheme {
                if (IS_DEV_MODE) {
                    NavigateScreen()
                } else {
                    // ✅ remember 상태로 jwtToken 관리
                    var jwtToken by remember { mutableStateOf(TokenManager.getToken()) }

                    if (jwtToken != null) {
                        // ✅ 토큰이 있다면 로그인된 상태 → 홈 화면으로 이동
                        Log.d("KakaoLogin", "저장된 JWT: $jwtToken")
                        HomeScreen(
                            onLogout = {
                                jwtToken = null // ✅ recomposition 유도
                            }
                        )
                    } else {
                        // ✅ 토큰이 없다면 로그인 화면
                        LoginScreen(
                            onLoginSuccess = { accessToken ->
                                // 로그인 후 처리 (예: navigate or recomposition)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DomentiaCareTheme {
        Greeting("Android")
    }
}