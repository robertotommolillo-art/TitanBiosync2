package com.titanbiosync.data.local.analytics

/** A single completed set row returned by [com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao.getRawSetsForExercise]. */
data class ExerciseRawSetRow(
    val sessionId: String,
    val startedAt: Long,
    val reps: Int,
    val weightKg: Float
)
