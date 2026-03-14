package com.titanbiosync.gym.ui.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.data.local.dao.gym.GymFolderDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateDao
import com.titanbiosync.data.local.dao.gym.WorkoutTemplateExerciseDao
import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GymLibraryViewModel @Inject constructor(
    private val folderDao: GymFolderDao,
    private val templateDao: WorkoutTemplateDao,
    private val templateExerciseDao: WorkoutTemplateExerciseDao
) : ViewModel() {

    // null => root
    val selectedFolderId = MutableStateFlow<String?>(null)

    private val currentFolderName = MutableLiveData<String?>(null)

    val showUpButton: LiveData<Boolean> = currentFolderName.mapNotNullToBoolean { it != null }
    val title: LiveData<String> = currentFolderName.mapToString { it ?: "Libreria" }

    val folders = folderDao.observeAll().asLiveData()

    private val templatesSource: LiveData<List<WorkoutTemplateEntity>> = selectedFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) templateDao.observeWithoutFolder()
            else templateDao.observeByFolderId(folderId)
        }
        .asLiveData()

    private var templatesOverride: MutableList<WorkoutTemplateEntity>? = null

    private val templatesMediator = MediatorLiveData<List<WorkoutTemplateEntity>>().apply {
        addSource(templatesSource) { list ->
            if (templatesOverride == null) value = list
        }
    }

    val templates: LiveData<List<WorkoutTemplateEntity>> = templatesMediator

    // Lista unica per UI
    val libraryItems: LiveData<List<LibraryListItem>> = MediatorLiveData<List<LibraryListItem>>().apply {
        fun rebuild() {
            val inFolder = currentFolderName.value != null
            val result = mutableListOf<LibraryListItem>()

            if (inFolder) {
                result += LibraryListItem.Up
                result += templatesMediator.value.orEmpty().map { LibraryListItem.Template(it) }
            } else {
                result += folders.value.orEmpty().map { LibraryListItem.Folder(it) }
                result += templatesMediator.value.orEmpty().map { LibraryListItem.Template(it) } // senza cartella
            }

            value = result
        }

        addSource(folders) { rebuild() }
        addSource(templatesMediator) { rebuild() }
        addSource(currentFolderName) { rebuild() }
    }

    fun openFolder(folder: GymFolderEntity) {
        currentFolderName.value = folder.name
        selectedFolderId.value = folder.id
        templatesOverride = null
    }

    fun navigateUpToRoot() {
        currentFolderName.value = null
        selectedFolderId.value = null
        templatesOverride = null
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val nextIndex = (folderDao.getMaxSortIndex() ?: -1) + 1
            folderDao.upsert(
                GymFolderEntity(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    sortIndex = nextIndex
                )
            )
        }
    }

    fun createTemplate(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        val folderId = selectedFolderId.value
        viewModelScope.launch {
            val nextIndex = if (folderId == null) {
                (templateDao.getMaxSortIndexWithoutFolder() ?: -1) + 1
            } else {
                (templateDao.getMaxSortIndexForFolder(folderId) ?: -1) + 1
            }

            templateDao.upsert(
                WorkoutTemplateEntity(
                    id = UUID.randomUUID().toString(),
                    folderId = folderId,
                    name = trimmed,
                    sortIndex = nextIndex,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val current = folderDao.getByIdOnce(folderId) ?: return@launch
            folderDao.upsert(current.copy(name = trimmed))
            // se stai rinominando la cartella aperta, aggiorna anche il titolo
            if (selectedFolderId.value == folderId) {
                currentFolderName.value = trimmed
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            templateDao.clearFolder(folderId)
            folderDao.deleteById(folderId)
            if (selectedFolderId.value == folderId) {
                navigateUpToRoot()
            }
        }
    }

    fun renameTemplate(templateId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val current = templatesSource.value?.firstOrNull { it.id == templateId } ?: return@launch
            templateDao.upsert(
                current.copy(
                    name = trimmed,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun moveTemplate(templateId: String, newFolderId: String?) {
        viewModelScope.launch {
            val current = templatesSource.value?.firstOrNull { it.id == templateId } ?: return@launch
            templateDao.upsert(
                current.copy(
                    folderId = newFolderId,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTemplate(templateId: String) {
        val folderId = selectedFolderId.value
        viewModelScope.launch {
            templateExerciseDao.deleteAllForTemplate(templateId)
            templateDao.deleteById(templateId)
            compactTemplateSortIndexes(folderId)
        }
    }

    // --- DRAG & DROP ---
    private fun currentTemplatesList(): MutableList<WorkoutTemplateEntity> {
        val o = templatesOverride
        if (o != null) return o

        val base = templatesSource.value.orEmpty().toMutableList()
        templatesOverride = base
        templatesMediator.value = base
        return base
    }

    /**
     * fromAdapterPos / toAdapterPos sono posizioni nella lista *unica*.
     * Se siamo dentro cartella, pos 0 è "Up", quindi le schede partono da 1.
     */
    fun onTemplatesReordered(fromAdapterPos: Int, toAdapterPos: Int): List<LibraryListItem> {
        val inFolder = currentFolderName.value != null
        val templateOffset = if (inFolder) 1 else folders.value.orEmpty().size

        val from = fromAdapterPos - templateOffset
        val to = toAdapterPos - templateOffset

        val list = currentTemplatesList()
        if (from !in list.indices || to !in list.indices) return libraryItems.value.orEmpty()

        val item = list.removeAt(from)
        list.add(to, item)

        templatesMediator.value = list
        // libraryItems si ricostruisce da MediatorLiveData, ma per immediatezza la ritorniamo
        return libraryItems.value.orEmpty()
    }

    fun commitTemplateOrder() {
        val list = templatesOverride ?: return
        viewModelScope.launch {
            list.forEachIndexed { index, item ->
                if (item.sortIndex != index) {
                    templateDao.setSortIndex(item.id, index)
                }
            }
            templatesOverride = null
        }
    }

    private suspend fun compactTemplateSortIndexes(folderId: String?) {
        val templates = if (folderId == null) {
            templateDao.getWithoutFolderOnce()
        } else {
            templateDao.getByFolderIdOnce(folderId)
        }

        templates.forEachIndexed { index, t ->
            if (t.sortIndex != index) {
                templateDao.setSortIndex(t.id, index)
            }
        }
    }
}

// ---- piccoli extension per evitare boilerplate ----
private fun <T> LiveData<T>.mapNotNullToBoolean(mapper: (T?) -> Boolean): LiveData<Boolean> {
    val out = MediatorLiveData<Boolean>()
    out.addSource(this) { out.value = mapper(it) }
    return out
}

private fun <T> LiveData<T>.mapToString(mapper: (T?) -> String): LiveData<String> {
    val out = MediatorLiveData<String>()
    out.addSource(this) { out.value = mapper(it) }
    return out
}