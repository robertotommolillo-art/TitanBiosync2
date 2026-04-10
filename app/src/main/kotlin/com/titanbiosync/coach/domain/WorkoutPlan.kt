package com.titanbiosync.coach.domain

/**
 * A structured workout plan returned by the AI Coach "generate" endpoint.
 */
data class WorkoutPlan(
    val title: String,
    val notes: String?,
    val exercises: List<WorkoutPlanExercise>
)

data class WorkoutPlanExercise(
    val nameIt: String,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int?,
    val notes: String?
)
