package com.titanbiosync.gym.online.model

data class OnlineExerciseCandidate(
    val candidateId: String,
    val title: String,
    val subtitle: String,
    val sourceName: String,
    val sourceUrl: String? = null,
    val confidence: Float = 0.6f
)