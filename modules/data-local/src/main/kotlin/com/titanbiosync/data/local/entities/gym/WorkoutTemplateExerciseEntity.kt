package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "gym_workout_template_exercises",
    primaryKeys = ["templateId", "position"],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutTemplateExerciseEntity(
    val templateId: String,
    val position: Int,
    val exerciseId: String,

    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val restSeconds: Int? = null,
    val notes: String? = null,

    // v10+
    val supersetGroupId: String? = null,
    val supersetOrder: Int? = null
)