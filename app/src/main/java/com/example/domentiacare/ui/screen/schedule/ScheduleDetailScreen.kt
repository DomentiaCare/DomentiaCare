
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.domentiacare.ui.screen.schedule.ScheduleViewModel
import java.time.LocalDate

@Composable
fun ScheduleDetailScreen(
    navController: NavController,
    date: LocalDate,
    viewModel: ScheduleViewModel
) {
    val schedulesForDate = viewModel.getSchedulesForDate(date)
    val schedulesByHour = schedulesForDate.groupBy {
        it.startDate.substring(11, 13)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            for (hour in 0..23) {
                val hourKey = String.format("%02d", hour)
                val schedulesAtHour = schedulesByHour[hourKey] ?: emptyList()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    // 시간 텍스트 + 실선
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$hourKey:00",
                            modifier = Modifier.width(64.dp),
                            color = Color.DarkGray
                        )
                        Canvas(
                            modifier = Modifier
                                .height(1.dp)
                                .weight(1f)
                        ) {
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 2f
                            )
                        }
                    }

                    // 일정이 있다면 실선 아래에 표시
                    schedulesAtHour.forEach { schedule ->
                        Box(
                            modifier = Modifier
                                .padding(start = 72.dp, top = 6.dp) // 시간 텍스트만큼 들여쓰기
                                .background(Color(0xFFBBDEFB), shape = MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📌 ${schedule.startDate.substring(11, 16)} - ${schedule.title}",
                                color = Color.Black)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { navController.navigate("addSchedule/${date}") { launchSingleTop = true } },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "일정 추가")
        }
    }
}
