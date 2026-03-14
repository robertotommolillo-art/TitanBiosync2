package com.titanbiosync.domain.usecase.session

import com.titanbiosync.domain.repository.SensorRepository
import com.titanbiosync.domain.repository.SessionRepository
import com.titanbiosync.domain.repository.MapLocationRepository
import com.titanbiosync.domain.usecase.location.CalculateRouteMetricsUseCase
import kotlinx.coroutines.flow.first

class GetSessionSummaryUseCase(
    private val sessionRepository: SessionRepository,
    private val sensorRepository: SensorRepository,
    private val mapLocationRepository: MapLocationRepository? = null,
    private val calculateRouteMetricsUseCase: CalculateRouteMetricsUseCase? = null
) {
    suspend operator fun invoke(sessionId: String): Result {
        val session = sessionRepository.observeById(sessionId).first()
            ?: return Result.Error("Session not found")

        val sensorReadings = sensorRepository.observeBySession(sessionId).first()

        val hrReadings = sensorReadings
            .filter { it.sensorType == "heart_rate" }
            .mapNotNull { it.value.toIntOrNull() }

        if (hrReadings.isEmpty()) {
            // Even if no HR, return success if we have other data (like location) or just basics
            // return Result.Error("No heart rate data available")
        }

        val avgHr = if (hrReadings.isNotEmpty()) hrReadings.average().toInt() else 0
        val minHr = hrReadings.minOrNull() ?: 0
        val maxHr = hrReadings.maxOrNull() ?: 0

        val maxTheoreticalHr = 190f
        val zones = calculateHeartRateZones(hrReadings, maxTheoreticalHr)

        val durationMs = (session.endedAt ?: System.currentTimeMillis()) - session.startedAt
        val durationHours = durationMs / 1000f / 3600f
        val avgMETs = estimateMETs(session.type, avgHr)
        val weightKg = 75f
        val calories = (avgMETs * weightKg * durationHours).toInt()

        // Calculate GPS metrics if available
        var distanceKm: Double? = null
        var avgSpeedKmh: Double? = null
        var maxSpeedKmh: Double? = null
        var avgPaceMinKm: Double? = null
        var elevationGainM: Double? = null

        if (mapLocationRepository != null && calculateRouteMetricsUseCase != null) {
            val locations = mapLocationRepository.observeBySession(sessionId).first()
            if (locations.isNotEmpty()) {
                val metrics = calculateRouteMetricsUseCase.invoke(locations) // Fixed nullable call
                distanceKm = metrics.distanceKm
                avgSpeedKmh = metrics.avgSpeedKmh
                maxSpeedKmh = metrics.maxSpeedKmh
                avgPaceMinKm = metrics.avgPaceMinKm
                elevationGainM = metrics.elevationGainM
            }
        }

        return Result.Success(
            sessionId = sessionId,
            sessionType = session.type,
            startTime = session.startedAt,
            endTime = session.endedAt ?: System.currentTimeMillis(),
            durationMs = durationMs,
            totalDataPoints = sensorReadings.size,
            avgHr = avgHr,
            minHr = minHr,
            maxHr = maxHr,
            zones = zones,
            calories = calories,
            distanceKm = distanceKm,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            avgPaceMinKm = avgPaceMinKm,
            elevationGainM = elevationGainM
        )
    }

    private fun calculateHeartRateZones(hrReadings: List<Int>, maxHr: Float): Zones {
        val total = hrReadings.size.toFloat()
        if (total == 0f) {
            return Zones(0f, 0f, 0f, 0f, 0f)
        }

        val zone1 = hrReadings.count { it < maxHr * 0.6f }
        val zone2 = hrReadings.count { it in (maxHr * 0.6f).toInt() until (maxHr * 0.7f).toInt() }
        val zone3 = hrReadings.count { it in (maxHr * 0.7f).toInt() until (maxHr * 0.8f).toInt() }
        val zone4 = hrReadings.count { it in (maxHr * 0.8f).toInt() until (maxHr * 0.9f).toInt() }
        val zone5 = hrReadings.count { it >= (maxHr * 0.9f).toInt() }

        return Zones(
            zone1Percent = (zone1 / total) * 100f,
            zone2Percent = (zone2 / total) * 100f,
            zone3Percent = (zone3 / total) * 100f,
            zone4Percent = (zone4 / total) * 100f,
            zone5Percent = (zone5 / total) * 100f
        )
    }

    private fun estimateMETs(sessionType: String, avgHr: Int): Float {
        return when (sessionType.lowercase()) {
            "running" -> when {
                avgHr < 120 -> 6f
                avgHr < 150 -> 9f
                else -> 12f
            }
            "cycling" -> when {
                avgHr < 120 -> 4f
                avgHr < 150 -> 7f
                else -> 10f
            }
            "workout" -> when {
                avgHr < 120 -> 3f
                avgHr < 150 -> 5f
                else -> 8f
            }
            else -> 4f
        }
    }

    sealed class Result {
        data class Success(
            val sessionId: String,
            val sessionType: String,
            val startTime: Long,
            val endTime: Long,
            val durationMs: Long,
            val totalDataPoints: Int,
            val avgHr: Int,
            val minHr: Int,
            val maxHr: Int,
            val zones: Zones,
            val calories: Int,
            val distanceKm: Double? = null,
            val avgSpeedKmh: Double? = null,
            val maxSpeedKmh: Double? = null,
            val avgPaceMinKm: Double? = null,
            val elevationGainM: Double? = null
        ) : Result()

        data class Error(val message: String) : Result()
    }

    data class Zones(
        val zone1Percent: Float,
        val zone2Percent: Float,
        val zone3Percent: Float,
        val zone4Percent: Float,
        val zone5Percent: Float
    )
}