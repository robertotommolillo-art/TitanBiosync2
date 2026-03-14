package com.titanbiosync.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gym_workout_session (
                id TEXT NOT NULL PRIMARY KEY,
                templateId TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                endedAt INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gym_workout_session_exercise (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                position INTEGER NOT NULL,
                nameItSnapshot TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_gym_workout_session_exercise_sessionId ON gym_workout_session_exercise(sessionId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_gym_workout_session_exercise_exerciseId ON gym_workout_session_exercise(exerciseId)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_gym_workout_session_exercise_sessionId_position ON gym_workout_session_exercise(sessionId, position)"
        )
    }
}