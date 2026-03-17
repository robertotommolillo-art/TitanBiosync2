package com.titanbiosync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.SessionDao
import com.titanbiosync.data.local.entities.SessionEntity
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.math.max

data class DashboardUiState(
    val user: User? = null,
    val activeSession: Session? = null,

    // old fields (ok to keep)
    val todaySessionsCount: Int = 0,
    val todayActiveMinutes: Int = 0,
    val connectedDevicesCount: Int = 0,
    val streakDays: Int? = null,

    // snapshot
    val motivationText: String = "",
    val weekWorkoutsCount: Int = 0,
    val weekActiveMinutes: Int = 0,
    val monthActiveMinutes: Int = 0,
    val weekVolumeText: String = "—",
    val lastWorkoutText: String = "—",

    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getActiveSessionUseCase: GetActiveSessionUseCase,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentUserId: String = "default_user_id"
    private var activeSessionTickerJob: Job? = null
    private var sessionsObserverJob: Job? = null

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            try {
                activeSessionTickerJob?.cancel()

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                val user = createUserUseCase(
                    CreateUserUseCase.Params(
                        email = "demo@titanbiosync.com",
                        displayName = "Demo User"
                    )
                )
                currentUserId = user.id

                // set user + motivation early
                _uiState.value = _uiState.value.copy(
                    user = user,
                    motivationText = pickMotivation()
                )

                // Start/Restart sessions observer for snapshot
                startSessionsObserver()

                val activeSession = getActiveSessionUseCase(
                    GetActiveSessionUseCase.Params(currentUserId)
                )

                val todaySessionsCount = if (activeSession != null) 1 else 0
                val todayActiveMinutes = computeActiveMinutes(activeSession)
                val connectedDevicesCount = computeConnectedDevicesCount(activeSession)

                _uiState.value = _uiState.value.copy(
                    activeSession = activeSession,
                    todaySessionsCount = todaySessionsCount,
                    todayActiveMinutes = todayActiveMinutes,
                    connectedDevicesCount = connectedDevicesCount,
                    streakDays = null,
                    isLoading = false
                )

                if (activeSession != null && activeSession.endedAt == null) {
                    startActiveSessionTicker()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun startSessionsObserver() {
        sessionsObserverJob?.cancel()
        sessionsObserverJob = viewModelScope.launch {
            sessionDao.observeByUser(currentUserId).collect { entities ->
                val snapshot = computeSnapshot(entities)
                _uiState.value = _uiState.value.copy(
                    weekWorkoutsCount = snapshot.weekWorkoutsCount,
                    weekActiveMinutes = snapshot.weekActiveMinutes,
                    monthActiveMinutes = snapshot.monthActiveMinutes,
                    lastWorkoutText = snapshot.lastWorkoutText,
                    weekVolumeText = "—" // placeholder Step 1
                )
            }
        }
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
        return max(0, (diffMs / 60_000L).toInt())
    }

    private fun normalizeEpochMillis(value: Long): Long {
        return if (value in 1_000_000_000L..9_999_999_999L) value * 1000L else value
    }

    private data class Snapshot(
        val weekWorkoutsCount: Int,
        val weekActiveMinutes: Int,
        val monthActiveMinutes: Int,
        val lastWorkoutText: String
    )

    private fun computeSnapshot(sessions: List<SessionEntity>): Snapshot {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val weekStart = today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val monthStart = today
            .withDayOfMonth(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        fun durationMinutes(s: SessionEntity): Int {
            val started = normalizeEpochMillis(s.startedAt)
            val ended = s.endedAt?.let { normalizeEpochMillis(it) } ?: now
            return max(0, ((ended - started) / 60_000L).toInt())
        }

        val weekSessions = sessions.filter { normalizeEpochMillis(it.startedAt) >= weekStart }
        val monthSessions = sessions.filter { normalizeEpochMillis(it.startedAt) >= monthStart }

        val weekMinutes = weekSessions.sumOf { durationMinutes(it) }
        val monthMinutes = monthSessions.sumOf { durationMinutes(it) }

        val last = sessions.firstOrNull()
        val lastWorkoutText = if (last == null) {
            "—"
        } else {
            val date = Instant.ofEpochMilli(normalizeEpochMillis(last.startedAt))
                .atZone(zone)
                .toLocalDate()
            val day = date.dayOfMonth
            val month = date.month.name.lowercase().take(3)
            "$day $month • ${last.type}"
        }

        return Snapshot(
            weekWorkoutsCount = weekSessions.size,
            weekActiveMinutes = weekMinutes,
            monthActiveMinutes = monthMinutes,
            lastWorkoutText = lastWorkoutText
        )
    }

    private fun pickMotivation(): String {
        val options = listOf(
            "Costanza > motivazione.",
            "Un passo alla volta.",
            "Oggi si spinge.",
            "Focus. Respira. Ripeti."
        )
        return options.random()
    }

    private fun computeConnectedDevicesCount(session: Session?): Int {
        val json = session?.deviceIdsJson?.trim().orEmpty()
        if (json.isBlank()) return 0

        val quotes = json.count { it == '"' }
        return if (quotes >= 2) quotes / 2 else 0
    }

    fun refreshDashboard() {
        loadDashboard()
    }

    override fun onCleared() {
        super.onCleared()
        activeSessionTickerJob?.cancel()
        sessionsObserverJob?.cancel()
    }
}