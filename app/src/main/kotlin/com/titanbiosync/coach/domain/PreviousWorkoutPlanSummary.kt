package com.titanbiosync.coach.domain

/**
 * Compact summary of a previously saved workout template used to reduce duplicates
 * when generating a new plan.
 */
data class PreviousWorkoutPlanSummary(
    val title: String,
    val exerciseNames: List<String>
)

