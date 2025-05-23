package com.example.domentiacare.data.model

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domentiacare.data.remote.RetrofitClient
import com.example.domentiacare.data.remote.dto.Patient
import com.example.domentiacare.data.remote.dto.Phone
import kotlinx.coroutines.launch

class PatientViewModel : ViewModel() {
    var patientList by mutableStateOf<List<Patient>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun loadPatients() {
        viewModelScope.launch {
            try {
                patientList = RetrofitClient.authApi.getPatients()
            } catch (e: Exception) {
                // 예외 처리
            }
        }
    }

    fun addPatient(phone: String, onResult: (Boolean) -> Unit) {
        Log.d("PatientViewModel", "addPatient: $phone")
        viewModelScope.launch {
            isLoading = true  // ✅ 시작할 때 로딩 ON
            try {
                val response = RetrofitClient.authApi.addPatients(Phone(phone))
                if (response.isSuccessful) {
                    onResult(response.isSuccessful)
                }
            } catch (e: Exception) {
                // 예외 처리
                Log.e("PatientViewModel", "Error adding patient: ${e.message}")
                onResult(false)
            }
            finally {
                isLoading = false  // ✅ 끝나면 무조건 로딩 OFF
            }
        }
    }
}
