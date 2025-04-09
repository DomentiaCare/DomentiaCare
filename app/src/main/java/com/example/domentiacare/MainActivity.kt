package com.example.domentiacare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.domentiacare.ui.screen.login.LoginScreen
import com.example.domentiacare.ui.screen.navigate.NavigateScreen
import com.example.domentiacare.ui.theme.DomentiaCareTheme

class MainActivity : ComponentActivity() {

    private val IS_DEV_MODE = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DomentiaCareTheme {
                if (IS_DEV_MODE) {
                    NavigateScreen()
                } else {
                    LoginScreen { accessToken ->
                        // TODO: 카카오 로그인 성공 후 처리
                        println("로그인 성공! accessToken: $accessToken")
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