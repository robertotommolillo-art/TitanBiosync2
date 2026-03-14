package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "consents",
    indices = [Index(value = ["user_id", "consent_type"])]
)
data class ConsentRecordEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "consent_type") val consentType: String,
    @ColumnInfo(name = "granted_at") val grantedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "version") val version: String? = null
)