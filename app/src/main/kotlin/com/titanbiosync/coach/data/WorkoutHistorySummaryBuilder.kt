package com.titanbiosync.coach.data

import com.titanbiosync.coach.domain.WorkoutHistorySummary
import com.titanbiosync.data.local.dao.gym.ExercisePrDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionExerciseDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateDao
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads recent workout history from Room and produces a compact [WorkoutHistorySummary]
 * suitable for sending to the backend coach proxy.
 *
 * Kept deliberately small (last N sessions, top M PRs) to control token usage and privacy.
 */
@Singleton
class WorkoutHistorySummaryBuilder @Inject constructor(
    private val sessionDao: GymWorkoutSessionDao,
    private val sessionExerciseDao: GymWorkoutSessionExerciseDao,
    private val setLogDao: GymWorkoutSetLogDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val exercisePrDao: ExercisePrDao,
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Builds a compact summary of the user's recent workout history.
     *
     * @param recentSessionCount  Number of recent completed sessions to include (default 7).
     * @param topPrCount          Number of top PR exercises to include (default 8).
     */
    suspend fun build(
        recentSessionCount: Int = 7,
        topPrCount: Int = 8
    ): WorkoutHistorySummary {
        val allSessions = sessionDao.observeAll().first()
        val completedSessions = allSessions
            .filter { it.endedAt != null }
            .sortedByDescending { it.startedAt }

        // Cache all templates for name lookup
        val allTemplates = workoutTemplateDao.getAllOnce().associateBy { it.id }

        // ── Recent sessions ────────────────────────────────────────────
        val recentSessions = completedSessions
            .take(recentSessionCount)
            .map { session ->
                val durationMs = (session.endedAt!! - session.startedAt).coerceAtLeast(0L)
                val durationMin = TimeUnit.MILLISECONDS.toMinutes(durationMs).toInt()

                val sets = setLogDao.getCompletedForSession(session.id)
                val totalVolumeKg = sets.sumOf { set ->
                    ((set.reps ?: 0) * (set.weightKg ?: 0f)).toDouble()
                }.toFloat()

                val templateName = allTemplates[session.templateId]?.name ?: "Allenamento"

                WorkoutHistorySummary.RecentSession(
                    templateName = templateName,
                    date = dateFormat.format(Date(session.startedAt)),
                    durationMin = durationMin,
                    totalVolumeKg = totalVolumeKg
                )
            }

        // ── Weekly frequency ───────────────────────────────────────────
        val weeklyFrequency = calculateWeeklyFrequency(completedSessions.map { it.startedAt })

        // ── Top PRs ────────────────────────────────────────────────────
        val recentSessionIds = completedSessions.take(recentSessionCount).map { it.id }
        val topPrs = buildTopPrs(recentSessionIds, topPrCount)

        return WorkoutHistorySummary(
            recentSessions = recentSessions,
            topPrs = topPrs,
            weeklyFrequency = weeklyFrequency
        )
    }

    /** Estimates average sessions per week over the last 4 weeks. */
    companion object {
        fun calculateWeeklyFrequency(sessionStartTimes: List<Long>): Float? {
            if (sessionStartTimes.isEmpty()) return null
            val fourWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(28)
            val recentCount = sessionStartTimes.count { it >= fourWeeksAgo }
            return if (recentCount == 0) null else recentCount / 4f
        }
    }

    /** Collects the top PR exercises from recent sessions. */
    private suspend fun buildTopPrs(
        recentSessionIds: List<String>,
        limit: Int
    ): List<WorkoutHistorySummary.ExercisePr> {
        if (recentSessionIds.isEmpty()) return emptyList()

        // Collect session exercises with their snapshots for name resolution
        val sessionExercises = recentSessionIds.flatMap { sessionId ->
            sessionExerciseDao.getForSession(sessionId)
        }
        val exerciseNameMap = sessionExercises.associate { it.exerciseId to it.nameItSnapshot }
        val exerciseIds = exerciseNameMap.keys.toList()

        if (exerciseIds.isEmpty()) return emptyList()

        val prs = exercisePrDao.getByExerciseIds(exerciseIds)

        return prs.sortedByDescending { it.maxE1rm }
            .take(limit)
            .map { pr ->
                WorkoutHistorySummary.ExercisePr(
                    exerciseName = exerciseNameMap[pr.exerciseId] ?: pr.exerciseId,
                    maxWeightKg = pr.maxWeightKg,
                    maxE1rm = pr.maxE1rm
                )
            }
    }
}
