package com.example.domentiacare.ui.screen.patientCare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.domentiacare.data.remote.WebSocketManager
import com.example.domentiacare.data.remote.dto.LocationRequestBody
import com.example.domentiacare.data.remote.dto.Patient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    val context = LocalContext.current
    val patientLatLng = remember { mutableStateOf(LatLng(37.6214975, 127.0673431)) }
    val userLatLng = remember { mutableStateOf<LatLng?>(null) }
    val isLocationPermissionGranted = remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(patientLatLng.value, 16f)
    }
    val patientMarkerState = rememberMarkerState(position = patientLatLng.value)
    val userMarkerState = remember { mutableStateOf<MarkerState?>(null) }

    // FusedLocationProvider 초기화
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 환자의 집 위치 마커 아이콘
    val homeIcon = remember { mutableStateOf<BitmapDescriptor?>(null) }
    val userIcon = remember { mutableStateOf<BitmapDescriptor?>(null) }

    // 위치 권한 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            isLocationPermissionGranted.value = true
            getCurrentUserLocation(fusedLocationClient, context) { location ->
                userLatLng.value = LatLng(location.latitude, location.longitude)
                userMarkerState.value = MarkerState(position = LatLng(location.latitude, location.longitude))
            }
        } else {
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        // 아이콘 로드
        val homeBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.home)
        homeIcon.value = BitmapDescriptorFactory.fromBitmap(homeBitmap)

        // 사용자 위치 아이콘 (기본 마커 또는 커스텀 아이콘)
        userIcon.value = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

        // 위치 권한 확인 및 요청
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationPermission || coarseLocationPermission) {
            isLocationPermissionGranted.value = true
            getCurrentUserLocation(fusedLocationClient, context) { location ->
                userLatLng.value = LatLng(location.latitude, location.longitude)
                userMarkerState.value = MarkerState(position = LatLng(location.latitude, location.longitude))
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // 환자 위치 가져오기
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

                    patientLatLng.value = LatLng(location.latitude, location.longitude)
                    patientMarkerState.position = patientLatLng.value
                    cameraPositionState.move(CameraUpdateFactory.newLatLng(patientLatLng.value))

                    // WebSocket 연결 시작
                    WebSocketManager.connectWebSocket(patient.patientId) { lat, lon ->
                        patientLatLng.value = LatLng(lat, lon)
                        patientMarkerState.position = patientLatLng.value
                    }
                } else {
                    Log.e("위치 없음", "Redis에서 위치를 찾을 수 없음")
                }
            }
            override fun onFailure(call: Call<LocationRequestBody>, t: Throwable) {
                Log.e("네트워크 오류", "초기 위치 요청 실패", t)
            }
        })
    }

    // 리소스 정리
    // 화면 종료 시 구독 해제
    DisposableEffect(Unit) {
        onDispose {
            WebSocketManager.disconnect()
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // 환자 현재 위치 마커
            Marker(
                state = patientMarkerState,
                title = patient.patientName,
                snippet = "환자의 현재 위치입니다."
            )

            // 환자 집 위치 마커
            Marker(
                state = MarkerState(
                    position = LatLng(patient.latitude, patient.longitude)
                ),
                title = "집",
                snippet = "${patient.patientName}의 집 위치입니다.",
                icon = homeIcon.value,
                onClick = { marker ->
                    // 사용자 위치에서 환자 집으로 길찾기
                    userLatLng.value?.let { userLocation ->
                        openNavigation(
                            context = context,
                            startLat = userLocation.latitude,
                            startLng = userLocation.longitude,
                            endLat = patient.latitude,
                            endLng = patient.longitude,
                            destinationName = "${patient.patientName}의 집"
                        )
                    } ?: run {
                        Toast.makeText(context, "사용자 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            )

            // 사용자 현재 위치 마커
            userMarkerState.value?.let { markerState ->
                Marker(
                    state = markerState,
                    title = "내 위치",
                    snippet = "현재 사용자의 위치입니다.",
                    icon = userIcon.value
                )
            }
        }

        // 플로팅 액션 버튼들
        FloatingActionButtons(
            phoneNumber = patient.phone,
            onNavigateToPatient = {
                // 사용자 위치에서 환자 현재 위치로 길찾기
                val userLocation = userLatLng.value
                if (userLocation != null) {
                    openNavigation(
                        context = context,
                        startLat = userLocation.latitude,
                        startLng = userLocation.longitude,
                        endLat = patientLatLng.value.latitude,
                        endLng = patientLatLng.value.longitude,
                        destinationName = "${patient.patientName}의 현재 위치"
                    )
                } else {
                    Toast.makeText(context, "사용자 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * 사용자의 현재 위치를 가져오는 함수
 */
private fun getCurrentUserLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: android.content.Context,
    onLocationReceived: (Location) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    onLocationReceived(it)
                    Log.d("UserLocation", "사용자 위치: ${it.latitude}, ${it.longitude}")
                } ?: run {
                    Log.w("UserLocation", "위치를 가져올 수 없습니다.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UserLocation", "위치 가져오기 실패", exception)
            }
    }
}

/**
 * 길찾기 기능을 실행하는 메서드
 */
fun openNavigation(
    context: android.content.Context,
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
    destinationName: String = "목적지"
) {
    try {
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                    "&origin=$startLat,$startLng" +
                    "&destination=$endLat,$endLng" +
                    "&travelmode=driving"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d("Navigation", "Google Maps로 길찾기 시작: $destinationName")
        } else {
            openNavigationInBrowser(context, startLat, startLng, endLat, endLng, destinationName)
        }
    } catch (e: Exception) {
        Log.e("Navigation", "길찾기 실행 중 오류 발생", e)
        Toast.makeText(context, "길찾기를 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
        openNavigationInBrowser(context, startLat, startLng, endLat, endLng, destinationName)
    }
}

/**
 * 웹 브라우저에서 길찾기를 실행하는 대체 메서드
 */
private fun openNavigationInBrowser(
    context: android.content.Context,
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
    destinationName: String
) {
    try {
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/$startLat,$startLng/$endLat,$endLng"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)

        Toast.makeText(context, "웹에서 길찾기를 시작합니다.", Toast.LENGTH_SHORT).show()
        Log.d("Navigation", "브라우저로 길찾기 시작: $destinationName")
    } catch (e: Exception) {
        Log.e("Navigation", "브라우저 길찾기도 실패", e)
        Toast.makeText(context, "길찾기 기능을 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun FloatingActionButtons(
    phoneNumber: String,
    onNavigateToPatient: () -> Unit
) {
    val context = LocalContext.current

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 환자에게로 길찾기 버튼
            FloatingActionButton(
                onClick = onNavigateToPatient,
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "환자에게로 길찾기"
                )
            }

            // 전화 걸기 버튼
            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(intent)
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                },
                containerColor = Color(0xFFF49000),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "전화 걸기"
                )
            }
        }
    }
}