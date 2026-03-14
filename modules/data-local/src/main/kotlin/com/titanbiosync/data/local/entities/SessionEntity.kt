package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["user_id"]), Index(value = ["started_at"])]
)
data class SessionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(name = "device_ids_json") val deviceIdsJson: String? = null,
    @ColumnInfo(name = "aggregated_metrics_json") val aggregatedMetricsJson: String? = null,
    @ColumnInfo(name = "annotations_json") val annotationsJson: String? = null
)