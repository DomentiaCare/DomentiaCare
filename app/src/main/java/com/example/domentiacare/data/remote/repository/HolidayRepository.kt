package com.example.domentiacare.data.remote.repository



import com.example.domentiacare.data.remote.HolidayRetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HolidayRepository {
    private val api = HolidayRetrofitClient.holidayApi

    suspend fun getHolidays(year: Int, month: Int): Map<LocalDate, String> {
        return try {
            val response = api.getHolidays(
                serviceKey = "tmJtQWmquLnWY7NBb6ZZrcKN8/zsBSPne7uuOZPhNgUdmqv9/FXZtfGK04yCXZBrHdkQxBzEkZzEGDLXcT9BXA==", // 🔑 여기에 인증키 넣기
                year = year,
                month = String.format("%02d", month)
            )

            response.response.body.items.item?.associate {
                val date = LocalDate.parse(
                    it.locdate.toString(),
                    DateTimeFormatter.ofPattern("yyyyMMdd")
                )
                date to it.dateName
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
