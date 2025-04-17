package com.example.domentiacare.data.remote.dto

data class HolidayResponse(
    val response: ResponseBody
)

data class ResponseBody(
    val body: HolidayBody
)

data class HolidayBody(
    val items: HolidayItems
)

data class HolidayItems(
    val item: List<HolidayItem>?
)

data class HolidayItem(
    val dateName: String,
    val locdate: Long,
    val isHoliday: String
)

