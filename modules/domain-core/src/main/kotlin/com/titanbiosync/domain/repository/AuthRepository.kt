package com.titanbiosync.domain.repository

import com.titanbiosync.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the authentication provider (e.g., Firebase Auth).
 * Implementations live in the data/app layer; domain-core stays provider-agnostic.
 */
interface AuthRepository {
    /** Emits the currently signed-in [AuthUser], or `null` when signed out. */
    fun observeAuthState(): Flow<AuthUser?>

    /** Returns the currently signed-in user synchronously, or `null`. */
    fun getCurrentUser(): AuthUser?

    /**
     * Attempts to sign in with email + password.
     * Returns [Result.success] with the [AuthUser] on success, or [Result.failure] on error.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthUser>

    /** Signs the current user out. */
    fun signOut()
}
