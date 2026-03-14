package com.titanbiosync.gym.ui.report

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.databinding.RowExerciseWorkBinding

class WorkoutExercisesAdapter :
    ListAdapter<ExerciseWorkUi, WorkoutExercisesAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowExerciseWorkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: RowExerciseWorkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExerciseWorkUi) {
            binding.exerciseName.text = item.nameIt
            binding.exerciseVolume.text = "Volume: ${"%.0f".format(item.volume)} kg"
        }
    }

    private object Diff : DiffUtil.ItemCallback<ExerciseWorkUi>() {
        override fun areItemsTheSame(oldItem: ExerciseWorkUi, newItem: ExerciseWorkUi) =
            oldItem.sessionExerciseId == newItem.sessionExerciseId

        override fun areContentsTheSame(oldItem: ExerciseWorkUi, newItem: ExerciseWorkUi) =
            oldItem == newItem
    }
}