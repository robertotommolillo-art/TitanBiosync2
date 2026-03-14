package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "gym_exercise_media",
    indices = [
        Index(value = ["exerciseId"]),
        Index(value = ["variantId"]),
        Index(value = ["type"]),
        Index(value = ["source"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseMediaEntity(
    @PrimaryKey val id: String,

    val exerciseId: String,        // <-- changed
    val variantId: String? = null,

    /**
     * Esempi: "video" | "image" | "gif" | "link"
     */
    val type: String,

    /**
     * "remote" oppure "asset"
     */
    val source: String = "remote",

    /**
     * Se source == "remote": URL completa (https://...)
     * Se source == "asset": path relativo ad android asset, es "gym_media/bench_press.mp4"
     */
    val url: String,

    val thumbnailUrl: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)