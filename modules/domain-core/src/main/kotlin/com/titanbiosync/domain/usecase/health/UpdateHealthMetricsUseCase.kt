package com.titanbiosync.domain.usecase.health

import com.titanbiosync.domain.model.HealthMetrics
import com.titanbiosync.domain.repository.HealthMetricsRepository
import com.titanbiosync.domain.usecase.UseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
/**
 * Use Case per aggiornare le metriche sanitarie giornaliere.
 * Se esistono già per quella data, le aggiorna; altrimenti le crea.
 */
class UpdateHealthMetricsUseCase (
    private val healthMetricsRepository: HealthMetricsRepository
) : UseCase<UpdateHealthMetricsUseCase.Params, HealthMetrics> {

    override suspend fun invoke(params: Params): HealthMetrics {
        val dateString = params.date?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val existing = healthMetricsRepository.observeByUserAndDate(params.userId, dateString).first()

        val metrics = if (existing != null) {
            // Aggiorna metriche esistenti
            existing.copy(
                restingHr = params.restingHr ?: existing.restingHr,
                hrv = params.hrv ?: existing.hrv,
                spo2Avg = params.spo2Avg ?: existing.spo2Avg,
                steps = params.steps ?: existing.steps,
                sleepSummaryJson = params.sleepSummaryJson ?: existing.sleepSummaryJson
            )
        } else {
            // Crea nuove metriche
            HealthMetrics(
                id = UUID.randomUUID().toString(),
                userId = params.userId,
                date = dateString,
                restingHr = params.restingHr,
                hrv = params.hrv,
                spo2Avg = params.spo2Avg,
                steps = params.steps,
                sleepSummaryJson = params.sleepSummaryJson
            )
        }

        healthMetricsRepository.upsert(metrics)
        return metrics
    }

    data class Params(
        val userId: String,
        val date: LocalDate? = null,
        val restingHr: Int? = null,
        val hrv: Float? = null,
        val spo2Avg: Float? = null,
        val steps: Int? = null,
        val sleepSummaryJson: String? = null
    )
}