package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_workout_templates",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["sortIndex"])
    ]
)
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val folderId: String? = null, // null => "Senza cartella"
    val name: String,
    val notes: String? = null,
    val sortIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)