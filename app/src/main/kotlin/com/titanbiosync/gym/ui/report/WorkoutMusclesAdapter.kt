package com.titanbiosync.gym.ui.report

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.databinding.RowMuscleWorkBinding
import kotlin.math.roundToInt

class WorkoutMusclesAdapter :
    ListAdapter<MuscleWorkUi, WorkoutMusclesAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowMuscleWorkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: RowMuscleWorkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MuscleWorkUi) {
            binding.muscleName.text = item.muscleNameIt
            binding.intensityBar.progress = (item.intensity * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }
    }

    private object Diff : DiffUtil.ItemCallback<MuscleWorkUi>() {
        override fun areItemsTheSame(oldItem: MuscleWorkUi, newItem: MuscleWorkUi): Boolean =
            oldItem.muscleId == newItem.muscleId

        override fun areContentsTheSame(oldItem: MuscleWorkUi, newItem: MuscleWorkUi): Boolean =
            oldItem == newItem
    }
}