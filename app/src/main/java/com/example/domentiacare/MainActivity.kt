package com.example.domentiacare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.domentiacare.ui.AppNavHost
import com.example.domentiacare.ui.theme.DomentiaCareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 로그인 화면 스킵 변수
    // true: 로그인 화면 스킵
    // false: 로그인 화면 활성화
    private val IS_DEV_MODE = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DomentiaCareTheme {
                if (IS_DEV_MODE) {
                    //RouteFinderScreen() // ✅ 이 부분 수정
                    AppNavHost() //이 함수 안에 초기 화면과 이동화면들 정의되어있음
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