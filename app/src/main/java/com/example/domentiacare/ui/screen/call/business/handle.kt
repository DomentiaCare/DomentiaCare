package com.example.domentiacare.ui.screen.call.business

import android.content.Context
import android.util.Log
import com.example.domentiacare.data.local.SimpleSchedule
import com.example.domentiacare.data.sync.SimpleSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun handleSaveSchedule(
    extractedTitle: String,
    currentUserId: Long,
    selectedYear: String,
    selectedMonth: String,
    selectedDay: String,
    selectedHour: String,
    selectedMinute: String,
    selectedPlace: String,
    context: Context,
    coroutineScope: CoroutineScope,
    onSavingChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    onSuccess: () -> Unit
) {
    if (extractedTitle.isBlank()) {
        onMessageChange("일정 제목을 입력해주세요.")
        return
    }

    if (currentUserId <= 0) {
        onMessageChange("사용자 정보를 확인할 수 없습니다.")
        return
    }

    val selectedDateTime = LocalDateTime.of(
        selectedYear.toInt(),
        selectedMonth.toInt(),
        selectedDay.toInt(),
        selectedHour.toInt(),
        selectedMinute.toInt()
    )

    if (selectedDateTime.isBefore(LocalDateTime.now())) {
        onMessageChange("과거 시간으로는 일정을 생성할 수 없습니다.")
        return
    }

    onSavingChange(true)
    onMessageChange("")

    coroutineScope.launch {
        try {
            val simpleSchedule = SimpleSchedule(
                localId = UUID.randomUUID().toString(),
                userId = currentUserId,
                title = extractedTitle.ifBlank { "Call Schedule" },
                description = "Call recording extracted schedule${if (selectedPlace.isNotEmpty()) " - Location: $selectedPlace" else ""}",
                startDate = selectedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                endDate = selectedDateTime.plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                isAi = true
            )

            val syncManager = SimpleSyncManager.getInstance(context)
            val result = syncManager.saveSchedule(simpleSchedule)

            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    onMessageChange("✅ 일정이 저장되었습니다.")
                    Log.d("CallDetailScreen", "로컬 저장 성공: ${result.getOrNull()?.localId}")
                    onSuccess()
                } else {
                    onMessageChange("❌ 저장 실패: ${result.exceptionOrNull()?.message}")
                    Log.e("CallDetailScreen", "저장 실패", result.exceptionOrNull())
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onMessageChange("❌ 저장 중 오류 발생: ${e.message}")
                Log.e("CallDetailScreen", "저장 예외", e)
            }
        } finally {
            withContext(Dispatchers.Main) {
                onSavingChange(false)
            }
        }
    }
}