package com.titanbiosync.data.local.mappers

import com.titanbiosync.data.local.entities.*
import com.titanbiosync.domain.model.*

/** User */
fun UserEntity.toDomain() = User(
    id = id,
    externalId = externalId,
    email = email,
    displayName = displayName,
    firstName = firstName,
    lastName = lastName,
    age = age,
    height = height,
    sex = sex,
    avatarUri = avatarUri,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    updatedAt = updatedAt,
    privacyConsent = privacyConsent,
    preferencesJson = preferencesJson
)

fun User.toEntity() = UserEntity(
    id = id,
    externalId = externalId,
    email = email,
    displayName = displayName,
    firstName = firstName,
    lastName = lastName,
    age = age,
    height = height,
    sex = sex,
    avatarUri = avatarUri,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    updatedAt = updatedAt,
    privacyConsent = privacyConsent,
    preferencesJson = preferencesJson
)

/** Device */
fun DeviceEntity.toDomain() = Device(
    id = id,
    deviceId = deviceId,
    model = model,
    firmwareVersion = firmwareVersion,
    lastSeenAt = lastSeenAt,
    status = status,
    capabilitiesJson = capabilitiesJson
)

fun Device.toEntity() = DeviceEntity(
    id = id,
    deviceId = deviceId,
    model = model,
    firmwareVersion = firmwareVersion,
    lastSeenAt = lastSeenAt,
    status = status,
    capabilitiesJson = capabilitiesJson
)

/** SensorReading */
fun SensorReadingEntity.toDomain() = SensorReading(
    id = id,
    deviceId = deviceId,
    timestamp = timestamp,
    sensorType = sensorType,
    value = value,
    qualityScore = qualityScore,
    sessionId = sessionId
)

fun SensorReading.toEntity() = SensorReadingEntity(
    id = id,
    deviceId = deviceId,
    timestamp = timestamp,
    sensorType = sensorType,
    value = value,
    qualityScore = qualityScore,
    sessionId = sessionId
)

/** Session */
fun SessionEntity.toDomain() = Session(
    id = id,
    userId = userId,
    type = type,
    startedAt = startedAt,
    endedAt = endedAt,
    deviceIdsJson = deviceIdsJson,
    aggregatedMetricsJson = aggregatedMetricsJson,
    annotationsJson = annotationsJson
)

fun Session.toEntity() = SessionEntity(
    id = id,
    userId = userId,
    type = type,
    startedAt = startedAt,
    endedAt = endedAt,
    deviceIdsJson = deviceIdsJson,
    aggregatedMetricsJson = aggregatedMetricsJson,
    annotationsJson = annotationsJson
)

/** HealthMetrics */
fun HealthMetricsEntity.toDomain() = HealthMetrics(
    id = id,
    userId = userId,
    date = date,
    restingHr = restingHr,
    hrv = hrv,
    spo2Avg = spo2Avg,
    steps = steps,
    sleepSummaryJson = sleepSummaryJson
)

fun HealthMetrics.toEntity() = HealthMetricsEntity(
    id = id,
    userId = userId,
    date = date,
    restingHr = restingHr,
    hrv = hrv,
    spo2Avg = spo2Avg,
    steps = steps,
    sleepSummaryJson = sleepSummaryJson
)

/** Recommendation */
fun RecommendationEntity.toDomain() = Recommendation(
    id = id,
    userId = userId,
    createdAt = createdAt,
    source = source,
    contentJson = contentJson,
    confidence = confidence,
    status = status,
    relatedSessionId = relatedSessionId
)

fun Recommendation.toEntity() = RecommendationEntity(
    id = id,
    userId = userId,
    createdAt = createdAt,
    source = source,
    contentJson = contentJson,
    confidence = confidence,
    status = status,
    relatedSessionId = relatedSessionId
)

/** CoachPrompt */
fun CoachPromptEntity.toDomain() = CoachPrompt(
    id = id,
    userId = userId,
    promptText = promptText,
    responseText = responseText,
    modelVersion = modelVersion,
    tokensUsed = tokensUsed,
    timestamp = timestamp
)

fun CoachPrompt.toEntity() = CoachPromptEntity(
    id = id,
    userId = userId,
    promptText = promptText,
    responseText = responseText,
    modelVersion = modelVersion,
    tokensUsed = tokensUsed,
    timestamp = timestamp
)

/** MapLocation */
fun MapLocationEntity.toDomain() = MapLocation(
    id = id,
    sessionId = sessionId,
    latitude = lat,
    longitude = lon,
    altitude = altitude,
    speed = speed,
    bearing = bearing,
    accuracy = accuracy,
    timestamp = timestamp
)

fun MapLocation.toEntity() = MapLocationEntity(
    id = id,
    sessionId = sessionId,
    lat = latitude,
    lon = longitude,
    altitude = altitude,
    speed = speed,
    bearing = bearing,
    timestamp = timestamp,
    accuracy = accuracy
)

/** Consent */
fun ConsentRecordEntity.toDomain() = ConsentRecord(
    id = id,
    userId = userId,
    consentType = consentType,
    grantedAt = grantedAt,
    version = version
)

fun ConsentRecord.toEntity() = ConsentRecordEntity(
    id = id,
    userId = userId,
    consentType = consentType,
    grantedAt = grantedAt,
    version = version
)

/** AppConfig */
fun AppConfigEntity.toDomain() = com.titanbiosync.domain.model.AppConfig(
    key = key,
    valueJson = valueJson
)

fun com.titanbiosync.domain.model.AppConfig.toEntity() = AppConfigEntity(
    key = key,
    valueJson = valueJson
)