package com.titanbiosync.coach.data

import com.titanbiosync.coach.domain.ChatMessage
import com.titanbiosync.coach.domain.PreviousWorkoutPlanSummary
import com.titanbiosync.coach.domain.WorkoutHistorySummary
import com.titanbiosync.coach.domain.WorkoutPlan
import com.titanbiosync.coach.domain.WorkoutPlanExercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the AI Coach backend proxy.
 *
 * Uses [HttpURLConnection] (no additional library dependencies).
 * JSON encoding/decoding uses [kotlinx.serialization].
 */
class AiCoachApi(
    baseUrl: String,
    appToken: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
    private val normalizedAppToken = appToken.trim()

    // ── Serializable DTOs ──────────────────────────────────────────────────

    @Serializable
    private data class ChatMessageDto(val role: String, val content: String)

    @Serializable
    private data class RecentSessionDto(
        val templateName: String,
        val date: String,
        val durationMin: Int,
        val totalVolumeKg: Float
    )

    @Serializable
    private data class ExercisePrDto(
        val exerciseName: String,
        val maxWeightKg: Float,
        val maxE1rm: Float
    )

    @Serializable
    private data class HistorySummaryDto(
        val recentSessions: List<RecentSessionDto> = emptyList(),
        val topPrs: List<ExercisePrDto> = emptyList(),
        val weeklyFrequency: Float? = null
    )

    @Serializable
    private data class ChatRequestDto(
        val messages: List<ChatMessageDto>,
        val history: HistorySummaryDto? = null
    )

    @Serializable
    private data class GenerateWorkoutRequestDto(
        val userSpec: String,
        val history: HistorySummaryDto? = null,
        val webSearch: Boolean = true,
        val localExerciseNames: List<String>? = null,
        val localExerciseHints: List<LocalExerciseHint>? = null,
        val previousPlans: List<PreviousWorkoutPlanDto>? = null,
        val similarityThreshold: Int = 45
    )

    @Serializable
    private data class PreviousWorkoutPlanDto(
        val title: String,
        val exerciseNames: List<String>
    )

    @Serializable
    private data class ChatResponseDto(
        val message: String,
        val tokensUsed: Int = 0
    )

    @Serializable
    private data class GenerateWorkoutResponseDto(
        val message: String,
        val plan: JsonObject? = null,
        val plans: JsonArray? = null,
        val tokensUsed: Int = 0
    )

    @Serializable
    private data class WorkoutPlanDto(
        val title: String,
        val notes: String? = null,
        val exercises: List<WorkoutPlanExerciseDto> = emptyList()
    )

    @Serializable
    private data class WorkoutPlanExerciseDto(
        val nameIt: String,
        val sets: Int,
        val reps: Int,
        val restSeconds: Int? = null,
        val notes: String? = null
    )

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Sends a chat message to the AI Coach.
     *
     * @param messages    Conversation history (oldest first).
     * @param history     Optional workout history summary.
     * @return The assistant's reply text.
     * @throws IOException on network error.
     * @throws AiCoachException on HTTP or API error.
     */
    fun chat(
        messages: List<ChatMessage>,
        history: WorkoutHistorySummary?
    ): String {
        val requestDto = ChatRequestDto(
            messages = messages.map { ChatMessageDto(it.role, it.content) },
            history = history?.toDto()
        )
        val responseBody = post("/coach/chat", json.encodeToString(requestDto))
        val response = json.decodeFromString<ChatResponseDto>(responseBody)
        return response.message
    }

    /**
     * Requests the AI Coach to generate a workout plan.
     *
     * @param userSpec  Natural language description of the desired workout.
     * @param history   Optional workout history summary.
     * @return Pair of (assistant message text, structured workout plans list).
     * @throws IOException on network error.
     * @throws AiCoachException on HTTP or API error.
     */
    fun generateWorkout(
        userSpec: String,
        history: WorkoutHistorySummary?,
        webSearch: Boolean = true,
        localExerciseNames: List<String>? = null,
        localExerciseHints: List<LocalExerciseHint>? = null,
        previousPlans: List<PreviousWorkoutPlanSummary>? = null,
        similarityThreshold: Int = 45
    ): Pair<String, List<WorkoutPlan>> {
        val requestDto = GenerateWorkoutRequestDto(
            userSpec = userSpec,
            history = history?.toDto(),
            webSearch = webSearch,
            localExerciseNames = localExerciseNames?.take(80),
            localExerciseHints = localExerciseHints?.take(18),
            previousPlans = previousPlans
                ?.take(6)
                ?.map { it.toDto() },
            similarityThreshold = similarityThreshold.coerceIn(40, 50)
        )
        val responseBody = post("/coach/generate-workout", json.encodeToString(requestDto))
        val response = json.decodeFromString<GenerateWorkoutResponseDto>(responseBody)

        val parsedPlans = response.plans
            ?.mapNotNull { element ->
                runCatching { json.decodeFromJsonElement<WorkoutPlanDto>(element) }.getOrNull()
            }
            .orEmpty()

        val fallbackPlan = response.plan
            ?.let { runCatching { json.decodeFromJsonElement<WorkoutPlanDto>(it) }.getOrNull() }

        val planDtos = when {
            parsedPlans.isNotEmpty() -> parsedPlans
            fallbackPlan != null -> listOf(fallbackPlan)
            else -> listOf(WorkoutPlanDto(title = "Scheda AI", exercises = emptyList()))
        }

        return response.message to planDtos.map { it.toDomain() }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun WorkoutHistorySummary.toDto() = HistorySummaryDto(
        recentSessions = recentSessions.map {
            RecentSessionDto(it.templateName, it.date, it.durationMin, it.totalVolumeKg)
        },
        topPrs = topPrs.map {
            ExercisePrDto(it.exerciseName, it.maxWeightKg, it.maxE1rm)
        },
        weeklyFrequency = weeklyFrequency
    )

    private fun WorkoutPlanDto.toDomain() = WorkoutPlan(
        title = title,
        notes = notes,
        exercises = exercises.map {
            WorkoutPlanExercise(
                nameIt = it.nameIt,
                sets = it.sets,
                reps = it.reps,
                restSeconds = it.restSeconds,
                notes = it.notes
            )
        }
    )

    private fun PreviousWorkoutPlanSummary.toDto() = PreviousWorkoutPlanDto(
        title = title,
        exerciseNames = exerciseNames.take(18)
    )

    /**
     * Performs an HTTP POST with JSON body, returns the response body as a string.
     */
    private fun post(path: String, body: String): String {
        if (normalizedAppToken.isBlank()) {
            throw IllegalStateException(
                "COACH_APP_TOKEN non configurato: imposta COACH_APP_TOKEN in Gradle o variabile ambiente"
            )
        }

        val url = URL("$normalizedBaseUrl$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
        conn.readTimeout = TimeUnit.SECONDS.toMillis(45).toInt()
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("X-App-Token", normalizedAppToken)
        conn.doOutput = true

        conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

        val statusCode = conn.responseCode
        val responseBody = if (statusCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            val errorBody = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            throw AiCoachException(statusCode, errorBody)
        }
        conn.disconnect()
        return responseBody
    }
}

/** Thrown when the backend returns a non-2xx HTTP status code. */
class AiCoachException(val statusCode: Int, val body: String) :
    IOException("Coach API error $statusCode: $body")
