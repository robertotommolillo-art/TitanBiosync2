package com.titanbiosync.coach.data

import com.titanbiosync.coach.domain.ChatMessage
import com.titanbiosync.coach.domain.PreviousWorkoutPlanSummary
import com.titanbiosync.coach.domain.WorkoutHistorySummary
import com.titanbiosync.coach.domain.WorkoutPlan
import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.ExerciseMuscleDao
import com.titanbiosync.data.local.dao.gym.GymFolderDao
import com.titanbiosync.data.local.dao.gym.MuscleDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseSetDao
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseSetEntity
import com.titanbiosync.domain.model.CoachPrompt
import com.titanbiosync.domain.repository.CoachPromptRepository
import com.titanbiosync.domain.repository.UserRepository
import com.titanbiosync.ui.profile.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    private val templateExerciseSetDao: WorkoutTemplateExerciseSetDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseMuscleDao: ExerciseMuscleDao,
    private val muscleDao: MuscleDao,
    private val userRepository: UserRepository,
) {

    companion object {
        const val AI_FOLDER_NAME = "Schede da AI"
        private const val PREVIOUS_PLANS_CONTEXT_COUNT = 6
        private val REQUEST_STOPWORDS = setOf(
            "scheda", "allenamento", "allenamenti", "giorni", "giorno", "con", "senza", "per",
            "della", "delle", "degli", "dello", "dell", "dalla", "dalle", "gli", "una", "uno",
            "intermedio", "principiante", "avanzato", "massa", "ipertrofia", "forza", "dimagrimento",
            "cardio", "voglio", "vorrei", "programma", "routine", "settimana", "settimane", "home",
            "gym", "palestra"
        )
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
     * @return Pair of (assistant confirmation message, structured workout plans list).
     */
    suspend fun generateWorkout(userSpec: String, webSearch: Boolean = true): Pair<String, List<WorkoutPlan>> =
        withContext(Dispatchers.IO) {
            val history: WorkoutHistorySummary? =
                runCatching { historyBuilder.build() }.getOrNull()
            val localHints = runCatching { buildLocalExerciseHints(userSpec) }.getOrNull().orEmpty()
            val localNames = localHints.map { it.nameIt }
            val previousPlans = runCatching {
                buildPreviousWorkoutPlans(maxPlans = PREVIOUS_PLANS_CONTEXT_COUNT)
            }.getOrNull().orEmpty()
            val similarityThreshold = UserPreferences
                .fromJson(userRepository.getCurrentProfile()?.preferencesJson)
                .aiCoachSimilarityThreshold
                .coerceIn(40, 50)
            api.generateWorkout(
                userSpec = userSpec,
                history = history,
                webSearch = webSearch,
                localExerciseNames = localNames,
                localExerciseHints = localHints,
                previousPlans = previousPlans,
                similarityThreshold = similarityThreshold
            )
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
            goal = UserPreferences.fromJson(userRepository.getCurrentProfile()?.preferencesJson).goal,
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

        // Insert individual set rows (one per set) so the detail view can show series/reps
        val setEntities = plan.exercises.flatMapIndexed { exerciseIndex, planExercise ->
            (0 until planExercise.sets).map { setIndex ->
                WorkoutTemplateExerciseSetEntity(
                    templateId = templateId,
                    position = exerciseIndex,
                    setIndex = setIndex,
                    reps = planExercise.reps
                )
            }
        }
        if (setEntities.isNotEmpty()) {
            templateExerciseSetDao.upsertAll(setEntities)
        }

        templateId
    }

    /** Saves multiple generated plans and returns the created template IDs. */
    suspend fun saveWorkoutPlansToGym(plans: List<WorkoutPlan>): List<String> =
        withContext(Dispatchers.IO) {
            plans.map { saveWorkoutPlanToGym(it) }
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


    private suspend fun buildLocalExerciseHints(userSpec: String, maxHints: Int = 18): List<LocalExerciseHint> {
        val terms = extractSearchTerms(userSpec)
        val candidates = linkedMapOf<String, ExerciseEntity>()

        if (terms.isNotEmpty()) {
            for (term in terms) {
                val matches = exerciseDao.search(term, limit = 12).first()
                matches.forEach { ex ->
                    val name = ex.nameIt?.trim().orEmpty()
                    if (name.isNotBlank()) candidates.putIfAbsent(name.lowercase(), ex)
                }
            }
        }

        if (candidates.isEmpty()) {
            exerciseDao.getAllOnce()
                .asSequence()
                .filter { it.archivedAt == null }
                .sortedBy { it.nameIt ?: it.nameEn ?: it.id }
                .take(maxHints)
                .forEach { ex ->
                    val name = ex.nameIt?.trim().orEmpty()
                    if (name.isNotBlank()) candidates.putIfAbsent(name.lowercase(), ex)
                }
        }

        if (candidates.isEmpty()) return emptyList()

        val muscleNameById = muscleDao.getAllOnce().associate { it.id to it.nameIt }
        val linksByExercise = exerciseMuscleDao.getForExercises(candidates.values.map { it.id }).groupBy { it.exerciseId }

        return candidates.values
            .map { ex ->
                val linkedMuscles = linksByExercise[ex.id].orEmpty()
                val primaryMuscles = linkedMuscles
                    .filter { it.role.equals("primary", ignoreCase = true) }
                    .mapNotNull { muscleNameById[it.muscleId] }
                val secondaryMuscles = linkedMuscles
                    .filter { it.role.equals("secondary", ignoreCase = true) }
                    .mapNotNull { muscleNameById[it.muscleId] }
                LocalExerciseHint(
                    nameIt = ex.nameIt?.trim().orEmpty(),
                    equipment = ex.equipmentIt?.trim()?.takeIf { it.isNotBlank() } ?: ex.equipment?.trim()?.takeIf { it.isNotBlank() },
                    muscles = (primaryMuscles + secondaryMuscles).distinct().take(5)
                )
            }
            .filter { it.nameIt.isNotBlank() }
            .take(maxHints)
    }

    private suspend fun buildPreviousWorkoutPlans(
        maxPlans: Int = 6,
        maxExercisesPerPlan: Int = 18
    ): List<PreviousWorkoutPlanSummary> {
        val templates = templateDao.getAllOnce()
            .sortedByDescending { it.updatedAt }

        if (templates.isEmpty()) return emptyList()

        val summaries = mutableListOf<PreviousWorkoutPlanSummary>()
        for (template in templates) {
            val templateExercises = templateExerciseDao.getAllForTemplateOnce(template.id)
                .sortedBy { it.position }
            if (templateExercises.isEmpty()) continue

            val exerciseIds = templateExercises.map { it.exerciseId }.distinct()
            val exerciseNameById = exerciseDao.getByIdsOnce(exerciseIds)
                .associateBy(
                    keySelector = { it.id },
                    valueTransform = { it.nameIt?.trim().orEmpty().ifBlank { it.nameEn?.trim().orEmpty() } }
                )

            val exerciseNames = templateExercises
                .mapNotNull { exercise ->
                    val resolved = exerciseNameById[exercise.exerciseId].orEmpty().trim()
                    val fallback = exercise.exerciseId.trim()
                    (resolved.ifBlank { fallback }).takeIf { it.isNotBlank() }
                }
                .distinct()
                .take(maxExercisesPerPlan)

            if (exerciseNames.isEmpty()) continue

            summaries += PreviousWorkoutPlanSummary(
                title = template.name,
                exerciseNames = exerciseNames
            )

            if (summaries.size >= maxPlans) break
        }

        return summaries
    }

    private fun extractSearchTerms(userSpec: String, maxTerms: Int = 6): List<String> {
        return userSpec
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 && !REQUEST_STOPWORDS.contains(it) }
            .distinct()
            .take(maxTerms)
    }
}
