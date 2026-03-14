package com.titanbiosync.gym.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import com.titanbiosync.databinding.ItemGymFolderBinding

class GymFolderAdapter(
    private val onSelected: (String?) -> Unit,
    private val onEdit: (GymFolderEntity) -> Unit,
    private val onDelete: (GymFolderEntity) -> Unit
) : RecyclerView.Adapter<GymFolderAdapter.VH>() {

    private var folders: List<GymFolderEntity> = emptyList()
    private var selectedId: String? = null

    fun submit(items: List<GymFolderEntity>, selectedFolderId: String?) {
        folders = items
        selectedId = selectedFolderId
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = folders.size + 1 // +1 = "Senza cartella"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGymFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onSelected, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (position == 0) {
            holder.bindWithoutFolder(selected = selectedId == null)
        } else {
            val f = folders[position - 1]
            holder.bindFolder(folder = f, selected = selectedId == f.id)
        }
    }

    class VH(
        private val binding: ItemGymFolderBinding,
        private val onSelected: (String?) -> Unit,
        private val onEdit: (GymFolderEntity) -> Unit,
        private val onDelete: (GymFolderEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindWithoutFolder(selected: Boolean) {
            val label = "Senza cartella"
            binding.text.text = if (selected) "✓ $label" else label
            binding.root.setOnClickListener { onSelected(null) }

            binding.editButton.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
        }

        fun bindFolder(folder: GymFolderEntity, selected: Boolean) {
            binding.text.text = if (selected) "✓ ${folder.name}" else folder.name
            binding.root.setOnClickListener { onSelected(folder.id) }

            binding.editButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.VISIBLE

            binding.editButton.setOnClickListener { onEdit(folder) }
            binding.deleteButton.setOnClickListener { onDelete(folder) }
        }
    }
}