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
import com.example.domentiacare.network.RecordResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientViewModel : ViewModel() {
    var patientList by mutableStateOf<List<Patient>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    // 환자별 녹음 관련 StateFlow 추가
    private val _patientRecords = MutableStateFlow<List<RecordResponse>>(emptyList())
    val patientRecords: StateFlow<List<RecordResponse>> = _patientRecords.asStateFlow()

    private val _recordsLoading = MutableStateFlow(false)
    val recordsLoading: StateFlow<Boolean> = _recordsLoading.asStateFlow()

    private val _recordsError = MutableStateFlow<String?>(null)
    val recordsError: StateFlow<String?> = _recordsError.asStateFlow()

    fun loadPatients() {
        viewModelScope.launch {
            try {
                patientList = RetrofitClient.authApi.getPatients()
                Log.d("PatientViewModel", "Patients loaded successfully: ${patientList.size} patients")
            } catch (e: Exception) {
                // 예외 처리
                Log.e("PatientViewModel", "Error loading patients: ${e.message}")
            }
        }
    }

    /**
     * 특정 환자의 통화 녹음 목록을 가져옵니다
     */
    fun getPatientsRecords(patientId: String) {
        viewModelScope.launch {
            try {
                _recordsLoading.value = true
                _recordsError.value = null

                Log.d("PatientViewModel", "환자 녹음 로딩 시작: $patientId")

                val response = RetrofitClient.authApi.getPatientRecords(patientId)

                if (response.isSuccessful) {
                    val records = response.body() ?: emptyList()
                    _patientRecords.value = records
                    Log.d("PatientViewModel", "환자 녹음 로딩 성공: ${records.size}개")
                } else {
                    val errorMsg = "서버 오류: ${response.code()}"
                    _recordsError.value = errorMsg
                    Log.e("PatientViewModel", errorMsg)
                }

            } catch (e: Exception) {
                val errorMsg = "네트워크 오류: ${e.message}"
                _recordsError.value = errorMsg
                Log.e("PatientViewModel", "환자 녹음 로딩 실패", e)
            } finally {
                _recordsLoading.value = false
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
