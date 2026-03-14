package com.titanbiosync.gym.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.ExerciseEntity
import com.titanbiosync.databinding.ItemGymExerciseBinding

class GymExerciseAdapter(
    private val onItemClick: (ExerciseEntity) -> Unit
) : ListAdapter<ExerciseEntity, GymExerciseAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGymExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemGymExerciseBinding,
        private val onItemClick: (ExerciseEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var current: ExerciseEntity? = null

        init {
            binding.root.setOnClickListener {
                current?.let(onItemClick)
            }
        }

        fun bind(item: ExerciseEntity) {
            current = item
            binding.title.text = "${item.nameIt} / ${item.nameEn}"
            val equip = item.equipment ?: "-"
            binding.subtitle.text = "${item.category} • $equip"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ExerciseEntity>() {
            override fun areItemsTheSame(oldItem: ExerciseEntity, newItem: ExerciseEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ExerciseEntity, newItem: ExerciseEntity) =
                oldItem == newItem
        }
    }
}