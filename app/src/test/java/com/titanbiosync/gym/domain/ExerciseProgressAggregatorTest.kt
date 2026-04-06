package com.titanbiosync.gym.domain

import com.titanbiosync.data.local.analytics.ExerciseRawSetRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseProgressAggregatorTest {

    private fun row(sessionId: String, startedAt: Long, reps: Int, weightKg: Float) =
        ExerciseRawSetRow(sessionId, startedAt, reps, weightKg)

    @Test
    fun `aggregate returns empty list for empty input`() {
        val result = ExerciseProgressAggregator.aggregate(emptyList())
        assertEquals(emptyList<ExerciseSessionPoint>(), result)
    }

    @Test
    fun `aggregate groups sets by session and computes maxWeight`() {
        val rows = listOf(
            row("s1", 1000L, 5, 80f),
            row("s1", 1000L, 5, 100f),
            row("s2", 2000L, 8, 70f)
        )
        val result = ExerciseProgressAggregator.aggregate(rows)
        assertEquals(2, result.size)
        assertEquals(100f, result[0].maxWeightKg, 0.01f) // s1 max
        assertEquals(70f, result[1].maxWeightKg, 0.01f)  // s2 max
    }

    @Test
    fun `aggregate computes totalVolume correctly`() {
        val rows = listOf(
            row("s1", 1000L, 5, 80f),  // 400
            row("s1", 1000L, 3, 100f)  // 300 → total 700
        )
        val result = ExerciseProgressAggregator.aggregate(rows)
        assertEquals(1, result.size)
        assertEquals(700f, result[0].totalVolume, 0.01f)
    }

    @Test
    fun `aggregate computes bestE1rm for valid reps`() {
        val rows = listOf(row("s1", 1000L, 5, 80f))
        val result = ExerciseProgressAggregator.aggregate(rows)
        val expected = 80f * (1f + 5f / 30f)
        assertEquals(expected, result[0].bestE1rm!!, 0.01f)
    }

    @Test
    fun `aggregate returns null bestE1rm when all reps exceed limit`() {
        val rows = listOf(row("s1", 1000L, 20, 50f))
        val result = ExerciseProgressAggregator.aggregate(rows)
        assertNull(result[0].bestE1rm)
    }

    @Test
    fun `aggregate sorts results ascending by sessionDate`() {
        val rows = listOf(
            row("s3", 3000L, 5, 70f),
            row("s1", 1000L, 5, 90f),
            row("s2", 2000L, 5, 80f)
        )
        val result = ExerciseProgressAggregator.aggregate(rows)
        assertEquals(listOf(1000L, 2000L, 3000L), result.map { it.sessionDate })
    }

    @Test
    fun `lastN returns all entries when fewer than limit`() {
        val rows = listOf(
            row("s1", 1000L, 5, 80f),
            row("s2", 2000L, 5, 85f)
        )
        val result = ExerciseProgressAggregator.lastN(rows, 10)
        assertEquals(2, result.size)
    }

    @Test
    fun `lastN returns last N entries ordered ascending`() {
        val rows = (1..12).map { i -> row("s$i", i.toLong() * 1000, 5, (60 + i).toFloat()) }
        val result = ExerciseProgressAggregator.lastN(rows, 8)
        assertEquals(8, result.size)
        // Should be sessions 5..12 (last 8), ordered ascending
        assertEquals(5000L, result.first().sessionDate)
        assertEquals(12000L, result.last().sessionDate)
    }

    @Test
    fun `primaryValue returns bestE1rm when available`() {
        val point = ExerciseSessionPoint(
            sessionId = "s1",
            sessionDate = 1000L,
            maxWeightKg = 80f,
            bestE1rm = 90f,
            totalVolume = 400f
        )
        assertEquals(90f, point.primaryValue, 0.01f)
    }

    @Test
    fun `primaryValue falls back to maxWeightKg when bestE1rm is null`() {
        val point = ExerciseSessionPoint(
            sessionId = "s1",
            sessionDate = 1000L,
            maxWeightKg = 80f,
            bestE1rm = null,
            totalVolume = 400f
        )
        assertEquals(80f, point.primaryValue, 0.01f)
    }
}
