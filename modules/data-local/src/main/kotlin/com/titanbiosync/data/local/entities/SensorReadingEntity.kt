package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sensor_readings",
    indices = [
        Index(value = ["device_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["session_id"])
    ]
)
data class SensorReadingEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "device_id") val deviceId: String? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sensor_type") val sensorType: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "quality_score") val qualityScore: Float? = null,
    @ColumnInfo(name = "session_id") val sessionId: String? = null
)