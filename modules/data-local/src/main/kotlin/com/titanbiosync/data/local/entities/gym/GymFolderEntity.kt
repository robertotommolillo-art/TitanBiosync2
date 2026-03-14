package com.titanbiosync.data.local.entities.gym

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_folders",
    indices = [
        Index(value = ["sortIndex"]),
        Index(value = ["createdAt"])
    ]
)
data class GymFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)