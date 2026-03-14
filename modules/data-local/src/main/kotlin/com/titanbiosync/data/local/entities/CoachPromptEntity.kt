package com.titanbiosync.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "coach_prompts",
    indices = [Index(value = ["user_id"]), Index(value = ["timestamp"])]
)
data class CoachPromptEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "prompt_text") val promptText: String,
    @ColumnInfo(name = "response_text") val responseText: String,
    @ColumnInfo(name = "model_version") val modelVersion: String? = null,
    @ColumnInfo(name = "tokens_used") val tokensUsed: Int? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)