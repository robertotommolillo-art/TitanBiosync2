package com.titanbiosync.gym.ui.history

import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionEntity
import java.time.LocalDate
import java.time.YearMonth

data class HistoryUiState(
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate? = null,
    val daysWithSessions: Set<LocalDate> = emptySet(),
    val sessionsForSelectedDate: List<GymWorkoutSessionEntity> = emptyList()
)