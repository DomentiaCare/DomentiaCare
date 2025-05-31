package com.example.domentiacare.ui.screen.call.utils

import android.util.Log
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

object DateTimeParser {

    fun parseLlamaScheduleResponseFull(response: String): Quintuple<String, String, String, String, String> {
        val summaryRegex = Regex("""Summary:\s*(.+)""")
        val scheduleRegex = Regex("""Schedule:\s*(\{[\s\S]*\})""")

        val summary = summaryRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""
        val jsonString = scheduleRegex.find(response)?.groupValues?.get(1)?.trim() ?: "{}"

        Log.d("parseLlama", "Llama response: $response")
        Log.d("parseLlama", "Parsed summary: $summary")
        Log.d("parseLlama", "Parsed jsonString: $jsonString")

        val json = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            Log.d("parseLlama", "Invalid JSON: $jsonString (${e.message})")
            JSONObject()
        }

        val dateRaw = json.optString("date").trim()
        val timeRaw = json.optString("time").trim()
        val place = json.optString("place").trim()

        Log.d("parseLlama", "Parsed date: $dateRaw, timeRaw: $timeRaw, place: $place")

        val dateString = parseDateSmart(dateRaw)
        val (hour, minute) = parseTimeSmart(timeRaw)

        Log.d("parseLlama", "Final hour: $hour, minute: $minute")

        return Quintuple(summary, dateString, hour, minute, place)
    }

    fun parseDateSmart(dateRaw: String): String {
        val lower = dateRaw.trim().lowercase(Locale.US)
        val today = LocalDate.now()
        val dowMap = mapOf(
            "sunday" to DayOfWeek.SUNDAY,
            "monday" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
        )

        try {
            if (Regex("""\d{4}-\d{2}-\d{2}""").matches(lower)) {
                LocalDate.parse(lower, DateTimeFormatter.ISO_LOCAL_DATE).let {
                    return it.toString()
                }
            }
        } catch (_: Exception) {}

        when (lower) {
            "today" -> return today.toString()
            "tomorrow" -> return today.plusDays(1).toString()
            "the day after tomorrow", "day after tomorrow" -> return today.plusDays(2).toString()
        }

        dowMap[lower]?.let { targetDOW ->
            var daysToAdd = (targetDOW.value - today.dayOfWeek.value + 7) % 7
            if (daysToAdd == 0) daysToAdd = 7
            return today.plusDays(daysToAdd.toLong()).toString()
        }

        val regexNextDay = Regex("""next\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday)""")
        regexNextDay.find(lower)?.let {
            val targetDOW = dowMap[it.groupValues[1]]!!
            var daysToAdd = (targetDOW.value - today.dayOfWeek.value + 7) % 7
            if (daysToAdd == 0) daysToAdd = 7
            return today.plusDays(daysToAdd.toLong()).toString()
        }

        val regexThisDay = Regex("""this\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday)""")
        regexThisDay.find(lower)?.let {
            val targetDOW = dowMap[it.groupValues[1]]!!
            val todayDow = today.dayOfWeek.value
            val daysToAdd = (targetDOW.value - todayDow + 7) % 7
            return today.plusDays(daysToAdd.toLong()).toString()
        }

        val regexMD1 = Regex("""([a-zA-Z]+)\s+(\d{1,2})""")
        val regexMD2 = Regex("""(\d{1,2})\s+([a-zA-Z]+)""")
        fun monthStrToNum(mon: String): Int = when (mon.lowercase(Locale.US).take(3)) {
            "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6;
            "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
            else -> 0
        }
        regexMD1.find(lower)?.let {
            val month = monthStrToNum(it.groupValues[1])
            val day = it.groupValues[2].toIntOrNull() ?: 1
            if (month in 1..12) return LocalDate.of(today.year, month, day).toString()
        }
        regexMD2.find(lower)?.let {
            val day = it.groupValues[1].toIntOrNull() ?: 1
            val month = monthStrToNum(it.groupValues[2])
            if (month in 1..12) return LocalDate.of(today.year, month, day).toString()
        }

        return dateRaw
    }

    fun parseTimeSmart(timeRaw: String): Pair<String, String> {
        val cleaned = timeRaw.trim().lowercase(Locale.US)
        Log.d("parseTimeSmart", "입력: '$timeRaw' -> 정리: '$cleaned'")

        if (cleaned.length == 4 && cleaned.all { it.isDigit() }) {
            val result = cleaned.substring(0, 2) to cleaned.substring(2, 4)
            Log.d("parseTimeSmart", "4자리 숫자 파싱: $result")
            return result
        }

        Regex("""(\d{1,2})[\:\-\.](\d{2})""").find(cleaned)?.let { match ->
            val result = match.groupValues[1].padStart(2, '0') to match.groupValues[2].padStart(2, '0')
            Log.d("parseTimeSmart", "구분자 포함 파싱: $result")
            return result
        }

        val ampmPatterns = listOf(
            Regex("""(\d{1,2}):(\d{2})\s*(am|pm)"""),
            Regex("""(\d{1,2})\s*(am|pm)"""),
            Regex("""(\d{1,2}):(\d{2})\s*([ap])"""),
            Regex("""(\d{1,2})\s*([ap])""")
        )

        for (pattern in ampmPatterns) {
            pattern.find(cleaned)?.let { match ->
                var hour = match.groupValues[1].toInt()
                val minute = if (match.groupValues.size > 3 && match.groupValues[2].isNotEmpty()) {
                    match.groupValues[2]
                } else {
                    "00"
                }
                val ampm = match.groupValues.last().lowercase()

                when {
                    ampm.startsWith("p") && hour != 12 -> hour += 12
                    ampm.startsWith("a") && hour == 12 -> hour = 0
                }

                val result = hour.toString().padStart(2, '0') to minute.padStart(2, '0')
                Log.d("parseTimeSmart", "AM/PM 파싱: $result (원본: ${match.value})")
                return result
            }
        }

        Regex("""(\d{1,2}):(\d{2})""").find(cleaned)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2]
            if (hour in 0..23) {
                val result = hour.toString().padStart(2, '0') to minute.padStart(2, '0')
                Log.d("parseTimeSmart", "24시간 형식 파싱: $result")
                return result
            }
        }

        Regex("""^(\d{1,2})$""").find(cleaned)?.let { match ->
            val hour = match.groupValues[1].toInt()
            if (hour in 0..23) {
                val result = hour.toString().padStart(2, '0') to "00"
                Log.d("parseTimeSmart", "단순 시간 파싱: $result")
                return result
            }
        }

        val engNumMap = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
            "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12
        )

        when {
            cleaned.contains("noon") -> {
                Log.d("parseTimeSmart", "noon 파싱: 12:00")
                return "12" to "00"
            }
            cleaned.contains("midnight") -> {
                Log.d("parseTimeSmart", "midnight 파싱: 00:00")
                return "00" to "00"
            }
        }

        Regex("""(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\s*(thirty|fifteen|forty five|o'clock|zero)?""")
            .find(cleaned)?.let { match ->
                val hour = engNumMap[match.groupValues[1]]?.toString()?.padStart(2, '0') ?: ""
                val min = when {
                    cleaned.contains("thirty") -> "30"
                    cleaned.contains("fifteen") -> "15"
                    cleaned.contains("forty five") -> "45"
                    cleaned.contains("zero") || cleaned.contains("o'clock") -> "00"
                    else -> "00"
                }
                val result = hour to min
                Log.d("parseTimeSmart", "영어 숫자+분 파싱: $result")
                return result
            }

        Regex("""(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)(?:\s*(am|pm|a|p))?""")
            .find(cleaned)?.let { match ->
                var hour = engNumMap[match.groupValues[1]] ?: 0
                val ampm = match.groupValues[2].lowercase()

                when {
                    ampm.startsWith("p") && hour != 12 -> hour += 12
                    ampm.startsWith("a") && hour == 12 -> hour = 0
                }

                val result = hour.toString().padStart(2, '0') to "00"
                Log.d("parseTimeSmart", "영어 숫자+AM/PM 파싱: $result")
                return result
            }

        val colloquialTimes = mapOf(
            "morning" to ("09" to "00"),
            "afternoon" to ("14" to "00"),
            "evening" to ("18" to "00"),
            "night" to ("20" to "00"),
            "early morning" to ("07" to "00"),
            "late morning" to ("11" to "00"),
            "early afternoon" to ("13" to "00"),
            "late afternoon" to ("16" to "00"),
            "early evening" to ("17" to "00"),
            "late evening" to ("21" to "00")
        )

        for ((phrase, time) in colloquialTimes) {
            if (cleaned.contains(phrase)) {
                Log.d("parseTimeSmart", "구어체 표현 파싱: $time (원본: $phrase)")
                return time
            }
        }

        Log.d("parseTimeSmart", "파싱 실패: '$timeRaw' -> 빈 값 반환")
        return "" to ""
    }

    fun parseDateToLocalDate(dateString: String): LocalDate {
        val now = LocalDate.now()
        val dayOfWeekMap = mapOf(
            "sunday" to DayOfWeek.SUNDAY,
            "monday" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
        )

        try {
            return LocalDate.parse(dateString)
        } catch (_: Exception) { }

        val lower = dateString.trim().lowercase()

        val regexNextThis = Regex("""(next|this)\s+(sunday|monday|tuesday|wednesday|thursday|friday|saturday)""")
        regexNextThis.find(lower)?.let {
            val mode = it.groupValues[1]
            val dow = dayOfWeekMap[it.groupValues[2]] ?: return now
            val todayDow = now.dayOfWeek.value

            return when (mode) {
                "next" -> {
                    var daysToAdd = (dow.value - todayDow + 7) % 7
                    if (daysToAdd == 0) daysToAdd = 7
                    now.plusDays(daysToAdd.toLong())
                }
                "this" -> {
                    val daysToAdd = (dow.value - todayDow + 7) % 7
                    now.plusDays(daysToAdd.toLong())
                }
                else -> now
            }
        }

        dayOfWeekMap[lower]?.let { dow ->
            var daysToAdd = (dow.value - now.dayOfWeek.value + 7) % 7
            if (daysToAdd == 0) daysToAdd = 7
            return now.plusDays(daysToAdd.toLong())
        }

        return now
    }
}