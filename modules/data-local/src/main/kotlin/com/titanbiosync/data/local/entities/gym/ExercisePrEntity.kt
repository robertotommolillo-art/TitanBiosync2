package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists per-exercise personal records (PRs) for strength progression tracking.
 * One row per exercise; updated whenever a new PR is achieved after a session.
 */
@Entity(
    tableName = "gym_exercise_pr",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ExercisePrEntity(
    @PrimaryKey val exerciseId: String,
    /** Best single-set weight lifted (kg). */
    val maxWeightKg: Float,
    /** Best estimated 1RM computed via Epley formula (kg). */
    val maxE1rm: Float,
    /** Highest rep count recorded in a single set. */
    val maxReps: Int,
    /** Epoch millis when this record was last updated. */
    val updatedAt: Long = System.currentTimeMillis()
)
