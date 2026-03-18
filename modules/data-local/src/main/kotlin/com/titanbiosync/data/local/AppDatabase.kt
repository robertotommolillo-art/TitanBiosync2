package com.titanbiosync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.titanbiosync.data.local.dao.AppConfigDao
import com.titanbiosync.data.local.dao.CoachPromptDao
import com.titanbiosync.data.local.dao.ConsentDao
import com.titanbiosync.data.local.dao.DeviceDao
import com.titanbiosync.data.local.dao.HealthMetricsDao
import com.titanbiosync.data.local.dao.MapLocationDao
import com.titanbiosync.data.local.dao.RecommendationDao
import com.titanbiosync.data.local.dao.SensorReadingDao
import com.titanbiosync.data.local.dao.SessionDao
import com.titanbiosync.data.local.dao.UserDao
import com.titanbiosync.data.local.dao.gym.ExerciseDao
import com.titanbiosync.data.local.dao.gym.ExerciseFilterOptionsDao
import com.titanbiosync.data.local.dao.gym.ExerciseMediaDao
import com.titanbiosync.data.local.dao.gym.ExerciseMuscleDao
import com.titanbiosync.data.local.dao.gym.ExerciseVariantDao
import com.titanbiosync.data.local.dao.gym.GymFolderDao
import com.titanbiosync.data.local.dao.gym.GymSeedMetaDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSessionExerciseDao
import com.titanbiosync.data.local.dao.gym.GymWorkoutSetLogDao
import com.titanbiosync.data.local.dao.gym.MuscleDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseSetDao
import com.titanbiosync.data.local.entities.AppConfigEntity
import com.titanbiosync.data.local.entities.CoachPromptEntity
import com.titanbiosync.data.local.entities.ConsentRecordEntity
import com.titanbiosync.data.local.entities.DeviceEntity
import com.titanbiosync.data.local.entities.HealthMetricsEntity
import com.titanbiosync.data.local.entities.MapLocationEntity
import com.titanbiosync.data.local.entities.RecommendationEntity
import com.titanbiosync.data.local.entities.SensorReadingEntity
import com.titanbiosync.data.local.entities.SessionEntity
import com.titanbiosync.data.local.entities.UserEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMediaEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMuscleEntity
import com.titanbiosync.data.local.entities.gym.ExerciseVariantEntity
import com.titanbiosync.data.local.entities.gym.GymSeedMetaEntity
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionEntity
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionExerciseEntity
import com.titanbiosync.data.local.entities.gym.GymWorkoutSetLogEntity
import com.titanbiosync.data.local.entities.gym.MuscleEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateExerciseSetEntity

@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        SensorReadingEntity::class,
        SessionEntity::class,
        HealthMetricsEntity::class,
        RecommendationEntity::class,
        CoachPromptEntity::class,
        MapLocationEntity::class,
        ConsentRecordEntity::class,
        AppConfigEntity::class,

        // --- Workout session ---
        GymWorkoutSessionEntity::class,
        GymWorkoutSessionExerciseEntity::class,
        GymWorkoutSetLogEntity::class,

        // --- GYM catalog ---
        com.titanbiosync.data.local.entities.gym.ExerciseEntity::class,
        ExerciseVariantEntity::class,
        ExerciseMediaEntity::class,
        MuscleEntity::class,
        ExerciseMuscleEntity::class,
        GymSeedMetaEntity::class,

        // --- GYM workout templates ---
        com.titanbiosync.data.local.entities.gym.GymFolderEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        WorkoutTemplateExerciseSetEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun sessionDao(): SessionDao
    abstract fun healthMetricsDao(): HealthMetricsDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun coachPromptDao(): CoachPromptDao
    abstract fun mapLocationDao(): MapLocationDao
    abstract fun consentDao(): ConsentDao
    abstract fun appConfigDao(): AppConfigDao

    abstract fun exerciseFilterOptionsDao(): ExerciseFilterOptionsDao

    // --- GYM catalog DAOs ---
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseVariantDao(): ExerciseVariantDao
    abstract fun exerciseMediaDao(): ExerciseMediaDao
    abstract fun muscleDao(): MuscleDao
    abstract fun exerciseMuscleDao(): ExerciseMuscleDao
    abstract fun gymSeedMetaDao(): GymSeedMetaDao

    // --- Workout templates DAOs ---
    abstract fun gymFolderDao(): GymFolderDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutTemplateExerciseDao(): WorkoutTemplateExerciseDao
    abstract fun workoutTemplateExerciseSetDao(): WorkoutTemplateExerciseSetDao

    // --- Workout session DAOs ---
    abstract fun gymWorkoutSessionDao(): GymWorkoutSessionDao
    abstract fun gymWorkoutSessionExerciseDao(): GymWorkoutSessionExerciseDao
    abstract fun gymWorkoutSetLogDao(): GymWorkoutSetLogDao
}