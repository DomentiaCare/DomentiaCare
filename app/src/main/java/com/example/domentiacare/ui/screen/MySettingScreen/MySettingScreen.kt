package com.example.domentiacare.ui.screen.MySetting

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun MySettingScreen(
    navController: NavController,
    getAssistantState: () -> Boolean,
    toggleAssistant: () -> Unit
) {
    val context = LocalContext.current
    var isChecked by remember { mutableStateOf(getAssistantState()) }

    // 🆕 상태 변경 감지를 위한 LaunchedEffect 개선
    LaunchedEffect(getAssistantState()) {
        // 진입 시 및 상태 변경 시 동기화
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

        // 🆕 개선된 AI 어시스턴트 설정 Switch - 강제 중지 정보 추가
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isChecked)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "AI 어시스턴트",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 🆕 상태별 상세 설명 추가
                    Text(
                        text = when {
                            isChecked -> "음성 어시스턴트가 활성화되어 있습니다.\n실행 중인 작업이 있다면 즉시 중지됩니다."
                            else -> "AI 어시스턴트가 비활성화되어 있습니다.\n활성화하면 음성 명령을 사용할 수 있습니다."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // 🆕 스위치 개선 - 상태 변경 시 즉시 반영
                Switch(
                    checked = isChecked,
                    onCheckedChange = { newValue ->
                        // 스위치 상태를 먼저 반영
                        isChecked = newValue

                        // 토글 함수 호출 (강제 중지 포함)
                        toggleAssistant()

                        // 토글 후 실제 상태 다시 확인하여 동기화
                        // (약간의 지연을 두고 상태 재확인)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(100) // 0.1초 후 상태 재확인
                            isChecked = getAssistantState()
                        }
                    }
                )
            }
        }

        // 🆕 AI 어시스턴트 사용법 안내 카드 추가
        if (isChecked) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "💡 사용법 안내",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildString {
                            append("• 플로팅 버튼을 눌러 음성 명령을 시작하세요\n")
                            append("• 녹음 중이거나 분석 중일 때 버튼을 누르면 즉시 중지됩니다\n")
                            append("• 영어로 명령하세요: \"Call caregiver\", \"What's today's schedule\" 등\n")
                            append("• 이 스위치를 끄면 실행 중인 모든 작업이 중지됩니다")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 🆕 권한 관련 안내 개선
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🔒 필요한 권한",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append("AI 어시스턴트 사용을 위해 다음 권한이 필요합니다:\n")
                        append("• 마이크 권한 (음성 인식)\n")
                        append("• 전화 권한 (통화 기능)\n")
                        append("• 위치 권한 (위치 찾기)\n\n")
                        append("권한이 거부된 경우 아래 버튼으로 설정에서 허용해주세요.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // ✅ 권한 설정 열기 버튼 개선
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "⚙️ 앱 권한 설정 열기",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "시스템 설정에서 앱 권한을 관리합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // 🆕 화살표 아이콘 추가
                Text(
                    "→",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}