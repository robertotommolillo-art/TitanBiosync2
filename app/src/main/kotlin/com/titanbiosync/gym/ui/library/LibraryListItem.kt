package com.titanbiosync.gym.ui.library

import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity

sealed class LibraryListItem {
    data object Up : LibraryListItem()
    data class Folder(val folder: GymFolderEntity) : LibraryListItem()
    data class Template(val template: WorkoutTemplateEntity) : LibraryListItem()
}