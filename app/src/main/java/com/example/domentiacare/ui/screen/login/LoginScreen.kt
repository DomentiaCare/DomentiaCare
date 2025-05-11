package com.example.domentiacare.ui.screen.login

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.KakaoLoginResponse
import com.example.domentiacare.data.remote.dto.KakaoTokenRequest
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess:() -> Unit
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


            val request = KakaoTokenRequest(token.accessToken)

            RetrofitClient.authApi.kakaoLogin(request).enqueue(object : Callback<KakaoLoginResponse> {
                override fun onResponse(call: Call<KakaoLoginResponse>, response: Response<KakaoLoginResponse>) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        Log.d("KakaoLogin", "서버 로그인 성공: ${result?.user?.nickname}")
                        Log.d("KakaoLogin", "서버 로그인 성공 jwt: ${result?.jwt}")
                        // JWT 저장 및 다음 화면 이동
                        result?.jwt?.let { jwt ->
                            TokenManager.saveToken(jwt) // 🔐 jwt를 메모리에 저장하여 앱을 다시 켤때 자동 로그인
                            CurrentUser.user = result?.user  //회원 정보를 로컬 싱글톤 객체에 저장
                            Log.d("KakaoLogin", "JWT 저장 완료: $jwt")
                            
                            //매개변수 함수
                            onLoginSuccess()
                        }
                    } else if (response.code() == 404) {
                        val errorBody = response.errorBody()?.string()
                        val json = JSONObject(errorBody)
                        val email = json.getString("email")
                        val nickname = json.getString("nickname")
                        navController.navigate("RegisterScreen?email=$email&nickname=$nickname") {
                            popUpTo("login") { inclusive = true }
                        }
                    }else {
                        Log.e("KakaoLogin", "서버 로그인 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<KakaoLoginResponse>, t: Throwable) {
                    Log.e("KakaoLogin", "서버 연결 실패", t)
                }
            })
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

