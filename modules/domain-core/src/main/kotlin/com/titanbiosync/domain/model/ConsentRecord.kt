package com.titanbiosync.domain.model

data class ConsentRecord(
    val id: String,
    val userId: String,
    val consentType: String,
    val grantedAt: Long,
    val version: String?
)