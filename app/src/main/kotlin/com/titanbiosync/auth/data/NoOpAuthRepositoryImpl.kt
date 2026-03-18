package com.titanbiosync.auth.data

import com.titanbiosync.domain.model.AuthUser
import com.titanbiosync.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * No-op implementation of [AuthRepository].
 * Firebase authentication is optional; the app runs in local-first mode.
 * Firebase backup/restore will be added in a future release.
 */
class NoOpAuthRepositoryImpl : AuthRepository {

    override fun observeAuthState(): Flow<AuthUser?> = flowOf(null)

    override fun getCurrentUser(): AuthUser? = null

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<AuthUser> = Result.failure(UnsupportedOperationException("Firebase auth not enabled"))

    override fun signOut() {
        // no-op
    }
}
