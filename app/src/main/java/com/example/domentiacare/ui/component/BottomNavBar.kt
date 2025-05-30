package com.example.domentiacare.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState


@Composable
fun BottomNavBar(navController: NavController) {

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        containerColor = Color.White
    ) {
        NavigationBarItem(
            selected = currentRoute == "MyPageScreen",
            onClick = { navController.navigate("MyPageScreen")},
            icon = { Icon(Icons.Default.Person, contentDescription = "내정보") },
            label = { Text("내정보") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFfbc271)
            )
        )
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { navController.navigate("home")  },
            icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
            label = { Text("홈") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFfbc271)
            )
        )
        NavigationBarItem(
            selected = currentRoute == "MySettingScreen",
            onClick = {
                navController.navigate("MySettingScreen"){
                    popUpTo("home") {inclusive = false}
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = "설정") },
            label = { Text("설정") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFfbc271)
            )
        )
    }
}