package com.titanbiosync.gym.online.model

data class OnlineExerciseResolved(
    val idHint: String? = null,
    val nameIt: String,
    val nameEn: String,
    val descriptionIt: String? = null,
    val descriptionEn: String? = null,
    val category: String,
    val equipment: String? = null,
    val mechanics: String? = null,
    val level: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val muscles: List<MuscleLink> = emptyList()
) {
    data class MuscleLink(
        val muscleId: String,
        val role: String,   // "primary" | "secondary" | "stabilizer"
        val weight: Float = 1f
    )
}