package com.example.domentiacare.data.repository

import com.example.domentiacare.data.local.PatientDao
import com.example.domentiacare.data.model.LocationDto
import com.example.domentiacare.data.model.Patient
import com.example.domentiacare.data.remote.api.PatientApi

class PatientRepository(
    private val dao: PatientDao,
    private val api: PatientApi
) {
    suspend fun getAll(): List<Patient> {
        val remote = api.getAll()
        dao.insertAll(remote)     // 캐싱 전략 선택
        return dao.getAll()
    }
    suspend fun add(patient: Patient) = api.create(patient).also { dao.insert(it) }
    suspend fun update(patient: Patient) = api.update(patient.id, patient).also { dao.update(it) }
    suspend fun delete(patient: Patient) = api.delete(patient.id).also { dao.delete(patient) }

    suspend fun sendLocation(id: Long, lat: Double, lng: Double) {
        api.sendLocation(id, LocationDto(lat, lng))
    }
}