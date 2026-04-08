package com.titanbiosync.data.local.analytics

/** Result row returned by [com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao.getExerciseSessionCounts]. */
data class ExerciseSessionCountRow(
    val exerciseId: String,
    val sessionCount: Int
)
