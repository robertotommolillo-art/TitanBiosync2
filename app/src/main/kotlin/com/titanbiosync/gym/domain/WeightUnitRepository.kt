package com.titanbiosync.gym.domain

import com.titanbiosync.data.local.AppConfigKeys
import com.titanbiosync.data.local.dao.AppConfigDao
import com.titanbiosync.data.local.entities.AppConfigEntity
import com.titanbiosync.data.local.json.SimpleJsonString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WeightUnitRepository @Inject constructor(
    private val appConfigDao: AppConfigDao
) {
    fun observe(): Flow<WeightUnit> {
        return appConfigDao.observeAll()
            .map { all ->
                val entity = all.firstOrNull { it.key == AppConfigKeys.WEIGHT_UNIT }
                val raw = SimpleJsonString.decode(entity?.valueJson)
                WeightUnit.fromKey(raw ?: WeightUnit.KG.key)
            }
    }

    suspend fun set(unit: WeightUnit) {
        appConfigDao.upsert(
            AppConfigEntity(
                key = AppConfigKeys.WEIGHT_UNIT,
                valueJson = SimpleJsonString.encode(unit.key)
            )
        )
    }
}