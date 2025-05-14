package com.example.domentiacare.ui

import ScheduleDetailScreen
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domentiacare.data.local.TokenManager
import com.example.domentiacare.ui.component.BottomNavBar
import com.example.domentiacare.ui.component.TopBar
import com.example.domentiacare.ui.screen.MyPage.MyPageScreen
import com.example.domentiacare.ui.screen.call.CallDetailScreen
import com.example.domentiacare.ui.screen.call.CallLogScreen
import com.example.domentiacare.ui.screen.home.Home
import com.example.domentiacare.ui.screen.login.LoginScreen
import com.example.domentiacare.ui.screen.login.RegisterScreen
import com.example.domentiacare.ui.screen.patientCare.Patient
import com.example.domentiacare.ui.screen.patientCare.PatientDetailScreen
import com.example.domentiacare.ui.screen.patientCare.PatientList
import com.example.domentiacare.ui.screen.patientCare.PatientLocationScreen
import com.example.domentiacare.ui.screen.schedule.AddScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.domentiacare.ui.component.DMT_DrawerMenuItem

    @Composable
    fun AppNavHost() {
        val navController = rememberNavController()
        val scheduleViewModel: ScheduleViewModel = viewModel()

        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

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

        ModalNavigationDrawer( // ✅ 모든 화면을 감싸도록 이동
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
                    TopBar(title = "DomenticaCare", drawerState = drawerState, scope = scope)
                },
                bottomBar = {
                    BottomNavBar(navController)
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = inintScreen, //home
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("login") {
                        LoginScreen(navController,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        Home(navController)
                    }
                    composable("schedule") {
                        ScheduleScreen(navController, scheduleViewModel)
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

                    composable(
                        "patientDetail/{name}/{age}/{condition}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("age") { type = NavType.IntType },
                            navArgument("condition") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: ""
                        val age = backStackEntry.arguments?.getInt("age") ?: 0
                        val condition = backStackEntry.arguments?.getString("condition") ?: ""
                        PatientDetailScreen(
                            navController,
                            Patient(name, age, condition),
                        )
                    }

                    composable(
                        "PatientLocationScreen/{name}",
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("name") ?: ""
                        PatientLocationScreen(navController, name)
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
                        } )
                    }
                    composable("CallLogScreen"){
                        CallLogScreen(navController)
                    }
                    composable("CallDetailScreen"){
                        CallDetailScreen(navController)
                    }
                }
            }
        }
    }

