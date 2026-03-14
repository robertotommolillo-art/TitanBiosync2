package com.titanbiosync.gym.ui.picker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.databinding.ItemExercisePickerRowBinding

class ExercisePickerAdapter(
    private val onClick: (ExerciseEntity) -> Unit
) : ListAdapter<ExerciseEntity, ExercisePickerAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExercisePickerRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemExercisePickerRowBinding,
        private val onClick: (ExerciseEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExerciseEntity) {
            binding.title.text = item.nameIt
            binding.subtitle.text = item.category
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ExerciseEntity>() {
        override fun areItemsTheSame(oldItem: ExerciseEntity, newItem: ExerciseEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ExerciseEntity, newItem: ExerciseEntity): Boolean =
            oldItem == newItem
    }
}