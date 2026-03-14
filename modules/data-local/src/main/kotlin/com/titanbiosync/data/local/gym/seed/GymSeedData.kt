package com.titanbiosync.data.local.gym.seed

import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMediaEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMuscleEntity
import com.titanbiosync.data.local.entities.gym.ExerciseVariantEntity
import com.titanbiosync.data.local.entities.gym.MuscleEntity

data class GymSeedData(
    val muscles: List<MuscleEntity>,
    val exercises: List<ExerciseEntity>,
    val variants: List<ExerciseVariantEntity>,
    val exerciseMuscles: List<ExerciseMuscleEntity>,
    val media: List<ExerciseMediaEntity>
)