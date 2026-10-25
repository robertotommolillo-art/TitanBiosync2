package com.titanbiosync.coach.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalExerciseHint(
    val nameIt: String,
    val equipment: String? = null,
    val muscles: List<String> = emptyList()
)

