package com.titanbiosync.domain.model

data class MapLocation(
    val id: String,
    val sessionId: String?,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val accuracy: Float?,
    val timestamp: Long
)