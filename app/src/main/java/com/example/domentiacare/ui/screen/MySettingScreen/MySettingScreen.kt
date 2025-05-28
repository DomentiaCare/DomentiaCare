package com.example.domentiacare.ui.screen.MySetting

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MySettingScreen(
    navController: NavController,
    getAssistantState: () -> Boolean,
    toggleAssistant: () -> Unit
) {
    val context = LocalContext.current
    var isChecked by remember { mutableStateOf(getAssistantState()) }

    LaunchedEffect(Unit) {
        // 진입 시 동기화
        isChecked = getAssistantState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "앱 설정",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ✅ AI 어시스턴트 설정 Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AI 어시스턴트",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isChecked) "음성 어시스턴트가 활성화되어 있습니다." else "비활성화된 상태입니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Switch(
                    checked = isChecked,
                    onCheckedChange = {
                        toggleAssistant()
                        isChecked = getAssistantState()
                    }
                )
            }
        }

        // ✅ 권한 설정 열기
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("앱 권한 설정 열기", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "현재 앱의 권한 설정 화면으로 이동합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
