package com.titanbiosync.gym.ui.library

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import com.titanbiosync.databinding.ItemLibraryFolderBinding
import com.titanbiosync.databinding.ItemLibraryUpBinding
import com.titanbiosync.databinding.ItemWorkoutTemplateBinding

class LibraryAdapter(
    private val onUp: () -> Unit,
    private val onFolderClick: (GymFolderEntity) -> Unit,
    private val onFolderEdit: (GymFolderEntity) -> Unit,
    private val onFolderDelete: (GymFolderEntity) -> Unit,
    private val onTemplateClick: (WorkoutTemplateEntity) -> Unit,
    private val onTemplateLongClick: (WorkoutTemplateEntity) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<LibraryListItem, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is LibraryListItem.Up -> 0
        is LibraryListItem.Folder -> 1
        is LibraryListItem.Template -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> UpVH(ItemLibraryUpBinding.inflate(inflater, parent, false), onUp)
            1 -> FolderVH(
                ItemLibraryFolderBinding.inflate(inflater, parent, false),
                onFolderClick,
                onFolderEdit,
                onFolderDelete
            )
            else -> TemplateVH(
                ItemWorkoutTemplateBinding.inflate(inflater, parent, false),
                onTemplateClick,
                onTemplateLongClick,
                onStartDrag
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is LibraryListItem.Up -> (holder as UpVH).bind()
            is LibraryListItem.Folder -> (holder as FolderVH).bind(item.folder)
            is LibraryListItem.Template -> (holder as TemplateVH).bind(item.template)
        }
    }

    class UpVH(
        private val binding: ItemLibraryUpBinding,
        private val onUp: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.root.setOnClickListener { onUp() }
        }
    }

    class FolderVH(
        private val binding: ItemLibraryFolderBinding,
        private val onClick: (GymFolderEntity) -> Unit,
        private val onEdit: (GymFolderEntity) -> Unit,
        private val onDelete: (GymFolderEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: GymFolderEntity) {
            binding.name.text = folder.name
            binding.root.setOnClickListener { onClick(folder) }
            binding.editButton.setOnClickListener { onEdit(folder) }
            binding.deleteButton.setOnClickListener { onDelete(folder) }
        }
    }

    class TemplateVH(
        private val binding: ItemWorkoutTemplateBinding,
        private val onClick: (WorkoutTemplateEntity) -> Unit,
        private val onLongClick: (WorkoutTemplateEntity) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(template: WorkoutTemplateEntity) {
            binding.title.text = template.name
            binding.subtitle.text = template.notes ?: ""

            binding.root.setOnClickListener { onClick(template) }
            binding.root.setOnLongClickListener {
                onLongClick(template)
                true
            }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                    true
                } else false
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LibraryListItem>() {
            override fun areItemsTheSame(oldItem: LibraryListItem, newItem: LibraryListItem): Boolean =
                when {
                    oldItem is LibraryListItem.Up && newItem is LibraryListItem.Up -> true
                    oldItem is LibraryListItem.Folder && newItem is LibraryListItem.Folder ->
                        oldItem.folder.id == newItem.folder.id
                    oldItem is LibraryListItem.Template && newItem is LibraryListItem.Template ->
                        oldItem.template.id == newItem.template.id
                    else -> false
                }

            override fun areContentsTheSame(oldItem: LibraryListItem, newItem: LibraryListItem): Boolean =
                oldItem == newItem
        }
    }
}