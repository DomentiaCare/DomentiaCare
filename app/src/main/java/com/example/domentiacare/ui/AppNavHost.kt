package com.example.domentiacare.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.domentiacare.ui.screen.schedule.AddScheduleScreen
import com.example.domentiacare.ui.screen.home.Home
import com.example.domentiacare.ui.screen.patientCare.PatientList
import com.example.domentiacare.ui.screen.schedule.ScheduleScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { Home(navController) }
        composable("schedule") { ScheduleScreen(navController) }
        composable("addSchedule/{selectedDate}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("selectedDate") ?: ""
            AddScheduleScreen(navController = navController, selectedDate = date)
        }
        composable("patientList") { PatientList(navController) }
    }
}
