package com.titanbiosync.data.local.gym.seed

import android.content.Context
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMediaEntity
import com.titanbiosync.data.local.entities.gym.ExerciseMuscleEntity
import com.titanbiosync.data.local.entities.gym.ExerciseVariantEntity
import com.titanbiosync.data.local.entities.gym.MuscleEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class GymSeedAssetLoader(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {
    fun loadFromAssets(basePath: String = "gym_seed"): GymSeedData {
        val muscles = decodeList("$basePath/muscles.json", ListSerializer(MuscleEntity.serializer()))
        val exercises = decodeList("$basePath/exercises.json", ListSerializer(ExerciseEntity.serializer()))
        val variants = decodeList("$basePath/exercise_variants.json", ListSerializer(ExerciseVariantEntity.serializer()))
        val links = decodeList("$basePath/exercise_muscles.json", ListSerializer(ExerciseMuscleEntity.serializer()))
        val media = decodeList("$basePath/exercise_media.json", ListSerializer(ExerciseMediaEntity.serializer()))

        return GymSeedData(
            muscles = muscles,
            exercises = exercises,
            variants = variants,
            exerciseMuscles = links,
            media = media
        )
    }

    private fun <T> decodeList(assetPath: String, serializer: KSerializer<List<T>>): List<T> {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return try {
            json.decodeFromString(serializer, text)
        } catch (t: Throwable) {
            throw IllegalStateException("Failed to decode seed asset: $assetPath", t)
        }
    }
}