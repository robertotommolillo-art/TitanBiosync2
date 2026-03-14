package com.titanbiosync.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sessions ADD COLUMN aggregated_metrics_json TEXT")
        Log.i("MIGRATION", "Applied MIGRATION_1_2: added aggregated_metrics_json")
    }
}