package com.example.domentiacare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.remote.repository.HolidayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: HolidayRepository
) : ViewModel() {

    var holidays by mutableStateOf<Map<LocalDate, String>>(emptyMap())
        private set

    fun loadHolidays(year: Int, month: Int) {
        viewModelScope.launch {
            holidays = repository.getHolidays(year, month)
        }
    }
}

