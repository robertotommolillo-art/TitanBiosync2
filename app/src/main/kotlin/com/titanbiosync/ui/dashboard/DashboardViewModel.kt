package com.titanbiosync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.model.Session
import com.titanbiosync.domain.model.User
import com.titanbiosync.domain.usecase.session.GetActiveSessionUseCase
import com.titanbiosync.domain.usecase.user.CreateUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

data class DashboardUiState(
    val user: User? = null,
    val activeSession: Session? = null,
    val todaySessionsCount: Int = 0,
    val todayActiveMinutes: Int = 0,
    val connectedDevicesCount: Int = 0,
    val streakDays: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentUserId: String = "default_user_id"
    private var activeSessionTickerJob: Job? = null

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            try {
                activeSessionTickerJob?.cancel()
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val user = createUserUseCase(
                    CreateUserUseCase.Params(
                        email = "demo@titanbiosync.com",
                        displayName = "Demo User"
                    )
                )
                currentUserId = user.id

                val activeSession = getActiveSessionUseCase(
                    GetActiveSessionUseCase.Params(currentUserId)
                )

                val todaySessionsCount = if (activeSession != null) 1 else 0
                val todayActiveMinutes = computeActiveMinutes(activeSession)
                val connectedDevicesCount = computeConnectedDevicesCount(activeSession)
                val streakDays: Int? = null

                _uiState.value = DashboardUiState(
                    user = user,
                    activeSession = activeSession,
                    todaySessionsCount = todaySessionsCount,
                    todayActiveMinutes = todayActiveMinutes,
                    connectedDevicesCount = connectedDevicesCount,
                    streakDays = streakDays,
                    isLoading = false,
                    error = null
                )

                if (activeSession != null && activeSession.endedAt == null) {
                    startActiveSessionTicker()
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun startActiveSessionTicker() {
        activeSessionTickerJob?.cancel()
        activeSessionTickerJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // ogni 30 secondi
                val session = _uiState.value.activeSession ?: break
                if (session.endedAt != null) break

                _uiState.value = _uiState.value.copy(
                    todayActiveMinutes = computeActiveMinutes(session),
                    connectedDevicesCount = computeConnectedDevicesCount(session)
                )
            }
        }
    }

    private fun computeActiveMinutes(session: Session?): Int {
        if (session == null) return 0

        val startedAtMs = normalizeEpochMillis(session.startedAt)
        val endedAtMs = session.endedAt?.let { normalizeEpochMillis(it) } ?: System.currentTimeMillis()

        val diffMs = endedAtMs - startedAtMs
        val minutes = (diffMs / 60_000L).toInt()
        return max(0, minutes)
    }

    private fun normalizeEpochMillis(value: Long): Long {
        // 10 cifre ~ epoch seconds, 13 cifre ~ epoch millis.
        return if (value in 1_000_000_000L..9_999_999_999L) value * 1000L else value
    }

    private fun computeConnectedDevicesCount(session: Session?): Int {
        val json = session?.deviceIdsJson?.trim().orEmpty()
        if (json.isBlank()) return 0

        // Atteso: ["id1","id2"] oppure [].
        // Fallback conservativo senza parser JSON: contiamo le virgolette e dividiamo per 2.
        val quotes = json.count { it == '"' }
        if (quotes >= 2) return quotes / 2

        return 0
    }

    fun refreshDashboard() {
        loadDashboard()
    }

    override fun onCleared() {
        super.onCleared()
        activeSessionTickerJob?.cancel()
    }
}