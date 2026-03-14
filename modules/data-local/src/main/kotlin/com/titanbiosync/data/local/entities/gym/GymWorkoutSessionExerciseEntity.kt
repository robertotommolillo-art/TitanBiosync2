package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_workout_session_exercise",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["sessionId", "position"], unique = true)
    ]
)
data class GymWorkoutSessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val position: Int,
    val nameItSnapshot: String
)