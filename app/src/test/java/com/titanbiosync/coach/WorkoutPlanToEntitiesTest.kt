package com.titanbiosync.coach

import com.titanbiosync.coach.domain.WorkoutPlan
import com.titanbiosync.coach.domain.WorkoutPlanExercise
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the logic that converts a [WorkoutPlan] to Room entities.
 * These mirror the conversion logic in [com.titanbiosync.coach.data.AiCoachRepository.saveWorkoutPlanToGym].
 */
class WorkoutPlanToEntitiesTest {

    // ── Helper: simulate entity creation ───────────────────────────────────

    private fun planToTemplateEntity(plan: WorkoutPlan, folderId: String, sortIndex: Int): WorkoutTemplateEntity {
        val now = 0L
        return WorkoutTemplateEntity(
            id = "template-id",
            folderId = folderId,
            name = plan.title,
            notes = plan.notes,
            sortIndex = sortIndex,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun planToExerciseEntities(
        plan: WorkoutPlan,
        templateId: String,
        exerciseIdMap: Map<String, String>
    ): List<WorkoutTemplateExerciseEntity> {
        return plan.exercises.mapIndexed { index, exercise ->
            WorkoutTemplateExerciseEntity(
                templateId = templateId,
                position = index,
                exerciseId = exerciseIdMap[exercise.nameIt] ?: "unknown",
                targetSets = exercise.sets,
                targetReps = exercise.reps,
                restSeconds = exercise.restSeconds,
                notes = exercise.notes
            )
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test
    fun `template entity has correct name and folderId`() {
        val plan = WorkoutPlan(title = "Push Day A", notes = "Recupero 90s", exercises = emptyList())
        val entity = planToTemplateEntity(plan, folderId = "folder-ai", sortIndex = 0)

        assertEquals("Push Day A", entity.name)
        assertEquals("folder-ai", entity.folderId)
        assertEquals("Recupero 90s", entity.notes)
        assertEquals(0, entity.sortIndex)
    }

    @Test
    fun `template entity has null notes when plan has no notes`() {
        val plan = WorkoutPlan(title = "Leg Day", notes = null, exercises = emptyList())
        val entity = planToTemplateEntity(plan, folderId = "folder-ai", sortIndex = 1)

        assertNull(entity.notes)
        assertEquals(1, entity.sortIndex)
    }

    @Test
    fun `exercise entities are created with correct position and sets`() {
        val exercises = listOf(
            WorkoutPlanExercise("Panca Piana", sets = 4, reps = 8, restSeconds = 90, notes = null),
            WorkoutPlanExercise("Shoulder Press", sets = 3, reps = 10, restSeconds = 75, notes = null),
            WorkoutPlanExercise("Tricep Dip", sets = 3, reps = 12, restSeconds = 60, notes = null)
        )
        val plan = WorkoutPlan(title = "Push A", notes = null, exercises = exercises)
        val idMap = mapOf("Panca Piana" to "ex1", "Shoulder Press" to "ex2", "Tricep Dip" to "ex3")

        val entities = planToExerciseEntities(plan, "template-1", idMap)

        assertEquals(3, entities.size)
        assertEquals(0, entities[0].position)
        assertEquals(1, entities[1].position)
        assertEquals(2, entities[2].position)

        assertEquals("ex1", entities[0].exerciseId)
        assertEquals(4, entities[0].targetSets)
        assertEquals(8, entities[0].targetReps)
        assertEquals(90, entities[0].restSeconds)

        assertEquals("ex2", entities[1].exerciseId)
        assertEquals(3, entities[1].targetSets)
    }

    @Test
    fun `exercise entities correctly propagate optional rest seconds`() {
        val exercises = listOf(
            WorkoutPlanExercise("Squat", sets = 5, reps = 5, restSeconds = null, notes = null),
            WorkoutPlanExercise("Leg Press", sets = 3, reps = 12, restSeconds = 120, notes = "lento")
        )
        val plan = WorkoutPlan(title = "Legs", notes = null, exercises = exercises)
        val idMap = mapOf("Squat" to "ex-squat", "Leg Press" to "ex-legpress")

        val entities = planToExerciseEntities(plan, "tpl-legs", idMap)

        assertNull(entities[0].restSeconds)
        assertNotNull(entities[1].restSeconds)
        assertEquals(120, entities[1].restSeconds)
        assertEquals("lento", entities[1].notes)
    }

    @Test
    fun `empty plan produces no exercise entities`() {
        val plan = WorkoutPlan(title = "Vuoto", notes = null, exercises = emptyList())
        val entities = planToExerciseEntities(plan, "tpl-empty", emptyMap())
        assertEquals(0, entities.size)
    }
}
