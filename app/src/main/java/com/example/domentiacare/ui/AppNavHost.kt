package com.example.domentiacare.ui

import ScheduleDetailScreen
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.data.model.CallRecordingViewModel
import com.example.domentiacare.data.model.PatientViewModel
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.service.whisper.WhisperScreen
import com.example.domentiacare.ui.component.BottomNavBar
import com.example.domentiacare.ui.component.DMT_DrawerMenuItem
import com.example.domentiacare.ui.component.TopBar
import com.example.domentiacare.ui.screen.MyPage.MyPageScreen
import com.example.domentiacare.ui.screen.call.CallDetailScreen
import com.example.domentiacare.ui.screen.call.CallLogScreen
import com.example.domentiacare.ui.screen.home.Home
import com.example.domentiacare.ui.screen.login.LoginScreen
import com.example.domentiacare.ui.screen.login.RegisterScreen
import com.example.domentiacare.ui.screen.patientCare.PatientDetailScreen
import com.example.domentiacare.ui.screen.patientCare.PatientList
import com.example.domentiacare.ui.screen.patientCare.PatientLocationScreen
import com.example.domentiacare.ui.screen.schedule.AddScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domentiacare.ui.screen.call.CallLogViewModel
import java.net.URLDecoder

// MainActivity에서 전달받을 알림 데이터
import com.example.domentiacare.NotificationData
import com.example.domentiacare.data.remote.dto.Patient

@Composable
fun AppNavHost(notificationData: NotificationData? = null) {
    val navController = rememberNavController()
    val scheduleViewModel: ScheduleViewModel = viewModel()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 🆕 알림에서 온 경우 해당 화면으로 네비게이션
    LaunchedEffect(notificationData) {
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

    //특정 화면의 이름을 알아내어 메뉴 드래그 비활성화하기
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry.value?.destination?.route
    val isMapScreen = currentDestination?.startsWith("PatientLocationScreen") == true

    //jwt 토큰이 없으면 로그인 화면으로 이동
    val token = TokenManager.getToken()
    val inintScreen = if (token == null) {
        "login"
    } else {
        "home"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isMapScreen,
        drawerContent = {
            ModalDrawerSheet (
                drawerContainerColor = Color.White
            ) {
                Text("메뉴", modifier = Modifier.padding(16.dp))
                DMT_DrawerMenuItem("홈", onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                    scope.launch { drawerState.close() }
                })

                DMT_DrawerMenuItem("일정관리", onClick = {
                    navController.navigate("schedule")
                    scope.launch { drawerState.close() }
                })

                DMT_DrawerMenuItem("환자관리", onClick = {
                    navController.navigate("patientList")
                    scope.launch { drawerState.close() }
                })

                DMT_DrawerMenuItem("로그아웃", onClick = {
                    navController.navigate("login") {
                        TokenManager.clearToken()
                        popUpTo("login") { inclusive = true }
                    }
                    scope.launch { drawerState.close() }
                })
            }
        }
    ) {
        BackHandler(enabled = drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }
        Scaffold(
            topBar = {
                TopBar(title = "DomentiaCare", drawerState = drawerState, scope = scope)
            },
            bottomBar = {
                BottomNavBar(navController)
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
                        Text("환자 정보를 찾을 수 없습니다.")
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
                        Text("환자 정보를 찾을 수 없습니다.")
                    }
                }

                composable("MyPageScreen") {
                    MyPageScreen(navController)
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
                        navController = navController
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