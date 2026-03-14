package com.titanbiosync.data.local.analytics

data class GymWorkoutAnalyticsRow(
    val sessionId: String,
    val startedAt: Long,
    val exerciseId: String,
    val exerciseNameIt: String,
    val reps: Int?,
    val weightKg: Float?,
    val completed: Boolean
)