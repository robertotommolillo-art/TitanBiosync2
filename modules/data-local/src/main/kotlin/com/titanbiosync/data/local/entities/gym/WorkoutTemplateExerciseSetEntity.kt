package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "gym_workout_template_exercise_sets",
    primaryKeys = ["templateId", "position", "setIndex"],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["templateId", "position"])
    ]
)
data class WorkoutTemplateExerciseSetEntity(
    val templateId: String,
    val position: Int,
    val setIndex: Int,
    val reps: Int? = null
)