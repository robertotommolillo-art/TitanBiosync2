package com.titanbiosync.domain.gym

/**
 * Canonical strength-lift families used for PR tracking.
 * Variants are matched via name-based heuristics; users never configure this manually.
 */
enum class StrengthFamily {
    BENCH,
    SQUAT,
    DEADLIFT,
    OHP
}

/**
 * Maps an exercise name (Italian or English) to a [StrengthFamily] using keyword heuristics.
 *
 * Matching is case-insensitive and checks substrings in priority order so that
 * more-specific patterns (e.g. "stacco sumo") are checked before generic ones.
 *
 * Returns null when no known family matches.
 */
object StrengthFamilyResolver {

    // Each entry: list of keywords → StrengthFamily.
    // Keywords are matched as substrings of the lowercased exercise name.
    private val RULES: List<Pair<List<String>, StrengthFamily>> = listOf(
        // --- DEADLIFT variants (check before SQUAT/OHP to avoid cross-match) ---
        listOf(
            "deadlift", "stacco", "romanian", "rdl",
            "sumo dead", "trap bar", "hex bar", "stacco sumo", "stacco romeno",
            "stacco trap"
        ) to StrengthFamily.DEADLIFT,

        // --- SQUAT variants ---
        listOf(
            "squat", "front squat", "high bar", "low bar",
            "paused squat", "back squat",
            "squat frontale", "squat bulgaro", "bulgarian split squat",
            "goblet squat", "zercher squat", "hack squat"
        ) to StrengthFamily.SQUAT,

        // --- BENCH variants ---
        listOf(
            "bench press", "panca piana", "panca inclinata", "panca declinata",
            "incline bench", "decline bench", "close grip bench",
            "paused bench", "chest press", "dumbbell press",  // db flat press
            "manubri piana", "manubri inclinata", "manubri declinata",
            "floor press"
        ) to StrengthFamily.BENCH,

        // --- OHP variants ---
        listOf(
            "overhead press", "ohp", "shoulder press", "military press",
            "push press", "press in piedi", "lento avanti",
            "seated press", "arnold press", "dumbbell shoulder press",
            "manubri spalle", "seated dumbbell press"
        ) to StrengthFamily.OHP
    )

    /**
     * Resolves the [StrengthFamily] from an exercise name.
     *
     * @param name exercise name in any language
     * @return matched family or null
     */
    fun resolve(name: String): StrengthFamily? {
        val lower = name.trim().lowercase()
        for ((keywords, family) in RULES) {
            if (keywords.any { lower.contains(it) }) return family
        }
        return null
    }
}
