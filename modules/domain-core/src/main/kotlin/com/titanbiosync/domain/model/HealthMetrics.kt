package com.titanbiosync.domain.model

data class HealthMetrics(
    val id: String,
    val userId: String,
    val date: String, // "yyyy-MM-dd"
    val restingHr: Int?,
    val hrv: Float?,
    val spo2Avg: Float?,
    val steps: Int?,
    val sleepSummaryJson: String?
)