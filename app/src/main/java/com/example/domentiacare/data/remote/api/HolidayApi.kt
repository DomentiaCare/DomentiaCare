package com.example.domentiacare.data.remote.api

import com.example.domentiacare.data.remote.dto.HolidayResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface HolidayApi {
    @GET("B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
    suspend fun getHolidays(
        @Query("ServiceKey") serviceKey: String,
        @Query("solYear") year: Int,
        @Query("solMonth") month: String,
    ): HolidayResponse
}
