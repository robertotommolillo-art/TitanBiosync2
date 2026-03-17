package com.titanbiosync.domain.model

/**
 * Lightweight representation of an authenticated user returned by the auth provider.
 * Decoupled from any Firebase types so domain-core stays a plain Kotlin/JVM module.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)
