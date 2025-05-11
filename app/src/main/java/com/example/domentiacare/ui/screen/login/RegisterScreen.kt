package com.example.domentiacare.ui.screen.login

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.KakaoLoginResponse
import com.example.domentiacare.data.remote.dto.RegisterUserRequest
import com.example.domentiacare.webView.AddressSearchActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun RegisterScreen(
    email: String,
    nickname: String,
    onRegistSuccess: () -> Unit
) {
    val context = LocalContext.current

    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var addressDetail1 by remember { mutableStateOf("") }
    var addressDetail2 by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    // 주소 검색을 위한 launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            address = data?.getStringExtra("zonecode") ?: ""
            addressDetail1 = data?.getStringExtra("roadAddress") ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("회원가입", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

        TextFieldWithIcon(value = email, onValueChange = {}, label = "이메일", icon = Icons.Default.Email, readOnly = true)

        TextFieldWithIcon(value = nickname, onValueChange = {}, label = "이름", icon = Icons.Default.Person, readOnly = true)

        TextFieldWithIcon(value = phone, onValueChange = { phone = it }, label = "휴대폰 번호", icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextFieldWithIcon(
                value = address,
                onValueChange = { address = it },
                label = "주소",
                icon = Icons.Default.Home,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val intent = Intent(context, AddressSearchActivity::class.java)
                launcher.launch(intent)}) {
                Text("주소찾기")
            }
        }

        TextFieldWithIcon(value = addressDetail1, onValueChange = { addressDetail1 = it }, label = "상세주소 1", icon = Icons.Default.EditLocation)
        TextFieldWithIcon(value = addressDetail2, onValueChange = { addressDetail2 = it }, label = "상세주소 2", icon = Icons.Default.EditLocationAlt)
        TextFieldWithIcon(value = birthDate, onValueChange = { birthDate = it }, label = "생년월일 (YYYY-MM-DD)", icon = Icons.Default.DateRange, keyboardType = KeyboardType.Number)

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("성별:")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = gender == "남자", onClick = { gender = "남자" })
                Text("남자")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = gender == "여자", onClick = { gender = "여자" })
                Text("여자")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("회원 유형:")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = role == "환자", onClick = { role = "환자" })
                Text("환자")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = role == "보호자", onClick = { role = "보호자" })
                Text("보호자")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val request = RegisterUserRequest(
                    email = email,
                    nickname = nickname,
                    phone = phone,
                    gender = gender,
                    birthDate = birthDate,
                    address = address ,
                    addressDetail1 = addressDetail1,
                    addressDetail2 = addressDetail2,
                    role = role
                )

                RetrofitClient.authApi.registerUser(request).enqueue(object : Callback<KakaoLoginResponse> {
                    override fun onResponse(call: Call<KakaoLoginResponse>, response: Response<KakaoLoginResponse>) {
                        if (response.isSuccessful) {
                            val jwt = response.body()?.jwt
                            if (jwt != null) {
                                TokenManager.saveToken(jwt) // 🔐 jwt를 메모리에 저장하여 앱을 다시 켤때 자동 로그인
                                CurrentUser.user = response.body()?.user  //회원 정보를 로컬 싱글톤 객체에 저장
                                Log.d("KakaoLogin", "JWT 저장 완료: $jwt")

                                //매개변수 함수
                                onRegistSuccess()
                                Toast.makeText(context, "회원가입 성공", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "회원가입 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<KakaoLoginResponse>, t: Throwable) {
                        Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("회원가입")
        }
    }
}

@Composable
fun TextFieldWithIcon(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = modifier,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType)
    )
}
