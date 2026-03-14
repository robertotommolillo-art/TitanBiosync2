package com.titanbiosync.data.local.gym.seed

import android.util.Log
import androidx.room.withTransaction
import com.titanbiosync.data.local.AppDatabase
import com.titanbiosync.data.local.entities.gym.GymSeedMetaEntity

class GymSeedImporter(
    private val db: AppDatabase
) {
    companion object {
        private const val META_KEY_VERSION = "gym_seed_version"
        private const val TAG = "GymSeedImporter"
    }

    /**
     * @param strict If true, throws on seed validation/insert issues (useful for debug/dev).
     *               If false, logs the error and skips the import (safer for release).
     */
    suspend fun importIfNeeded(
        seedVersion: Int,
        seed: GymSeedData,
        strict: Boolean = false
    ) {
        val current = db.gymSeedMetaDao().getValue(META_KEY_VERSION)?.toIntOrNull()
        if (current == seedVersion) return

        try {
            validateSeed(seed)

            db.withTransaction {
                db.muscleDao().upsertAll(seed.muscles)
                db.exerciseDao().upsertAll(seed.exercises)
                db.exerciseVariantDao().upsertAll(seed.variants)

                db.exerciseMuscleDao().upsertAll(seed.exerciseMuscles)
                db.exerciseMediaDao().upsertAll(seed.media)

                db.gymSeedMetaDao().upsert(
                    GymSeedMetaEntity(
                        key = META_KEY_VERSION,
                        value = seedVersion.toString()
                    )
                )
            }
        } catch (t: Throwable) {
            if (strict) throw t
            Log.e(TAG, "Gym seed import skipped (non-strict mode). seedVersion=$seedVersion", t)
        }
    }

    private fun validateSeed(seed: GymSeedData) {
        val exerciseIds = seed.exercises.map { it.id }.toHashSet()
        val muscleIds = seed.muscles.map { it.id }.toHashSet()
        val variantIds = seed.variants.map { it.id }.toHashSet()

        val missingExerciseIdsInLinks = seed.exerciseMuscles
            .map { it.exerciseId }
            .filter { it !in exerciseIds }
            .distinct()
            .sorted()

        val missingMuscleIdsInLinks = seed.exerciseMuscles
            .map { it.muscleId }
            .filter { it !in muscleIds }
            .distinct()
            .sorted()

        val missingExerciseIdsInMedia = seed.media
            .map { it.exerciseId }
            .filter { it !in exerciseIds }
            .distinct()
            .sorted()

        val missingVariantIdsInMedia = seed.media
            .mapNotNull { it.variantId }
            .filter { it !in variantIds }
            .distinct()
            .sorted()

        if (
            missingExerciseIdsInLinks.isNotEmpty() ||
            missingMuscleIdsInLinks.isNotEmpty() ||
            missingExerciseIdsInMedia.isNotEmpty() ||
            missingVariantIdsInMedia.isNotEmpty()
        ) {
            throw IllegalStateException(
                buildString {
                    appendLine("Seed validation failed:")
                    if (missingExerciseIdsInLinks.isNotEmpty()) {
                        appendLine(" - exercise_muscles.json missing exerciseId(s): ${missingExerciseIdsInLinks.joinToString()}")
                    }
                    if (missingMuscleIdsInLinks.isNotEmpty()) {
                        appendLine(" - exercise_muscles.json missing muscleId(s): ${missingMuscleIdsInLinks.joinToString()}")
                    }
                    if (missingExerciseIdsInMedia.isNotEmpty()) {
                        appendLine(" - exercise_media.json missing exerciseId(s): ${missingExerciseIdsInMedia.joinToString()}")
                    }
                    if (missingVariantIdsInMedia.isNotEmpty()) {
                        appendLine(" - exercise_media.json missing variantId(s): ${missingVariantIdsInMedia.joinToString()}")
                    }
                }
            )
        }
    }
}