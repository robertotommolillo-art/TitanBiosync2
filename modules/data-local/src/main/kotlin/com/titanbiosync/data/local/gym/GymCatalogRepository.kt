package com.titanbiosync.data.local.gym

import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.MuscleDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymCatalogRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val muscleDao: MuscleDao
) {
    fun observeExercisesActive() = exerciseDao.observeAllActive()
    fun observeMuscles() = muscleDao.observeAll()
}