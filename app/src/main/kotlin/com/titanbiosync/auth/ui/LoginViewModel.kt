package com.titanbiosync.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.model.AuthUser
import com.titanbiosync.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: AuthUser? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Returns true if a user is already signed in (e.g., persistent session). */
    fun isAlreadySignedIn(): Boolean = authRepository.getCurrentUser() != null

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Email e password sono obbligatorie"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.signInWithEmailAndPassword(email, password)
            _uiState.value = result.fold(
                onSuccess = { user -> LoginUiState(isLoading = false, loggedInUser = user) },
                onFailure = { e -> LoginUiState(isLoading = false, error = e.message) }
            )
        }
    }
}
