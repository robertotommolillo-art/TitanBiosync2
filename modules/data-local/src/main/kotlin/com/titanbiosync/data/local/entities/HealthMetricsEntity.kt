package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "health_metrics",
    indices = [Index(value = ["user_id", "date"], unique = true)]
)
data class HealthMetricsEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "resting_hr") val restingHr: Int? = null,
    @ColumnInfo(name = "hrv") val hrv: Float? = null,
    @ColumnInfo(name = "spo2_avg") val spo2Avg: Float? = null,
    @ColumnInfo(name = "steps") val steps: Int? = null,
    @ColumnInfo(name = "sleep_summary_json") val sleepSummaryJson: String? = null
)