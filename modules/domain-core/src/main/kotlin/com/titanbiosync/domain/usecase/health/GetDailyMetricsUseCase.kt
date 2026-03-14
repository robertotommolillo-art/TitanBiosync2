package com.titanbiosync.domain.usecase.health

import com.titanbiosync.domain.model.HealthMetrics
import com.titanbiosync.domain.repository.HealthMetricsRepository
import com.titanbiosync.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Use Case per osservare le metriche sanitarie di un giorno specifico.
 */
class GetDailyMetricsUseCase (
    private val healthMetricsRepository: HealthMetricsRepository
) : FlowUseCase<GetDailyMetricsUseCase.Params, Flow<HealthMetrics?>> {

    override fun invoke(params: Params): Flow<HealthMetrics?> {
        val dateString = params.date?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        return healthMetricsRepository.observeByUserAndDate(params.userId, dateString)
    }

    data class Params(
        val userId: String,
        val date: LocalDate? = null // default: oggi
    )
}