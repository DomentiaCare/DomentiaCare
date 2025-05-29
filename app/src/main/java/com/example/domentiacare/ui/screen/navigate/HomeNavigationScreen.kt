package com.example.domentiacare.ui.screen.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.navigation.NavController
import com.example.domentiacare.R
import com.example.domentiacare.data.local.CurrentUser
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@Composable
fun HomeNavigationScreen(navController: NavController,
                         flag: String? = null) {
    val context = LocalContext.current
    val currentUser = CurrentUser.user

    // 기본 집 주소 (실제로는 사용자 설정에서 가져와야 함)
    val homeAddress = "서울특별시 강남구 테헤란로 123"
    val homeLatLng = remember { mutableStateOf(LatLng(37.5666102, 126.9783881)) } // 기본 집 위치
    val userLatLng = remember { mutableStateOf<LatLng?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val isLocationPermissionGranted = remember { mutableStateOf(false) }
    val isMapReady = remember { mutableStateOf(false) }

    // 지도 카메라 상태
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(homeLatLng.value, 15f)
    }

    val homeMarkerState = rememberMarkerState(position = homeLatLng.value)
    val userMarkerState = remember { mutableStateOf<MarkerState?>(null) }

    // 위치 서비스 클라이언트
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 마커 아이콘
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

                // 지도가 준비되었을 때만 카메라 이동
                if (isMapReady.value) {
                    updateCameraToShowBothLocations(cameraPositionState, userLatLng.value!!, homeLatLng.value)
                }

                isLoading.value = false
            }
        } else {
            isLoading.value = false
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    // 초기화
    LaunchedEffect(Unit) {

        // 위치 권한 확인
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

                // 지도가 준비되었을 때만 카메라 이동
                if (isMapReady.value) {
                    updateCameraToShowBothLocations(cameraPositionState, userLatLng.value!!, homeLatLng.value)
                }

                isLoading.value = false
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        Log.d("HomeNavigationScreen", "LaunchedEffect 진입")

        when {
            flag == null -> {
                Log.d("Navigation", "null 값")
                // null 처리
            }
            flag == "home" -> {
                // 🎯 자동 길찾기 실행
                // 사용자 위치가 확인될 때까지 잠시 대기 후 실행
                kotlinx.coroutines.delay(2000) // 2초 대기 (위치 정보 로딩 시간)

                // 현재 위치와 집 위치 정보로 자동 길찾기 실행
                val currentUserLocation = userLatLng.value
                val homeLocation = homeLatLng.value

                if (currentUserLocation != null) {
                    Log.d("HomeNavigationScreen", "자동 구글 맵 길찾기 실행")
                    Log.d("HomeNavigationScreen", "출발지: ${currentUserLocation.latitude}, ${currentUserLocation.longitude}")
                    Log.d("HomeNavigationScreen", "목적지: ${homeLocation.latitude}, ${homeLocation.longitude}")

                    // 구글 맵 길찾기 자동 실행
                    openGoogleMapsNavigation(
                        context = context,
                        startLat = currentUserLocation.latitude,
                        startLng = currentUserLocation.longitude,
                        endLat = homeLocation.latitude,
                        endLng = homeLocation.longitude,
                        destinationName = "집"
                    )
                } else {
                    Log.w("HomeNavigationScreen", "사용자 위치 정보가 없어 자동 길찾기를 실행할 수 없습니다")
                    // 기본 위치(서울 시청)에서 집으로 길찾기
                    val defaultLocation = LatLng(37.5665, 126.9780) // 서울 시청

                    Log.d("HomeNavigationScreen", "기본 위치에서 집으로 길찾기 실행")
                    openGoogleMapsNavigation(
                        context = context,
                        startLat = defaultLocation.latitude,
                        startLng = defaultLocation.longitude,
                        endLat = homeLocation.latitude,
                        endLng = homeLocation.longitude,
                        destinationName = "집"
                    )
                }
            }
            flag.isDigitsOnly() -> { // 환자 ID 받는 곳
                // 숫자인지 확인
                val number = flag.toIntOrNull()
                Log.d("Navigation", "숫자 값: $number")
                // 환자 ID로 환자 위치

            }
            else -> {
                Log.d("Navigation", "문자열 값: $flag")

            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF4E6),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 상단 헤더
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .background(
                                    Color(0xFFF5F5F5),
                                    RoundedCornerShape(12.dp)
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "뒤로 가기",
                                tint = Color(0xFF2D2D2D)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "집",
                                tint = Color(0xFFF49000),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "집으로 가기",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D2D2D)
                            )
                        }

                        // 공간 맞추기용 빈 박스
                        Box(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "${currentUser?.nickname ?: "사용자"}님, 안전하게 집으로 가세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 지도 영역
            if (isLoading.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFF49000),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "현재 위치를 찾고 있습니다...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF757575)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapLoaded = {
                            isMapReady.value = true
                            // 지도가 로드된 후에 아이콘 설정
                            try {
                                val homeBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.home)
                                homeIcon.value = BitmapDescriptorFactory.fromBitmap(homeBitmap)
                            } catch (e: Exception) {
                                homeIcon.value = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            }
                            userIcon.value = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)

                            // 사용자 위치가 있으면 카메라 이동
                            userLatLng.value?.let { userLocation ->
                                updateCameraToShowBothLocations(cameraPositionState, userLocation, homeLatLng.value)
                            }
                        }
                    ) {
                        // 집 마커
                        if (homeIcon.value != null) {
                            Marker(
                                state = homeMarkerState,
                                title = "우리 집",
                                snippet = "집 주소: $homeAddress",
                                icon = homeIcon.value,
                                onClick = { marker ->
                                    // 집 마커 클릭 시 길찾기 시작
                                    userLatLng.value?.let { userLocation ->
                                        openGoogleMapsNavigation(
                                            context = context,
                                            startLat = userLocation.latitude,
                                            startLng = userLocation.longitude,
                                            endLat = homeLatLng.value.latitude,
                                            endLng = homeLatLng.value.longitude,
                                            destinationName = "집"
                                        )
                                    } ?: run {
                                        Toast.makeText(context, "현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    true
                                }
                            )
                        } else {
                            // 아이콘 로딩 중일 때는 기본 마커
                            Marker(
                                state = homeMarkerState,
                                title = "우리 집",
                                snippet = "집 주소: $homeAddress",
                                onClick = { marker ->
                                    userLatLng.value?.let { userLocation ->
                                        openGoogleMapsNavigation(
                                            context = context,
                                            startLat = userLocation.latitude,
                                            startLng = userLocation.longitude,
                                            endLat = homeLatLng.value.latitude,
                                            endLng = homeLatLng.value.longitude,
                                            destinationName = "집"
                                        )
                                    } ?: run {
                                        Toast.makeText(context, "현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    true
                                }
                            )
                        }

                        // 현재 위치 마커
                        userMarkerState.value?.let { markerState ->
                            if (userIcon.value != null) {
                                Marker(
                                    state = markerState,
                                    title = "현재 위치",
                                    snippet = "여기에 계십니다",
                                    icon = userIcon.value
                                )
                            } else {
                                Marker(
                                    state = markerState,
                                    title = "현재 위치",
                                    snippet = "여기에 계십니다"
                                )
                            }
                        }
                    }
                }
            }

            // 하단 버튼 영역
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 구글 맵으로 길찾기 버튼
                    Button(
                        onClick = {
                            userLatLng.value?.let { userLocation ->
                                openGoogleMapsNavigation(
                                    context = context,
                                    startLat = userLocation.latitude,
                                    startLng = userLocation.longitude,
                                    endLat = homeLatLng.value.latitude,
                                    endLng = homeLatLng.value.longitude,
                                    destinationName = "집"
                                )
                            } ?: run {
                                Toast.makeText(context, "현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF49000)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = "길찾기",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "구글 맵으로 집까지 길찾기",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }



                    // 긴급 연락 버튼
                    OutlinedButton(
                        onClick = {
                            val phoneNumber = "010-1234-5678" // 보호자 번호
                            val intent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$phoneNumber")
                            }
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                                == PackageManager.PERMISSION_GRANTED) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "전화 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE53E3E), Color(0xFFFF6B6B))
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "긴급 연락",
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "보호자에게 전화하기",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53E3E),
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 사용자의 현재 위치를 가져오는 함수
 */
private fun getCurrentUserLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
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
 * 구글 맵 길찾기 실행
 */
fun openGoogleMapsNavigation(
    context: Context,
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
                    "&travelmode=walking"  // 걸어서 이동
        )

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Log.d("Navigation", "Google Maps로 길찾기 시작: $destinationName")
        } else {
            // 구글 맵이 없으면 브라우저로 열기
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/$startLat,$startLng/$endLat,$endLng")
            )
            context.startActivity(browserIntent)
            Toast.makeText(context, "웹에서 길찾기를 시작합니다.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("Navigation", "길찾기 실행 중 오류 발생", e)
        Toast.makeText(context, "길찾기를 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 카메라를 두 위치를 모두 포함하도록 이동하는 함수
 */
fun updateCameraToShowBothLocations(
    cameraPositionState: CameraPositionState,
    userLocation: LatLng,
    homeLocation: LatLng
) {
    try {
        val bounds = LatLngBounds.builder()
            .include(userLocation)
            .include(homeLocation)
            .build()

        // 패딩을 추가하여 마커들이 화면 가장자리에 붙지 않도록 함
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 200)
        cameraPositionState.move(cameraUpdate)
    } catch (e: Exception) {
        Log.e("CameraUpdate", "카메라 이동 중 오류 발생", e)
        // 실패 시 기본 위치로 이동
        try {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
        } catch (e2: Exception) {
            Log.e("CameraUpdate", "기본 카메라 이동도 실패", e2)
        }
    }
}