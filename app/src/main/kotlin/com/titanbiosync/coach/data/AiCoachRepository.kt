package com.titanbiosync.coach.data

import com.titanbiosync.coach.domain.ChatMessage
import com.titanbiosync.coach.domain.WorkoutHistorySummary
import com.titanbiosync.coach.domain.WorkoutPlan
import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.GymFolderDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseEntity
import com.titanbiosync.domain.model.CoachPrompt
import com.titanbiosync.domain.repository.CoachPromptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates all AI Coach operations:
 *  - Chat with the coach via the backend proxy.
 *  - Generate a workout plan and convert it to Room entities.
 *  - Save generated plans into the "Schede da AI" Gym folder.
 */
@Singleton
class AiCoachRepository @Inject constructor(
    private val api: AiCoachApi,
    private val historyBuilder: WorkoutHistorySummaryBuilder,
    private val coachPromptRepository: CoachPromptRepository,
    private val folderDao: GymFolderDao,
    private val templateDao: WorkoutTemplateDao,
    private val templateExerciseDao: WorkoutTemplateExerciseDao,
    private val exerciseDao: ExerciseDao,
) {

    companion object {
        const val AI_FOLDER_NAME = "Schede da AI"
    }

    // ── Chat ───────────────────────────────────────────────────────────────

    /**
     * Sends a message to the AI Coach and returns the assistant's reply.
     * Persists both the user message and assistant reply to Room.
     *
     * @param userId        Local user ID for prompt persistence.
     * @param messages      Full conversation history (oldest first).
     * @return Assistant reply text.
     */
    suspend fun chat(
        userId: String,
        messages: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val history = runCatching { historyBuilder.build() }.getOrNull()
        val reply = api.chat(messages, history)

        // Persist the last user message + reply
        val lastUserMsg = messages.lastOrNull { it.role == "user" }
        if (lastUserMsg != null) {
            coachPromptRepository.insert(
                CoachPrompt(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    promptText = lastUserMsg.content,
                    responseText = reply,
                    modelVersion = null,
                    tokensUsed = null,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        reply
    }

    // ── Generate workout ───────────────────────────────────────────────────

    /**
     * Generates a structured workout plan from the user's specification.
     *
     * @param userSpec  Natural language description of the desired plan.
     * @return Pair of (assistant confirmation message, structured [WorkoutPlan]).
     */
    suspend fun generateWorkout(userSpec: String): Pair<String, WorkoutPlan> =
        withContext(Dispatchers.IO) {
            val history: WorkoutHistorySummary? =
                runCatching { historyBuilder.build() }.getOrNull()
            api.generateWorkout(userSpec, history)
        }

    // ── Save to Gym ────────────────────────────────────────────────────────

    /**
     * Saves a [WorkoutPlan] into the Gym as a workout template inside the "Schede da AI" folder.
     * Creates the folder if it does not exist.
     *
     * Exercises are matched by Italian name. If an exercise is not found, it is created
     * as a custom exercise so the template is never left incomplete.
     *
     * @param plan  The workout plan to save.
     * @return The ID of the newly created workout template.
     */
    suspend fun saveWorkoutPlanToGym(plan: WorkoutPlan): String = withContext(Dispatchers.IO) {
        val folderId = getOrCreateAiFolder()
        val templateId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Determine sort index for the new template
        val sortIndex = (templateDao.getMaxSortIndexForFolder(folderId) ?: -1) + 1

        val template = WorkoutTemplateEntity(
            id = templateId,
            folderId = folderId,
            name = plan.title,
            notes = plan.notes,
            sortIndex = sortIndex,
            createdAt = now,
            updatedAt = now
        )
        templateDao.upsert(template)

        // Insert exercises
        val exerciseEntities = plan.exercises.mapIndexed { index, planExercise ->
            val exerciseId = resolveOrCreateExercise(planExercise.nameIt)
            WorkoutTemplateExerciseEntity(
                templateId = templateId,
                position = index,
                exerciseId = exerciseId,
                targetSets = planExercise.sets,
                targetReps = planExercise.reps,
                restSeconds = planExercise.restSeconds,
                notes = planExercise.notes
            )
        }
        if (exerciseEntities.isNotEmpty()) {
            templateExerciseDao.upsertAll(exerciseEntities)
        }

        templateId
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Returns the ID of the "Schede da AI" folder, creating it if absent.
     */
    private suspend fun getOrCreateAiFolder(): String {
        val existing = folderDao.getByNameOnce(AI_FOLDER_NAME)
        if (existing != null) return existing.id

        val folderId = UUID.randomUUID().toString()
        val sortIndex = (folderDao.getMaxSortIndex() ?: -1) + 1
        folderDao.upsert(
            GymFolderEntity(
                id = folderId,
                name = AI_FOLDER_NAME,
                sortIndex = sortIndex,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        return folderId
    }

    /**
     * Finds an active exercise by Italian name (case-insensitive).
     * If not found, creates a custom exercise and returns its ID.
     */
    private suspend fun resolveOrCreateExercise(nameIt: String): String {
        val existing = exerciseDao.findByNameIt(nameIt)
        if (existing != null) return existing.id

        val newId = UUID.randomUUID().toString()
        exerciseDao.upsert(
            ExerciseEntity(
                id = newId,
                nameIt = nameIt,
                nameEn = nameIt,
                category = "bodybuilding",
                isCustom = true,
                createdAt = System.currentTimeMillis()
            )
        )
        return newId
    }
}
