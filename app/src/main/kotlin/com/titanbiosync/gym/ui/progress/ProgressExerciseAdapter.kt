package com.titanbiosync.gym.ui.progress

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.R
import com.titanbiosync.databinding.ItemProgressExerciseBinding

class ProgressExerciseAdapter(
    private val onItemClick: (ProgressExerciseUi) -> Unit
) : ListAdapter<ProgressExerciseUi, ProgressExerciseAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProgressExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemProgressExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProgressExerciseUi) {
            binding.exerciseName.text = item.exerciseName
            binding.sessionCount.text = if (item.sessionCount > 0) {
                binding.root.context.getString(R.string.progress_sessions_count, item.sessionCount)
            } else {
                binding.root.context.getString(R.string.progress_no_sessions)
            }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProgressExerciseUi>() {
            override fun areItemsTheSame(old: ProgressExerciseUi, new: ProgressExerciseUi) =
                old.exerciseId == new.exerciseId

            override fun areContentsTheSame(old: ProgressExerciseUi, new: ProgressExerciseUi) =
                old == new
        }
    }
}
