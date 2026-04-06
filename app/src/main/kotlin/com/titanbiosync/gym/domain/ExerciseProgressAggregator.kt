package com.titanbiosync.gym.domain

import com.titanbiosync.data.local.analytics.ExerciseRawSetRow
import com.titanbiosync.domain.gym.E1rmCalculator

/**
 * Aggregates raw set rows (from the DAO) into per-session [ExerciseSessionPoint] entries.
 *
 * This is a pure object with no Android/Room dependencies so it can be unit-tested easily.
 */
object ExerciseProgressAggregator {

    /**
     * Converts a flat list of raw set rows (all belonging to one exercise) into a sorted list
     * of per-session summary points.
     *
     * @param rows  Raw set rows for a single exercise, any order.
     * @return      Session points sorted ascending by [ExerciseSessionPoint.sessionDate].
     */
    fun aggregate(rows: List<ExerciseRawSetRow>): List<ExerciseSessionPoint> {
        if (rows.isEmpty()) return emptyList()

        // Group rows by sessionId, keep the earliest startedAt for each session
        val bySession = rows.groupBy { it.sessionId }

        return bySession.map { (sessionId, sets) ->
            val startedAt = sets.first().startedAt
            val maxWeight = sets.maxOf { it.weightKg }
            val totalVolume = sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
            val bestE1rm = E1rmCalculator.bestE1rm(
                sets.map { Pair(it.weightKg, it.reps) }
            )
            ExerciseSessionPoint(
                sessionId = sessionId,
                sessionDate = startedAt,
                maxWeightKg = maxWeight,
                bestE1rm = bestE1rm,
                totalVolume = totalVolume
            )
        }.sortedBy { it.sessionDate }
    }

    /**
     * Returns the last [limit] session points (most recent), ordered ascending (oldest first).
     * Useful for sparkline charts. Accepts already-aggregated [points].
     */
    fun lastN(points: List<ExerciseSessionPoint>, limit: Int): List<ExerciseSessionPoint> {
        return if (points.size <= limit) points else points.takeLast(limit)
    }
}

/** A single-session summary for one exercise. */
data class ExerciseSessionPoint(
    val sessionId: String,
    /** Session start timestamp (ms). */
    val sessionDate: Long,
    val maxWeightKg: Float,
    val bestE1rm: Float?,
    val totalVolume: Float
) {
    /** The primary metric value used for chart y-axis: best e1RM when available, else max weight. */
    val primaryValue: Float get() = bestE1rm ?: maxWeightKg
}
