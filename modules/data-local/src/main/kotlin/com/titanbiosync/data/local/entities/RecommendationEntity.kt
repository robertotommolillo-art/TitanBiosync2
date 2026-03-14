package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "recommendations",
    indices = [Index(value = ["user_id"]), Index(value = ["created_at"])]
)
data class RecommendationEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "content_json") val contentJson: String,
    @ColumnInfo(name = "confidence") val confidence: Float? = null,
    @ColumnInfo(name = "status") val status: String? = null,
    @ColumnInfo(name = "related_session_id") val relatedSessionId: String? = null
)