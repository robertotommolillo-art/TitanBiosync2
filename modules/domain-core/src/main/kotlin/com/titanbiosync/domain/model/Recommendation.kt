package com.titanbiosync.domain.model

data class Recommendation(
    val id: String,
    val userId: String,
    val createdAt: Long,
    val source: String,
    val contentJson: String,
    val confidence: Float?,
    val status: String?,
    val relatedSessionId: String?
)