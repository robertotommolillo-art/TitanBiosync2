package com.titanbiosync.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.model.Session
import com.titanbiosync.domain.usecase.session.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StartSessionUiState(
    val isLoading: Boolean = false,
    val startedSession: Session? = null,
    val error: String? = null
)

@HiltViewModel
class StartSessionViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartSessionUiState())
    val uiState: StateFlow<StartSessionUiState> = _uiState.asStateFlow()

    // ID utente di default (in un'app reale verrebbe da auth)
    private val currentUserId = "default_user_id"

    fun startSession(sessionType: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val session = startSessionUseCase(
                    StartSessionUseCase.Params(
                        userId = currentUserId,
                        sessionType = sessionType
                    )
                )

                _uiState.value = StartSessionUiState(
                    isLoading = false,
                    startedSession = session
                )

            } catch (e: Exception) {
                _uiState.value = StartSessionUiState(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = StartSessionUiState()
    }
}