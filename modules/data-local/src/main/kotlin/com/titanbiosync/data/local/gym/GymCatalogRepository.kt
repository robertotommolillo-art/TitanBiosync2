package com.titanbiosync.data.local.gym

import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.MuscleDao
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymCatalogRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val muscleDao: MuscleDao
) {
    fun observeExercisesActive() = exerciseDao.observeAllActive()
    fun observeMuscles() = muscleDao.observeAll()

    /** Cancella tutti gli esercizi e li importa dal nuovo JSON (assets/gym_seed/exercises_it.json) */
    suspend fun replaceAllExercisesFromAssets(context: Context) {
        // 1. Cancella tutti gli esercizi
        exerciseDao.deleteAll()

        // 2. Carica il nuovo JSON
        val inputStream = context.assets.open("gym_seed/exercises_it.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<ExerciseEntity>>() {}.type
        val exercises: List<ExerciseEntity> = Gson().fromJson(json, type)

        // 3. Inserisci tutti
        exerciseDao.upsertAll(exercises)
    }
}