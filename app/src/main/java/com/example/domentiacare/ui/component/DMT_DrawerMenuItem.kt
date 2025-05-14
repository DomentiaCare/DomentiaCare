package com.example.domentiacare.ui.component

import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


@Composable
fun DMT_DrawerMenuItem(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    containerColor: Color = Color(0xFFF49000), // 기본 주황색
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = containerColor,
            unselectedTextColor = Color.Black,
            selectedTextColor = Color.White
        ),
    )
}