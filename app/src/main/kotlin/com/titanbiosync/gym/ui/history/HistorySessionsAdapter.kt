package com.titanbiosync.gym.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionEntity
import com.titanbiosync.databinding.ItemHistorySessionBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistorySessionsAdapter(
    private val onClick: (GymWorkoutSessionEntity) -> Unit
) : ListAdapter<GymWorkoutSessionEntity, HistorySessionsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistorySessionBinding.inflate(
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
        private val binding: ItemHistorySessionBinding,
        private val onClick: (GymWorkoutSessionEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val zoneId = ZoneId.systemDefault()
        private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        fun bind(item: GymWorkoutSessionEntity) {
            val startTime = Instant.ofEpochMilli(item.startedAt).atZone(zoneId).toLocalTime()
            binding.title.text = "Allenamento ${startTime.format(timeFmt)}"

            val durationMin = item.endedAt?.let { ended ->
                ((ended - item.startedAt) / 60000L).toInt().coerceAtLeast(0)
            }
            binding.subtitle.text = durationMin?.let { "Durata: ${it} min" } ?: "In corso / senza fine"

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<GymWorkoutSessionEntity>() {
        override fun areItemsTheSame(oldItem: GymWorkoutSessionEntity, newItem: GymWorkoutSessionEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GymWorkoutSessionEntity, newItem: GymWorkoutSessionEntity) =
            oldItem == newItem
    }
}