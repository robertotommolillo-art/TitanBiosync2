package com.titanbiosync.domain.model

data class CoachPrompt(
    val id: String,
    val userId: String,
    val promptText: String,
    val responseText: String,
    val modelVersion: String?,
    val tokensUsed: Int?,
    val timestamp: Long
)