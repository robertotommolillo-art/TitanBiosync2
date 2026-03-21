package com.titanbiosync.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class E1rmCalculatorTest {

    // ---- Basic Epley formula ----

    @Test
    fun `calculate returns correct e1rm for 1 rep`() {
        // 1 rep → e1RM = weight × (1 + 1/30)
        val result = E1rmCalculator.calculate(100f, 1)
        assertNotNull(result)
        assertEquals(100f * (1f + 1f / 30f), result!!, 0.01f)
    }

    @Test
    fun `calculate returns correct e1rm for 5 reps`() {
        val result = E1rmCalculator.calculate(80f, 5)
        assertNotNull(result)
        assertEquals(80f * (1f + 5f / 30f), result!!, 0.01f)
    }

    @Test
    fun `calculate returns correct e1rm for 10 reps`() {
        val result = E1rmCalculator.calculate(60f, 10)
        assertNotNull(result)
        assertEquals(60f * (1f + 10f / 30f), result!!, 0.01f)
    }

    // ---- Edge cases at boundary ----

    @Test
    fun `calculate returns value at max reps boundary`() {
        val result = E1rmCalculator.calculate(50f, E1rmCalculator.MAX_REPS_FOR_E1RM)
        assertNotNull(result)
    }

    @Test
    fun `calculate returns null for reps above max`() {
        val result = E1rmCalculator.calculate(50f, E1rmCalculator.MAX_REPS_FOR_E1RM + 1)
        assertNull(result)
    }

    // ---- Invalid inputs ----

    @Test
    fun `calculate returns null when reps is zero`() {
        assertNull(E1rmCalculator.calculate(100f, 0))
    }

    @Test
    fun `calculate returns null when reps is negative`() {
        assertNull(E1rmCalculator.calculate(100f, -1))
    }

    @Test
    fun `calculate returns null when weight is zero`() {
        assertNull(E1rmCalculator.calculate(0f, 5))
    }

    @Test
    fun `calculate returns null when weight is negative`() {
        assertNull(E1rmCalculator.calculate(-10f, 5))
    }

    // ---- bestE1rm ----

    @Test
    fun `bestE1rm returns null for empty list`() {
        assertNull(E1rmCalculator.bestE1rm(emptyList()))
    }

    @Test
    fun `bestE1rm ignores invalid entries`() {
        val sets = listOf(Pair(0f, 5), Pair(100f, 0), Pair(-10f, 3))
        assertNull(E1rmCalculator.bestE1rm(sets))
    }

    @Test
    fun `bestE1rm returns highest e1rm`() {
        val sets = listOf(
            Pair(100f, 1),   // e1RM ≈ 103.3
            Pair(80f, 5),    // e1RM ≈ 93.3
            Pair(60f, 10)    // e1RM ≈ 80
        )
        val best = E1rmCalculator.bestE1rm(sets)
        assertNotNull(best)
        // 100 × (1 + 1/30) = 103.33...
        assertEquals(100f * (1f + 1f / 30f), best!!, 0.01f)
    }

    @Test
    fun `bestE1rm ignores sets above max reps`() {
        val sets = listOf(
            Pair(50f, 20), // invalid - above max
            Pair(40f, 10)  // valid
        )
        val best = E1rmCalculator.bestE1rm(sets)
        assertNotNull(best)
        assertEquals(40f * (1f + 10f / 30f), best!!, 0.01f)
    }
}
