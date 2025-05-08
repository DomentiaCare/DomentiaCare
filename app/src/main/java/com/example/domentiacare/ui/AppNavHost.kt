package com.example.domentiacare.ui

import ScheduleDetailScreen
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domentiacare.ui.screen.home.Home
import com.example.domentiacare.ui.screen.patientCare.Patient
import com.example.domentiacare.ui.screen.patientCare.PatientDetailScreen
import com.example.domentiacare.ui.screen.patientCare.PatientList
import com.example.domentiacare.ui.screen.patientCare.PatientLocationScreen
import com.example.domentiacare.ui.screen.schedule.AddScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleScreen
import com.example.domentiacare.ui.screen.schedule.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

    @Composable
    fun AppNavHost() {
        val navController = rememberNavController()
        val scheduleViewModel: ScheduleViewModel = viewModel()

        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer( // ✅ 모든 화면을 감싸도록 이동
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("메뉴", modifier = Modifier.padding(16.dp))
                    NavigationDrawerItem(label = { Text("홈") }, selected = false, onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                        scope.launch { drawerState.close() }
                    })
                    NavigationDrawerItem(label = { Text("일정관리") }, selected = false, onClick = {
                        navController.navigate("schedule")
                        scope.launch { drawerState.close() }
                    })
                    NavigationDrawerItem(label = { Text("환자관리") }, selected = false, onClick = {
                        navController.navigate("patientList")
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
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    Home(navController, drawerState, scope)
                }
                composable("schedule") {
                    ScheduleScreen(navController, drawerState, scope, scheduleViewModel)
                }
                composable("addSchedule/{selectedDate}") { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("selectedDate") ?: ""
                    AddScheduleScreen(navController = navController, drawerState, scope, selectedDate = date, scheduleViewModel)
                }
                composable("patientList") {
                    PatientList(navController, drawerState, scope)
                }
                composable("scheduleDetail/{date}") { backStackEntry ->
                    val dateString = backStackEntry.arguments?.getString("date") ?: ""
                    val date = LocalDate.parse(dateString)
                    ScheduleDetailScreen(navController, drawerState, scope, date, scheduleViewModel)
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
                    PatientDetailScreen(navController, drawerState, scope, Patient(name, age, condition), )
                }

                composable(
                    "PatientLocationScreen/{name}",
                    arguments = listOf(
                        navArgument("name") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    PatientLocationScreen( navController, drawerState, scope, name )
                }
            }
        }
    }


