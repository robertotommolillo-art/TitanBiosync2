package com.titanbiosync.domain.usecase.location

import com.titanbiosync.domain.model.MapLocation
import kotlin.math.*

class CalculateRouteMetricsUseCase {

    operator fun invoke(locations: List<MapLocation>): RouteMetrics {
        if (locations.size < 2) {
            return RouteMetrics(
                distanceKm = 0.0,
                avgSpeedKmh = 0.0,
                maxSpeedKmh = 0.0,
                avgPaceMinKm = 0.0,
                elevationGainM = 0.0
            )
        }

        var totalDistance = 0.0
        var elevationGain = 0.0
        var maxSpeed = 0.0

        val sortedLocations = locations.sortedBy { it.timestamp }

        for (i in 1 until sortedLocations.size) {
            val prev = sortedLocations[i - 1]
            val curr = sortedLocations[i]

            // Calcola distanza tra due punti (formula Haversine)
            val distance = calculateDistance(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude
            )
            totalDistance += distance

            // Calcola elevazione
            if (prev.altitude != null && curr.altitude != null) {
                val elevDiff = curr.altitude - prev.altitude
                if (elevDiff > 0) {
                    elevationGain += elevDiff
                }
            }

            // Trova velocità massima
            curr.speed?.let { speed ->
                val speedKmh = speed * 3.6 // m/s to km/h
                if (speedKmh > maxSpeed) {
                    maxSpeed = speedKmh
                }
            }
        }

        val distanceKm = totalDistance / 1000.0

        // Calcola velocità media (basata su tempo totale)
        val durationSeconds = (sortedLocations.last().timestamp - sortedLocations.first().timestamp) / 1000.0
        val durationHours = durationSeconds / 3600.0
        val avgSpeedKmh = if (durationHours > 0) distanceKm / durationHours else 0.0

        // Calcola pace (min/km)
        val avgPaceMinKm = if (avgSpeedKmh > 0) 60.0 / avgSpeedKmh else 0.0

        return RouteMetrics(
            distanceKm = distanceKm,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeed,
            avgPaceMinKm = avgPaceMinKm,
            elevationGainM = elevationGain
        )
    }

    // Formula Haversine per calcolo distanza tra coordinate GPS
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusM = 6371000.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusM * c
    }

    data class RouteMetrics(
        val distanceKm: Double,
        val avgSpeedKmh: Double,
        val maxSpeedKmh: Double,
        val avgPaceMinKm: Double,
        val elevationGainM: Double
    )
}