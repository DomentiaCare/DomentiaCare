package com.example.domentiacare.data.remote.api

import com.example.domentiacare.data.model.Patient
import com.example.domentiacare.data.model.LocationDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.Response

interface PatientApi {
    @GET("api/patients")
    suspend fun getAll(): List<Patient>

    @POST("api/patients")
    suspend fun create(@Body patient: Patient): Patient

    @PUT("api/patients/{id}")
    suspend fun update(@Path("id") id: Long, @Body patient: Patient): Patient

    @DELETE("api/patients/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>

    @POST("api/patients/{id}/locations")
    suspend fun sendLocation(
        @Path("id") id: Long,
        @Body loc: LocationDto
    )
}