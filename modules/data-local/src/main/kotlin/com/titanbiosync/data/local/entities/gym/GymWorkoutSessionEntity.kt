package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gym_workout_session")
data class GymWorkoutSessionEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val startedAt: Long,
    val endedAt: Long? = null
)