package com.titanbiosync.ui.profile

/**
 * User onboarding preferences stored in the current user preferences JSON.
 *
 * The JSON format is a flat object versioned with the "v" key so future fields can be
 * added without breaking existing installations:
 *   {"v":1,"weightUnit":"kg","heightUnit":"cm","goal":"hypertrophy","themeId":"default","aiCoachIntroShown":true,"aiCoachPostWorkoutPromptShown":true}
 *
 * Unknown keys are silently ignored, missing keys fall back to defaults.
 */
data class UserPreferences(
    val weightUnit: String = WEIGHT_KG,
    val heightUnit: String = HEIGHT_CM,
    val goal: String? = null,
    val themeId: String = THEME_DEFAULT,
    val aiCoachIntroShown: Boolean = false,
    val aiCoachPostWorkoutPromptShown: Boolean = false,
    val aiCoachSimilarityThreshold: Int = 45
) {
    companion object {
        const val WEIGHT_KG = "kg"
        const val WEIGHT_LB = "lb"

        const val HEIGHT_CM = "cm"
        const val HEIGHT_FT = "ft"

        const val GOAL_HYPERTROPHY = "hypertrophy"
        const val GOAL_STRENGTH = "strength"
        const val GOAL_FAT_LOSS = "fat_loss"

        const val THEME_DEFAULT = "default"
        const val THEME_CYBER = "cyber"
        const val THEME_CRIMSON = "crimson"
        const val THEME_FOREST = "forest"

        val DEFAULT = UserPreferences()

        /** Deserialise from the stored user preferences JSON. Returns [DEFAULT] on any parse failure. */
        fun fromJson(json: String?): UserPreferences {
            if (json.isNullOrBlank()) return DEFAULT
            return try {
                val weightUnit = extractString(json, "weightUnit") ?: WEIGHT_KG
                val heightUnit = extractString(json, "heightUnit") ?: HEIGHT_CM
                val goal = extractString(json, "goal")
                val themeId = extractString(json, "themeId") ?: THEME_DEFAULT
                val aiCoachIntroShown = extractBoolean(json, "aiCoachIntroShown") ?: false
                val aiCoachPostWorkoutPromptShown = extractBoolean(json, "aiCoachPostWorkoutPromptShown") ?: false
                val aiCoachSimilarityThreshold = extractInt(json, "aiCoachSimilarityThreshold") ?: 45
                UserPreferences(
                    weightUnit = if (weightUnit == WEIGHT_LB) WEIGHT_LB else WEIGHT_KG,
                    heightUnit = if (heightUnit == HEIGHT_FT) HEIGHT_FT else HEIGHT_CM,
                    goal = goal?.takeIf {
                        it in listOf(GOAL_HYPERTROPHY, GOAL_STRENGTH, GOAL_FAT_LOSS)
                    },
                    themeId = themeId,
                    aiCoachIntroShown = aiCoachIntroShown,
                    aiCoachPostWorkoutPromptShown = aiCoachPostWorkoutPromptShown,
                    aiCoachSimilarityThreshold = aiCoachSimilarityThreshold.coerceIn(40, 50)
                )
            } catch (_: Exception) {
                DEFAULT
            }
        }

        /** Serialise to the JSON string to be stored in the user preferences field. */
        fun toJson(prefs: UserPreferences): String = buildString {
            append("{\"v\":1")
            append(",\"weightUnit\":\"${escapeJson(prefs.weightUnit)}\"")
            append(",\"heightUnit\":\"${escapeJson(prefs.heightUnit)}\"")
            if (prefs.goal != null) {
                append(",\"goal\":\"${escapeJson(prefs.goal)}\"")
            }
            append(",\"themeId\":\"${escapeJson(prefs.themeId)}\"")
            append(",\"aiCoachIntroShown\":${prefs.aiCoachIntroShown}")
            append(",\"aiCoachPostWorkoutPromptShown\":${prefs.aiCoachPostWorkoutPromptShown}")
            append(",\"aiCoachSimilarityThreshold\":${prefs.aiCoachSimilarityThreshold.coerceIn(40, 50)}")
            append("}")
        }

        private fun escapeJson(value: String): String =
            value.replace("\\", "\\\\").replace("\"", "\\\"")

        private fun extractString(json: String, key: String): String? {
            val escapedKey = Regex.escape(key)
            val pattern = "\"$escapedKey\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
            val regex = Regex(pattern)
            val match = regex.find(json) ?: return null
            return match.groupValues[1]
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
        }

        private fun extractBoolean(json: String, key: String): Boolean? {
            val escapedKey = Regex.escape(key)
            return Regex("\"$escapedKey\"\\s*:\\s*(true|false)")
                .find(json)
                ?.groupValues
                ?.getOrNull(1)
                ?.toBooleanStrictOrNull()
        }

        private fun extractInt(json: String, key: String): Int? {
            val escapedKey = Regex.escape(key)
            return Regex("\"$escapedKey\"\\s*:\\s*(\\d+)")
                .find(json)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
        }
    }
}
