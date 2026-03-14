package com.titanbiosync.gym.ui.library

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import com.titanbiosync.databinding.ItemWorkoutTemplateBinding

class WorkoutTemplateAdapter(
    private val onClick: (String) -> Unit,
    private val onLongClick: (WorkoutTemplateEntity) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<WorkoutTemplateEntity, WorkoutTemplateAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWorkoutTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onClick, onLongClick, onStartDrag)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemWorkoutTemplateBinding,
        private val onClick: (String) -> Unit,
        private val onLongClick: (WorkoutTemplateEntity) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WorkoutTemplateEntity) {
            binding.title.text = item.name
            binding.subtitle.text = item.notes ?: ""

            binding.root.setOnClickListener { onClick(item.id) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                    true
                } else {
                    false
                }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkoutTemplateEntity>() {
            override fun areItemsTheSame(oldItem: WorkoutTemplateEntity, newItem: WorkoutTemplateEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WorkoutTemplateEntity, newItem: WorkoutTemplateEntity) =
                oldItem == newItem
        }
    }
}