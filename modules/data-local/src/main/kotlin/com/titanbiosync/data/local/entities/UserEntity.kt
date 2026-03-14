package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [Index(value = ["external_id"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "external_id") val externalId: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = Instant.now().toEpochMilli(),
    @ColumnInfo(name = "last_active_at") val lastActiveAt: Long? = null,
    @ColumnInfo(name = "privacy_consent") val privacyConsent: String? = null,
    @ColumnInfo(name = "preferences_json") val preferencesJson: String? = null
)