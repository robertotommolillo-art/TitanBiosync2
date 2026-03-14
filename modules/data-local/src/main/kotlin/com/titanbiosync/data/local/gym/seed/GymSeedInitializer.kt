package com.titanbiosync.data.local.gym.seed

import android.content.Context
import com.titanbiosync.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymSeedInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importer: GymSeedImporter
) {
    suspend fun ensureSeeded(strict: Boolean) {
        val seed = GymSeedAssetLoader(context).loadFromAssets(GymSeedConfig.ASSET_BASE_PATH)
        importer.importIfNeeded(
            seedVersion = GymSeedConfig.SEED_VERSION,
            seed = seed,
            strict = strict
        )
    }
}