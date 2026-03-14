package com.titanbiosync.ui.session

data class SessionSummary(
    val sessionId: String,
    val sessionType: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val totalDataPoints: Int,
    val heartRateStats: HeartRateStats,
    val caloriesBurned: Int,
    val distance: Float? = null, // km (se GPS attivo)
    val avgSpeed: Float? = null // km/h (se GPS attivo)
) {
    val durationMinutes: Int
        get() = (durationMs / 1000 / 60).toInt()

    val durationSeconds: Int
        get() = (durationMs / 1000 % 60).toInt()

    val formattedDuration: String
        get() = String.format("%02d:%02d", durationMinutes, durationSeconds)
}

data class HeartRateStats(
    val average: Int,
    val min: Int,
    val max: Int,
    val zone1Percent: Float, // < 60% max HR (Very Light)
    val zone2Percent: Float, // 60-70% (Light)
    val zone3Percent: Float, // 70-80% (Moderate)
    val zone4Percent: Float, // 80-90% (Hard)
    val zone5Percent: Float  // 90-100% (Maximum)
)