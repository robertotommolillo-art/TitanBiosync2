package com.titanbiosync.coach

import com.titanbiosync.coach.data.WorkoutHistorySummaryBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the pure helper in [WorkoutHistorySummaryBuilder].
 * Does not require Room or coroutines — tests the companion object directly.
 */
class WorkoutHistorySummaryBuilderTest {

    @Test
    fun `calculateWeeklyFrequency returns null for empty list`() {
        val result = WorkoutHistorySummaryBuilder.calculateWeeklyFrequency(emptyList())
        assertNull(result)
    }

    @Test
    fun `calculateWeeklyFrequency returns null when no sessions in last 4 weeks`() {
        val fiveWeeksAgo = System.currentTimeMillis() - 35L * 24 * 60 * 60 * 1000
        val result = WorkoutHistorySummaryBuilder.calculateWeeklyFrequency(listOf(fiveWeeksAgo))
        assertNull(result)
    }

    @Test
    fun `calculateWeeklyFrequency computes frequency for sessions in last 4 weeks`() {
        val now = System.currentTimeMillis()
        // 8 sessions in the last 28 days → 8 / 4 = 2.0 per week
        val sessions = List(8) { now - it.toLong() * 24 * 60 * 60 * 1000 }
        val result = WorkoutHistorySummaryBuilder.calculateWeeklyFrequency(sessions)
        assertEquals(2.0f, result!!, 0.01f)
    }

    @Test
    fun `calculateWeeklyFrequency only counts sessions within 28 days`() {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - 7L * 24 * 60 * 60 * 1000
        val sixWeeksAgo = now - 42L * 24 * 60 * 60 * 1000
        // Only the recent session should be counted → 1 / 4 = 0.25
        val result = WorkoutHistorySummaryBuilder.calculateWeeklyFrequency(listOf(oneWeekAgo, sixWeeksAgo))
        assertEquals(0.25f, result!!, 0.01f)
    }
}
