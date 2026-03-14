package com.titanbiosync.gym.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionDao
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionDao: GymWorkoutSessionDao
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val _uiStateLive = MutableLiveData(
        HistoryUiState(
            visibleMonth = YearMonth.now(),
            selectedDate = null,
            sessionsForSelectedDate = emptyList(),
            daysWithSessions = emptySet()
        )
    )
    val uiStateLive: LiveData<HistoryUiState> = _uiStateLive

    val uiState: HistoryUiState get() = _uiStateLive.value!!

    private var observeJob: Job? = null
    private var visibleMonthSessionsCache: List<GymWorkoutSessionEntity> = emptyList()

    init {
        observeMonth(YearMonth.now())
    }

    /**
     * Quando entri nello Storico dal bottom nav, vuoi ripartire dal mese corrente,
     * ma senza preselezionare alcun giorno.
     */
    fun resetToCurrentMonth() {
        val nowMonth = YearMonth.now()
        if (uiState.visibleMonth == nowMonth && uiState.selectedDate == null) {
            return
        }

        _uiStateLive.value = uiState.copy(
            visibleMonth = nowMonth,
            selectedDate = null,
            sessionsForSelectedDate = emptyList(),
            daysWithSessions = emptySet()
        )

        observeMonth(nowMonth)
    }

    fun onMonthScrolled(month: YearMonth) {
        if (month == uiState.visibleMonth) return
        _uiStateLive.value = uiState.copy(
            visibleMonth = month,
            selectedDate = null,
            sessionsForSelectedDate = emptyList()
        )
        observeMonth(month)
    }

    fun goToPreviousMonth() {
        val month = uiState.visibleMonth.minusMonths(1)
        _uiStateLive.value = uiState.copy(
            visibleMonth = month,
            selectedDate = null,
            sessionsForSelectedDate = emptyList()
        )
        observeMonth(month)
    }

    fun goToNextMonth() {
        val month = uiState.visibleMonth.plusMonths(1)
        _uiStateLive.value = uiState.copy(
            visibleMonth = month,
            selectedDate = null,
            sessionsForSelectedDate = emptyList()
        )
        observeMonth(month)
    }

    fun selectDate(date: LocalDate) {
        val sessions = filterSessionsForDay(visibleMonthSessionsCache, date)
        _uiStateLive.value = uiState.copy(
            selectedDate = date,
            sessionsForSelectedDate = sessions
        )
    }

    private fun observeMonth(month: YearMonth) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            sessionDao.observeBetween(start, endExclusive).collectLatest { sessions ->
                visibleMonthSessionsCache = sessions

                val daysWith = sessions
                    .map { it.startedAt.toLocalDate(zoneId) }
                    .toSet()

                val selected = uiState.selectedDate
                val sessionsForSelected = selected?.let { filterSessionsForDay(sessions, it) } ?: emptyList()

                _uiStateLive.value = uiState.copy(
                    visibleMonth = month,
                    daysWithSessions = daysWith,
                    sessionsForSelectedDate = sessionsForSelected
                )
            }
        }
    }

    private fun filterSessionsForDay(
        sessions: List<GymWorkoutSessionEntity>,
        date: LocalDate
    ): List<GymWorkoutSessionEntity> {
        return sessions.filter { it.startedAt.toLocalDate(zoneId) == date }
            .sortedByDescending { it.startedAt }
    }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}