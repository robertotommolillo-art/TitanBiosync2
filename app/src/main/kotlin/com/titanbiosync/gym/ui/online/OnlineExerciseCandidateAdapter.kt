package com.titanbiosync.gym.ui.online

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.databinding.ItemWorkoutTemplateBinding
import com.titanbiosync.gym.online.model.OnlineExerciseCandidate

class OnlineExerciseCandidateAdapter(
    private val onClick: (OnlineExerciseCandidate) -> Unit
) : ListAdapter<OnlineExerciseCandidate, OnlineExerciseCandidateAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWorkoutTemplateBinding.inflate(
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
        private val binding: ItemWorkoutTemplateBinding,
        private val onClick: (OnlineExerciseCandidate) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnlineExerciseCandidate) {
            binding.title.text = item.title
            binding.subtitle.text = "${item.subtitle} • ${item.sourceName} (${(item.confidence * 100).toInt()}%)"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OnlineExerciseCandidate>() {
            override fun areItemsTheSame(oldItem: OnlineExerciseCandidate, newItem: OnlineExerciseCandidate) =
                oldItem.candidateId == newItem.candidateId

            override fun areContentsTheSame(oldItem: OnlineExerciseCandidate, newItem: OnlineExerciseCandidate) =
                oldItem == newItem
        }
    }
}