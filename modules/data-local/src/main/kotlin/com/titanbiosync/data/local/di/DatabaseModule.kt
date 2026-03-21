package com.titanbiosync.data.local.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.titanbiosync.data.local.AppDatabase
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
import com.titanbiosync.data.local.dao.gym.ExercisePrDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseSetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "titanbiosync.db"

    /**
     * Migration 6 -> 7
     * - Adds archivedAt column to gym_exercises
     * - Adds FK constraints to gym_exercise_muscles (requires table rebuild)
     * - Creates gym_seed_meta
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1) Soft delete column on exercises
            db.execSQL("ALTER TABLE gym_exercises ADD COLUMN archivedAt INTEGER")

            // 2) Create gym_seed_meta
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS gym_seed_meta (
                    `key` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`key`)
                )
                """.trimIndent()
            )

            // 3) Rebuild gym_exercise_muscles to add FK constraints
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS gym_exercise_muscles_new (
                    exerciseId TEXT NOT NULL,
                    muscleId TEXT NOT NULL,
                    role TEXT NOT NULL,
                    weight REAL NOT NULL,
                    PRIMARY KEY(exerciseId, muscleId),
                    FOREIGN KEY(exerciseId) REFERENCES gym_exercises(id) ON DELETE CASCADE,
                    FOREIGN KEY(muscleId) REFERENCES gym_muscles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // Copy data
            db.execSQL(
                """
                INSERT INTO gym_exercise_muscles_new (exerciseId, muscleId, role, weight)
                SELECT exerciseId, muscleId, role, weight
                FROM gym_exercise_muscles
                """.trimIndent()
            )

            // Drop old + rename new
            db.execSQL("DROP TABLE gym_exercise_muscles")
            db.execSQL("ALTER TABLE gym_exercise_muscles_new RENAME TO gym_exercise_muscles")

            // Recreate indices (Room will expect them)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_muscles_muscleId ON gym_exercise_muscles(muscleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_muscles_exerciseId ON gym_exercise_muscles(exerciseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_muscles_role ON gym_exercise_muscles(role)")
        }
    }

    /**
     * Migration 7 -> 8
     * - Rebuilds gym_exercise_media to make exerciseId NOT NULL
     * - Adds missing indices expected by Room for gym_exercises (archivedAt, isCustom)
     */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // --- Rebuild gym_exercise_media (exerciseId NOT NULL) ---
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS gym_exercise_media_new (
                    id TEXT NOT NULL,
                    exerciseId TEXT NOT NULL,
                    variantId TEXT,
                    type TEXT NOT NULL,
                    source TEXT NOT NULL,
                    url TEXT NOT NULL,
                    thumbnailUrl TEXT,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(exerciseId) REFERENCES gym_exercises(id) ON DELETE CASCADE,
                    FOREIGN KEY(variantId) REFERENCES gym_exercise_variants(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO gym_exercise_media_new (
                    id, exerciseId, variantId, type, source, url, thumbnailUrl, createdAt
                )
                SELECT
                    id, exerciseId, variantId, type, source, url, thumbnailUrl, createdAt
                FROM gym_exercise_media
                WHERE exerciseId IS NOT NULL
                """.trimIndent()
            )

            db.execSQL("DROP TABLE gym_exercise_media")
            db.execSQL("ALTER TABLE gym_exercise_media_new RENAME TO gym_exercise_media")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_exerciseId ON gym_exercise_media(exerciseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_variantId ON gym_exercise_media(variantId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_type ON gym_exercise_media(type)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_source ON gym_exercise_media(source)")

            // --- Add missing indices expected by Room for gym_exercises ---
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_archivedAt ON gym_exercises(archivedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_isCustom ON gym_exercises(isCustom)")
        }
    }

    /**
     * Migration 8 -> 9
     * - Rebuilds gym_exercises to match current entity schema and ensures category is NOT NULL.
     *
     * NOTE:
     * This migration assumes gym_exercises has the columns:
     * id, nameIt, nameEn, descriptionIt, descriptionEn, category, equipment, mechanics, level,
     * isCustom, createdAt, archivedAt
     *
     * If your ExerciseEntity differs, adjust the column list accordingly.
     */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS gym_exercises_new (
                    id TEXT NOT NULL,
                    nameIt TEXT NOT NULL,
                    nameEn TEXT NOT NULL,
                    descriptionIt TEXT,
                    descriptionEn TEXT,
                    category TEXT NOT NULL,
                    equipment TEXT,
                    mechanics TEXT,
                    level TEXT,
                    isCustom INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    archivedAt INTEGER,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO gym_exercises_new (
                    id, nameIt, nameEn, descriptionIt, descriptionEn,
                    category, equipment, mechanics, level,
                    isCustom, createdAt, archivedAt
                )
                SELECT
                    id, nameIt, nameEn, descriptionIt, descriptionEn,
                    COALESCE(category, 'bodybuilding') AS category,
                    equipment, mechanics, level,
                    isCustom, createdAt, archivedAt
                FROM gym_exercises
                """.trimIndent()
            )

            db.execSQL("DROP TABLE gym_exercises")
            db.execSQL("ALTER TABLE gym_exercises_new RENAME TO gym_exercises")

            // Recreate indexes expected by Room
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_nameIt ON gym_exercises(nameIt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_nameEn ON gym_exercises(nameEn)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_category ON gym_exercises(category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_equipment ON gym_exercises(equipment)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_mechanics ON gym_exercises(mechanics)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_level ON gym_exercises(level)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_archivedAt ON gym_exercises(archivedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercises_isCustom ON gym_exercises(isCustom)")
        }
    }
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {

            // ---------------------------------------------------------------------
            // 1) gym_folders (NEW in v10) - force correct schema
            // ---------------------------------------------------------------------
            db.execSQL("DROP TABLE IF EXISTS gym_folders")
            db.execSQL("DROP INDEX IF EXISTS index_gym_folders_sortIndex")
            db.execSQL("DROP INDEX IF EXISTS index_gym_folders_createdAt")

            db.execSQL(
                """
            CREATE TABLE gym_folders (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                sortIndex INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
            )

            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_folders_sortIndex ON gym_folders(sortIndex)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_folders_createdAt ON gym_folders(createdAt)")

            // ---------------------------------------------------------------------
            // 2) gym_workout_template_exercises - add superset columns (v10+)
            // ---------------------------------------------------------------------
            // Add columns only if they don't exist (SQLite doesn't support IF NOT EXISTS on ADD COLUMN,
            // so this will fail if the column already exists; in typical 9->10 flow it doesn't exist yet).
            db.execSQL("ALTER TABLE gym_workout_template_exercises ADD COLUMN supersetGroupId TEXT")
            db.execSQL("ALTER TABLE gym_workout_template_exercises ADD COLUMN supersetOrder INTEGER")

            // Recreate indices expected by Room (safe with IF NOT EXISTS)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_workout_template_exercises_templateId ON gym_workout_template_exercises(templateId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_workout_template_exercises_exerciseId ON gym_workout_template_exercises(exerciseId)")

            // ---------------------------------------------------------------------
            // 3) gym_workout_template_exercise_sets (NEW in v10)
            // ---------------------------------------------------------------------
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS gym_workout_template_exercise_sets (
                templateId TEXT NOT NULL,
                position INTEGER NOT NULL,
                setIndex INTEGER NOT NULL,
                reps INTEGER,
                PRIMARY KEY(templateId, position, setIndex)
            )
            """.trimIndent()
            )

            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_workout_template_exercise_sets_templateId ON gym_workout_template_exercise_sets(templateId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_workout_template_exercise_sets_templateId_position ON gym_workout_template_exercise_sets(templateId, position)")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN first_name TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN last_name TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN age INTEGER")
            db.execSQL("ALTER TABLE users ADD COLUMN height REAL")
            db.execSQL("ALTER TABLE users ADD COLUMN sex TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN avatar_uri TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN updated_at INTEGER")
        }
    }

    /**
     * Migration 11 -> 12
     * - Adds gym_exercise_pr table for per-exercise strength PR tracking.
     */
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS gym_exercise_pr (
                    exerciseId TEXT NOT NULL,
                    maxWeightKg REAL NOT NULL,
                    maxE1rm REAL NOT NULL,
                    maxReps INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(exerciseId),
                    FOREIGN KEY(exerciseId) REFERENCES gym_exercises(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_gym_exercise_pr_exerciseId ON gym_exercise_pr(exerciseId)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val builder = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DB_NAME
        )
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)

        // fallback distruttivo SOLO in debug
        return if (isDebugBuild(context)) {
            builder.fallbackToDestructiveMigration().build()
        } else {
            builder.build()
        }
    }

    private fun isDebugBuild(context: Context): Boolean {
        return try {
            val clazz = Class.forName("${context.packageName}.BuildConfig")
            clazz.getField("DEBUG").getBoolean(null)
        } catch (_: Exception) {
            false
        }
    }

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideDeviceDao(db: AppDatabase): DeviceDao = db.deviceDao()
    @Provides fun provideSensorReadingDao(db: AppDatabase): SensorReadingDao = db.sensorReadingDao()
    @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun provideHealthMetricsDao(db: AppDatabase): HealthMetricsDao = db.healthMetricsDao()
    @Provides fun provideRecommendationDao(db: AppDatabase): RecommendationDao = db.recommendationDao()
    @Provides fun provideCoachPromptDao(db: AppDatabase): CoachPromptDao = db.coachPromptDao()
    @Provides fun provideMapLocationDao(db: AppDatabase): MapLocationDao = db.mapLocationDao()
    @Provides fun provideConsentDao(db: AppDatabase): ConsentDao = db.consentDao()
    @Provides fun provideAppConfigDao(db: AppDatabase): AppConfigDao = db.appConfigDao()

    // --- GYM catalog ---
    @Provides fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideExerciseVariantDao(db: AppDatabase): ExerciseVariantDao = db.exerciseVariantDao()
    @Provides fun provideExerciseMediaDao(db: AppDatabase): ExerciseMediaDao = db.exerciseMediaDao()
    @Provides fun provideMuscleDao(db: AppDatabase): MuscleDao = db.muscleDao()
    @Provides fun provideExerciseMuscleDao(db: AppDatabase): ExerciseMuscleDao = db.exerciseMuscleDao()
    @Provides fun provideGymSeedMetaDao(db: AppDatabase): GymSeedMetaDao = db.gymSeedMetaDao()
    @Provides fun provideExerciseFilterOptionsDao(db: AppDatabase): ExerciseFilterOptionsDao =
        db.exerciseFilterOptionsDao()

    // --- Templates ---
    @Provides fun provideGymFolderDao(db: AppDatabase): GymFolderDao = db.gymFolderDao()
    @Provides fun provideWorkoutTemplateDao(db: AppDatabase): WorkoutTemplateDao = db.workoutTemplateDao()
    @Provides fun provideWorkoutTemplateExerciseDao(db: AppDatabase): WorkoutTemplateExerciseDao =
        db.workoutTemplateExerciseDao()

    // --- Session ---
    @Provides fun provideGymWorkoutSessionDao(db: AppDatabase): GymWorkoutSessionDao = db.gymWorkoutSessionDao()
    @Provides fun provideGymWorkoutSessionExerciseDao(db: AppDatabase): GymWorkoutSessionExerciseDao =
        db.gymWorkoutSessionExerciseDao()
    @Provides fun provideGymWorkoutSetLogDao(db: AppDatabase): GymWorkoutSetLogDao = db.gymWorkoutSetLogDao()
    @Provides
    fun provideWorkoutTemplateExerciseSetDao(db: AppDatabase): WorkoutTemplateExerciseSetDao =
        db.workoutTemplateExerciseSetDao()

    // --- Strength PR tracking ---
    @Provides fun provideExercisePrDao(db: AppDatabase): ExercisePrDao = db.exercisePrDao()
}