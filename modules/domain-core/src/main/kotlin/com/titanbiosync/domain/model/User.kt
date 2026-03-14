package com.titanbiosync.domain.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val externalId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val lastActiveAt: Long? = null,
    val privacyConsent: String? = null,
    val preferencesJson: String? = null
)