package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "gym_exercise_muscles",
    primaryKeys = ["exerciseId", "muscleId"],
    indices = [
        Index(value = ["muscleId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["role"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MuscleEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseMuscleEntity(
    val exerciseId: String,
    val muscleId: String,
    val role: String,
    val weight: Float = 1f
)