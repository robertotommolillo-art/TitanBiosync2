package com.titanbiosync.domain.gym

/**
 * Computes estimated 1-Rep-Max (e1RM) using the Epley formula:
 *   e1RM = weight × (1 + reps / 30)
 *
 * Returns null when:
 * - reps ≤ 0 or weight ≤ 0 (invalid input)
 * - reps > MAX_REPS_FOR_E1RM (outside reliable range)
 */
object E1rmCalculator {

    /** Rep ceiling: above this value e1RM estimates are unreliable. */
    const val MAX_REPS_FOR_E1RM = 15

    /**
     * Calculates the estimated 1-Rep-Max.
     *
     * @param weightKg  weight lifted in kilograms (must be > 0)
     * @param reps      number of repetitions performed (must be in 1..[MAX_REPS_FOR_E1RM])
     * @return          e1RM in kilograms, or null if inputs are out of valid range
     */
    fun calculate(weightKg: Float, reps: Int): Float? {
        if (reps <= 0 || weightKg <= 0f) return null
        if (reps > MAX_REPS_FOR_E1RM) return null
        return weightKg * (1f + reps / 30f)
    }

    /**
     * Selects the best e1RM from a list of (weight, reps) pairs,
     * ignoring invalid entries.
     *
     * @param sets list of Pair(weightKg, reps)
     * @return the highest e1RM found, or null if none is valid
     */
    fun bestE1rm(sets: List<Pair<Float, Int>>): Float? {
        return sets.mapNotNull { (w, r) -> calculate(w, r) }.maxOrNull()
    }
}
