package com.titanbiosync.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.titanbiosync.domain.model.AuthUser
import com.titanbiosync.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUser(): AuthUser? =
        firebaseAuth.currentUser?.toAuthUser()

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<AuthUser> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        result.user?.toAuthUser()
            ?: error("Sign-in succeeded but no user returned")
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName
    )
}
