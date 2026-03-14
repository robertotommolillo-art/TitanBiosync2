package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "devices",
    indices = [Index(value = ["device_id"], unique = true)]
)
data class DeviceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "model") val model: String? = null,
    @ColumnInfo(name = "firmware_version") val firmwareVersion: String? = null,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long? = null,
    @ColumnInfo(name = "status") val status: String? = null,
    @ColumnInfo(name = "capabilities_json") val capabilitiesJson: String? = null
)