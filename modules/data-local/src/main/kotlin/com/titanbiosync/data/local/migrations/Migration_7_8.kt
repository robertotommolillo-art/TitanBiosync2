package com.titanbiosync.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) Create new table with NOT NULL exerciseId
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

        // 2) Copy (skip invalid rows where exerciseId is null)
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

        // 3) Replace table
        db.execSQL("DROP TABLE gym_exercise_media")
        db.execSQL("ALTER TABLE gym_exercise_media_new RENAME TO gym_exercise_media")

        // 4) Recreate indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_exerciseId ON gym_exercise_media(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_variantId ON gym_exercise_media(variantId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_type ON gym_exercise_media(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_exercise_media_source ON gym_exercise_media(source)")
    }
}