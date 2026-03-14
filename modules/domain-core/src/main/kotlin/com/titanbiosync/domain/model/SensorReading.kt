package com.titanbiosync.domain.model

data class SensorReading(
    val id: String,
    val deviceId: String?,
    val timestamp: Long,
    val sensorType: String,
    val value: String,
    val qualityScore: Float?,
    val sessionId: String?
)