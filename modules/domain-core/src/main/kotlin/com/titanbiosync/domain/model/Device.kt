package com.titanbiosync.domain.model

data class Device(
    val id: String,
    val deviceId: String,
    val model: String?,
    val firmwareVersion: String?,
    val lastSeenAt: Long?,
    val status: String?,
    val capabilitiesJson: String?
)