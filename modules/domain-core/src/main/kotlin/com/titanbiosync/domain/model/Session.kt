package com.titanbiosync.domain.model

data class Session(
    val id: String,
    val userId: String,
    val type: String,
    val startedAt: Long,
    val endedAt: Long?,
    val deviceIdsJson: String?,
    val aggregatedMetricsJson: String?,
    val annotationsJson: String?
)