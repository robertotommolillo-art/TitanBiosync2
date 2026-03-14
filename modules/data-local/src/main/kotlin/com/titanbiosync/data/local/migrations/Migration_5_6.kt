package com.titanbiosync.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gym_workout_set_log (
                id TEXT NOT NULL PRIMARY KEY,
                sessionExerciseId TEXT NOT NULL,
                setIndex INTEGER NOT NULL,
                reps INTEGER,
                weightKg REAL,
                completed INTEGER NOT NULL,
                completedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_gym_workout_set_log_sessionExerciseId ON gym_workout_set_log(sessionExerciseId)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_gym_workout_set_log_sessionExerciseId_setIndex ON gym_workout_set_log(sessionExerciseId, setIndex)"
        )
    }
}