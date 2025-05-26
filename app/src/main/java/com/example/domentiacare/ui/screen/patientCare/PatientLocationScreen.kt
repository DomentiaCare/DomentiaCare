package com.example.domentiacare.ui.screen.patientCare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.domentiacare.R
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.connectWebSocket
import com.example.domentiacare.data.remote.dto.LocationRequestBody
import com.example.domentiacare.data.remote.dto.Patient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun PatientLocationScreen(
    navController: NavController,
    patient : Patient
) {
    val patientLatLng = remember { mutableStateOf(LatLng(37.5828483, 127.0105811)) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(patientLatLng.value, 16f)
    }
    val markerState = rememberMarkerState(position = patientLatLng.value)

    // 환자의 집 위치 마커 아이콘
    val context = LocalContext.current
    val homeIcon = remember {
        mutableStateOf<BitmapDescriptor?>(null)
    }

    LaunchedEffect(Unit) {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.home)
        homeIcon.value = BitmapDescriptorFactory.fromBitmap(bitmap)
        Log.d("초기 위치 요청", "환자 ID: ${patient.patientId}, ${patient.latitude}, ${patient.longitude}")
        RetrofitClient.authApi.getCurrentLocation(patient.patientId).enqueue(object :
            Callback<LocationRequestBody> {
            override fun onResponse(
                call: Call<LocationRequestBody>,
                response: Response<LocationRequestBody>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val location = response.body()!!
                    Log.d("초기 위치", "lat=${location.latitude}, lon=${location.longitude}")
                    // 지도 초기화
                    // ✅ 상태변수 업데이트 (지도 초기화)
                    patientLatLng.value = LatLng(location.latitude, location.longitude)
                    markerState.position = patientLatLng.value
                    cameraPositionState.move(CameraUpdateFactory.newLatLng(patientLatLng.value))
                    // ✅ WebSocket 연결 시작
                    connectWebSocket(patient.patientId) { lat, lon ->
                        patientLatLng.value = LatLng(lat, lon)
                        markerState.position = patientLatLng.value
                    }
                } else {
                    Log.e("위치 없음", "Redis에서 위치를 찾을 수 없음")
                    // "위치 없음" 메시지 표시
                }
            }
            override fun onFailure(call: Call<LocationRequestBody>, t: Throwable) {
                Log.e("네트워크 오류", "초기 위치 요청 실패", t)
            }
        })

        }
//    val patientLatLng = LatLng(37.58284829999999, 127.0105811)


    // ✅ 카메라 포지션 상태 초기화
//    val cameraPositionState = rememberCameraPositionState {
//        position = CameraPosition.fromLatLngZoom(patientLatLng.value, 16f)
//    }

    // ✅ 마커 상태 초기화


    Box(
        modifier = Modifier.fillMaxSize()
        //.padding(innerPadding)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = markerState,
                title = patient.patientName,
                snippet = "환자의 현재 위치입니다."
            )
            Marker(
                state = MarkerState(
                    position = LatLng(patient.latitude, patient.longitude)
                ),
                title = "집",
                snippet = "${patient.patientName} 의 집 위치입니다.",
                icon = homeIcon.value
            )
        }
        CallFloatingButton(phoneNumber = patient.phone) // ← 여기에 추가
    }
}

@Composable
fun CallFloatingButton(phoneNumber: String) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 허용되면 즉시 전화 걸기
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${phoneNumber}")
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, bottom = 20.dp), // ← 살짝 오른쪽 위로 이동
        contentAlignment = Alignment.BottomStart
    ) {
        FloatingActionButton(
            onClick = { /* TODO: 클릭 동작 통화?? */
                // 현재 권한 상태 확인
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 권한 있으면 바로 전화
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:${phoneNumber}")
                    }
                    context.startActivity(intent)
                } else {
                    // 권한 없으면 요청
                    launcher.launch(Manifest.permission.CALL_PHONE)
                }},
            containerColor = Color(0xFFF49000),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "전화 걸기"
            )
        }
    }
}
