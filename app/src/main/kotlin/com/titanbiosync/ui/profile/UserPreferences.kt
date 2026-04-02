package com.titanbiosync.ui.profile

/**
 * User onboarding preferences stored in [com.titanbiosync.domain.model.User.preferencesJson].
 *
 * The JSON format is a flat object versioned with the "v" key so future fields can be
 * added without breaking existing installations:
 *   {"v":1,"weightUnit":"kg","heightUnit":"cm","goal":"hypertrophy"}
 *
 * Unknown keys are silently ignored, missing keys fall back to defaults.
 */
data class UserPreferences(
    val weightUnit: String = WEIGHT_KG,
    val heightUnit: String = HEIGHT_CM,
    val goal: String? = null
) {
    companion object {
        const val WEIGHT_KG = "kg"
        const val WEIGHT_LB = "lb"

        const val HEIGHT_CM = "cm"
        const val HEIGHT_FT = "ft"

        const val GOAL_HYPERTROPHY = "hypertrophy"
        const val GOAL_STRENGTH = "strength"
        const val GOAL_FAT_LOSS = "fat_loss"

        val DEFAULT = UserPreferences()

        /** Deserialise from the JSON string stored in [User.preferencesJson]. Returns [DEFAULT] on any parse failure. */
        fun fromJson(json: String?): UserPreferences {
            if (json.isNullOrBlank()) return DEFAULT
            return try {
                val weightUnit = extractString(json, "weightUnit") ?: WEIGHT_KG
                val heightUnit = extractString(json, "heightUnit") ?: HEIGHT_CM
                val goal = extractString(json, "goal")
                UserPreferences(
                    weightUnit = if (weightUnit == WEIGHT_LB) WEIGHT_LB else WEIGHT_KG,
                    heightUnit = if (heightUnit == HEIGHT_FT) HEIGHT_FT else HEIGHT_CM,
                    goal = goal?.takeIf { it in listOf(GOAL_HYPERTROPHY, GOAL_STRENGTH, GOAL_FAT_LOSS) }
                )
            } catch (_: Exception) {
                DEFAULT
            }
        }

        /** Serialise to the JSON string to be stored in [User.preferencesJson]. */
        fun toJson(prefs: UserPreferences): String = buildString {
            append("{\"v\":1")
            append(",\"weightUnit\":\"${escapeJson(prefs.weightUnit)}\"")
            append(",\"heightUnit\":\"${escapeJson(prefs.heightUnit)}\"")
            if (prefs.goal != null) {
                append(",\"goal\":\"${escapeJson(prefs.goal)}\"")
            }
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
    }
}
