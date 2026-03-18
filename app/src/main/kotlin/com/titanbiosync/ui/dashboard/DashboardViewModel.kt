package com.titanbiosync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.model.Session
import com.titanbiosync.domain.model.User
import com.titanbiosync.domain.repository.UserRepository
import com.titanbiosync.domain.usecase.session.GetActiveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
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
    private val userRepository: UserRepository,
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

                val user = resolveLocalUser()
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

    /**
     * Resolves the local [User] profile. If no profile exists yet, creates a placeholder.
     */
    private suspend fun resolveLocalUser(): User {
        val existing = userRepository.getCurrentProfile()
        if (existing != null) return existing

        // No profile yet — create a minimal placeholder (profile setup should handle this)
        val newUser = User(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
        userRepository.upsert(newUser)
        return newUser
    }

    private fun startActiveSessionTicker() {
        activeSessionTickerJob?.cancel()
        activeSessionTickerJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
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
        return if (value in 1_000_000_000L..9_999_999_999L) value * 1000L else value
    }

    private fun computeConnectedDevicesCount(session: Session?): Int {
        val json = session?.deviceIdsJson?.trim().orEmpty()
        if (json.isBlank()) return 0

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
