package com.example.domentiacare.ui

// MainActivity에서 전달받을 알림 데이터
import ScheduleDetailScreen
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domentiacare.NotificationData
import com.example.domentiacare.data.local.CurrentUser
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.model.CallRecordingViewModel
import com.example.domentiacare.data.model.PatientViewModel
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.service.whisper.WhisperScreen
import com.example.domentiacare.ui.component.BottomNavBar
import com.example.domentiacare.ui.screen.MyPage.MyPageScreen
import com.example.domentiacare.ui.screen.MySetting.MySettingScreen
import com.example.domentiacare.ui.screen.call.CallDetailScreen
import com.example.domentiacare.ui.screen.call.CallLogScreen
import com.example.domentiacare.ui.screen.home.Home
import com.example.domentiacare.ui.screen.login.LoginScreen
import com.example.domentiacare.ui.screen.login.RegisterScreen
import com.example.domentiacare.ui.screen.patientCare.PatientAddScheduleScreen
import com.example.domentiacare.ui.screen.patientCare.PatientDetailScreen
import com.example.domentiacare.ui.screen.patientCare.PatientList
import com.example.domentiacare.ui.screen.patientCare.PatientLocationScreen
import com.example.domentiacare.ui.screen.patientCare.ScheduleScreenWrapper
import com.example.domentiacare.ui.screen.schedule.AddScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleViewModel
import com.example.domentiacare.ui.test.TestCalendar
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URLDecoder
import java.time.LocalDate

@Composable
fun AppNavHost(
    notificationData: NotificationData? = null,
    getAssistantState: () -> Boolean = {false},
    toggleAssistant: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scheduleViewModel = remember { ScheduleViewModel(context) }

    // 현재 화면 정보 가져오기
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 바텀바를 숨길 화면들 정의
    val screensWithoutBottomBar = listOf(
        "login",
        "RegisterScreen?email={email}&nickname={nickname}"
    )

    // 현재 화면이 바텀바를 숨겨야 하는지 확인
    val shouldShowBottomBar = !screensWithoutBottomBar.any { route ->
        currentRoute?.startsWith(route.split("?")[0]) == true
    }


    // 🆕 알림에서 온 경우 해당 화면으로 네비게이션
    LaunchedEffect(notificationData) {
        observeNetworkAndSync(context, scheduleViewModel)  // ✅ 네트워크 감지 등록 (한 번만 실행되도록)
        notificationData?.let { data ->
            if (data.fromNotification) {
                Log.d("AppNavHost", "알림에서 온 데이터: ${data.targetScreen}")

                when (data.targetScreen) {
                    "schedule" -> {
                        if (data.scheduleData != null) {
                            // 일정 화면으로 이동하면서 알림 데이터 전달
                            navController.navigate("schedule") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    }
                    "call_record" -> {
                        navController.navigate("CallLogScreen") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                    "location" -> {
                        navController.navigate("patientList") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                    else -> {
                        navController.navigate("home")
                    }
                }
            }
        }
    }

    //jwt 토큰이 없으면 로그인 화면으로 이동
    val token = TokenManager.getToken()
    val inintScreen = if (token == null) {
        "login"
    } else {
        "home"
    }

    // ✅ ModalNavigationDrawer와 TopBar 제거, 깔끔한 Scaffold만 사용
    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = inintScreen,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(navController,
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                            sendFcmTokenAfterLogin()
                            scheduleViewModel.syncFromServerAfterLogin()
                        }
                    }
                )
            }
            composable("home") {
                Home(navController)
            }
            composable("schedule") {
                // 🆕 알림 데이터를 ScheduleScreen에 전달
                ScheduleScreen(
                    navController = navController,
                    viewModel = scheduleViewModel,
                    notificationData = notificationData?.scheduleData
                )
            }
            composable("addSchedule/{selectedDate}") { backStackEntry ->
                val date = backStackEntry.arguments?.getString("selectedDate") ?: ""
                AddScheduleScreen(
                    navController = navController,
                    selectedDate = date,
                    scheduleViewModel
                )
            }
            composable("patientList") {
                PatientList(navController)
            }
            composable("scheduleDetail/{date}") { backStackEntry ->
                val dateString = backStackEntry.arguments?.getString("date") ?: ""
                val date = LocalDate.parse(dateString)
                ScheduleDetailScreen(
                    navController,
                    date,
                    scheduleViewModel
                )
            }

            // 기존 라우트들 유지...
            composable(
                "patientDetail/{patientId}",
                arguments = listOf(
                    navArgument("patientId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId")
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("patientList")
                }
                val viewModel: PatientViewModel = viewModel(parentEntry)
                val patient = viewModel.patientList.find { it.patientId == patientId }
                if (patient != null) {
                    PatientDetailScreen(navController, patient)
                } else {
                    androidx.compose.material3.Text("환자 정보를 찾을 수 없습니다.")
                }
            }

            composable(
                "PatientLocationScreen/{id}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: -1L
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("patientList")
                }
                val viewModel: PatientViewModel = viewModel(parentEntry)
                val patient = viewModel.patientList.find { it.patientId == id }

                if (patient != null) {
                    PatientLocationScreen(navController, patient)
                } else {
                    androidx.compose.material3.Text("환자 정보를 찾을 수 없습니다.")
                }
            }

            composable("MyPageScreen") {
                // ✅ 로그아웃 기능을 MyPageScreen에 전달
                MyPageScreen(
                    navController = navController,
                    onLogout = {
                        navController.navigate("login") {
                            TokenManager.clearToken()
                            CurrentUser.user = null
                            scheduleViewModel.clearSchedulesOnLogout()
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                "RegisterScreen?email={email}&nickname={nickname}",
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType },
                    navArgument("nickname") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                val nickname = backStackEntry.arguments?.getString("nickname") ?: ""
                RegisterScreen(email = email, nickname = nickname, onRegistSuccess ={
                    navController.navigate("home") {
                        popUpTo("RegisterScreen") { inclusive = true }
                        scheduleViewModel.syncFromServerAfterLogin()
                    }
                    sendFcmTokenAfterLogin()
                } )
            }

            composable(
                "CallDetailScreen/{filePath}",
                arguments = listOf(navArgument("filePath") { type = NavType.StringType })
            ) { backStackEntry ->
                val filePath = URLDecoder.decode(backStackEntry.arguments?.getString("filePath") ?: "", "utf-8")
                CallDetailScreen(filePath, navController)
            }
            composable("WhisperScreen"){
                WhisperScreen()
            }

            composable("MySettingScreen") {
                MySettingScreen(
                    navController = navController,
                    getAssistantState = getAssistantState,
                    toggleAssistant = toggleAssistant
                )
            }

            composable("CallLogScreen") {
                val context = LocalContext.current
                val viewModel: CallRecordingViewModel = viewModel()
                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) {
                            viewModel.loadRecordings()
                        } else {
                            Toast
                                .makeText(context, "통화 기록 권한이 필요합니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CALL_LOG
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.loadRecordings()
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    }
                }

                CallLogScreen(
                    viewModel = viewModel,
                    navController = navController,
                    patientId = null
                )
            }

            // 그리고 환자별 통화 녹음 라우트도 추가:
            composable(
                "CallLogScreen/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                val context = LocalContext.current
                val viewModel: CallRecordingViewModel = viewModel()

                // 환자별 통화 녹음 - 권한 불필요 (서버 데이터)
                LaunchedEffect(patientId) {
                    viewModel.loadPatientRecordings(patientId)
                }

                CallLogScreen(
                    patientId = patientId,
                    viewModel = viewModel,
                    navController = navController
                )
            }
            composable("TestCalendar"){
                TestCalendar()
            }
            composable("schedule/{patientId}") { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId")?.toLongOrNull()
                if (patientId != null) {
                    ScheduleScreenWrapper(navController , patientId = patientId)
                }
            }
            composable("addSchedule/{patientId}/{selectedDate}") { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId")?.toLongOrNull()
                val selectedDate = backStackEntry.arguments?.getString("selectedDate") ?: LocalDate.now().toString()

                if (patientId != null) {
                    PatientAddScheduleScreen(
                        navController = navController,
                        patientId = patientId,
                        selectedDate = selectedDate,
                        viewModel = viewModel()
                    )
                }
            }
        }
    }
}

fun sendFcmTokenAfterLogin(){
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token ->
            val request = mapOf("token" to token)
            RetrofitClient.authApi.sendFcmToken(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    Log.d("FCM", "✅ 토큰 서버 전송 성공")
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("FCM", "❌ 토큰 전송 실패", t)
                }
            })
        }
}

val builder = NetworkRequest.Builder()
    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

private var hasSynced = false

fun observeNetworkAndSync(context: Context, viewModel: ScheduleViewModel) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    connectivityManager.registerNetworkCallback(
        builder.build(),
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!hasSynced && TokenManager.getToken() != null) {
                    hasSynced = true
                    // 온라인 전환 시 RoomDB → 서버 동기화
                    viewModel.syncOfflineSchedules()
                    Log.d("NetworkCallback", "✅ Network is available, syncing schedules")
                    viewModel.syncServerSchedules()
                }
            }
            override fun onLost(network: Network) {
                hasSynced = false
                Log.d("NetworkCallback", "❌ 네트워크 끊김")
            }
        }
    )
}