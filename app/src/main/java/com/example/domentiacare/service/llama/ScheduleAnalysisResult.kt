package com.example.domentiacare.service.llama

sealed class ScheduleAnalysisResult {
    data class Success(val jsonResult: String) : ScheduleAnalysisResult()
    object NoSchedule : ScheduleAnalysisResult()
    data class Error(val message: String) : ScheduleAnalysisResult()
}