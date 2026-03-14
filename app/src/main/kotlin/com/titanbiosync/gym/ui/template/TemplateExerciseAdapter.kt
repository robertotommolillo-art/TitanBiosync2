package com.titanbiosync.gym.ui.template

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.model.gym.TemplateExerciseRow
import com.titanbiosync.databinding.ItemTemplateExerciseBinding

class TemplateExerciseAdapter(
    private val onLongClick: (TemplateExerciseRow) -> Unit
) : ListAdapter<TemplateExerciseRow, TemplateExerciseAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTemplateExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding, onLongClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemTemplateExerciseBinding,
        private val onLongClick: (TemplateExerciseRow) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: TemplateExerciseRow) {
            val suffix = if (row.supersetGroupId != null) {
                when (row.supersetOrder) {
                    0 -> " (Superserie A)"
                    1 -> " (Superserie B)"
                    else -> " (Superserie)"
                }
            } else ""

            binding.title.text = row.nameIt + suffix
            binding.subtitle.text = row.nameEn

            binding.root.setOnLongClickListener {
                onLongClick(row)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TemplateExerciseRow>() {
            override fun areItemsTheSame(oldItem: TemplateExerciseRow, newItem: TemplateExerciseRow) =
                oldItem.position == newItem.position && oldItem.exerciseId == newItem.exerciseId

            override fun areContentsTheSame(oldItem: TemplateExerciseRow, newItem: TemplateExerciseRow) =
                oldItem == newItem
        }
    }
}