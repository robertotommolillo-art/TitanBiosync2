package com.titanbiosync.gym.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.titanbiosync.data.local.AppDatabase
import com.titanbiosync.data.local.gym.seed.GymSeedAssetLoader
import com.titanbiosync.data.local.gym.seed.GymSeedConfig
import com.titanbiosync.data.local.gym.seed.GymSeedImporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GymSeedValidationTest {

    @Test
    fun seedAssets_areConsistent_andImportDoesNotThrow() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()

        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val seed = GymSeedAssetLoader(context)
                .loadFromAssets(GymSeedConfig.ASSET_BASE_PATH)

            GymSeedImporter(db).importIfNeeded(
                seedVersion = GymSeedConfig.SEED_VERSION,
                seed = seed,
                strict = true
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            fail("Seed validation/import failed: ${t.message}")
        } finally {
            db.close()
        }
    }
}