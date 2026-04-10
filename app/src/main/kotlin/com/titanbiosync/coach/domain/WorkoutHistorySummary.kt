package com.titanbiosync.coach.domain

/**
 * Compact summary of the user's recent workout history sent to the backend.
 * Kept small to control token usage and protect privacy.
 */
data class WorkoutHistorySummary(
    val recentSessions: List<RecentSession> = emptyList(),
    val topPrs: List<ExercisePr> = emptyList(),
    val weeklyFrequency: Float? = null
) {
    data class RecentSession(
        val templateName: String,
        val date: String,
        val durationMin: Int,
        val totalVolumeKg: Float
    )

    data class ExercisePr(
        val exerciseName: String,
        val maxWeightKg: Float,
        val maxE1rm: Float
    )
}
