package com.example.domentiacare.data.remote

import com.example.domentiacare.data.remote.api.HolidayApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object HolidayRetrofitClient {
    private const val BASE_URL = "http://apis.data.go.kr/"

    private val client = OkHttpClient.Builder().build()

    val holidayApi: HolidayApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(HolidayApi::class.java)
}

