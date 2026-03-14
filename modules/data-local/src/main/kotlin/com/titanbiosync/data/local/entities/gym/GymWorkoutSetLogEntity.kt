package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_workout_set_log",
    indices = [
        Index(value = ["sessionExerciseId"]),
        Index(value = ["sessionExerciseId", "setIndex"], unique = true)
    ]
)
data class GymWorkoutSetLogEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val reps: Int? = null,
    val weightKg: Float? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null
)