package com.titanbiosync.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.usecase.session.GetSessionSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionSummaryUiState(
    val summary: GetSessionSummaryUseCase.Result.Success? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val getSessionSummaryUseCase: GetSessionSummaryUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId")
        ?: throw IllegalArgumentException("sessionId required")

    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState: StateFlow<SessionSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            try {
                when (val result = getSessionSummaryUseCase(sessionId)) {
                    is GetSessionSummaryUseCase.Result.Success -> {
                        _uiState.value = SessionSummaryUiState(
                            summary = result,
                            isLoading = false
                        )
                    }
                    is GetSessionSummaryUseCase.Result.Error -> {
                        _uiState.value = SessionSummaryUiState(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SessionSummaryUiState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}