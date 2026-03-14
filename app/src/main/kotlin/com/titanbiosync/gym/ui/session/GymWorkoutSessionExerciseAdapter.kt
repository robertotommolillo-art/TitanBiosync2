package com.titanbiosync.gym.ui.session

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.GymWorkoutSessionExerciseEntity
import com.titanbiosync.data.local.entities.gym.GymWorkoutSetLogEntity
import com.titanbiosync.databinding.ItemGymWorkoutSessionExerciseBinding
import com.titanbiosync.gym.domain.WeightUnit

class GymWorkoutSessionExerciseAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val observeSets: (sessionExerciseId: String) -> LiveData<List<GymWorkoutSetLogEntity>>,
    private val onAddSet: (sessionExerciseId: String) -> Unit,
    private val onUpdateSet: (set: GymWorkoutSetLogEntity, reps: Int?, weightKg: Float?, completed: Boolean) -> Unit
) : ListAdapter<GymWorkoutSessionExerciseEntity, GymWorkoutSessionExerciseAdapter.VH>(Diff) {

    private var weightUnit: WeightUnit = WeightUnit.KG

    fun setWeightUnit(unit: WeightUnit) {
        weightUnit = unit
        notifyDataSetChanged()
    }

    object Diff : DiffUtil.ItemCallback<GymWorkoutSessionExerciseEntity>() {
        override fun areItemsTheSame(oldItem: GymWorkoutSessionExerciseEntity, newItem: GymWorkoutSessionExerciseEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GymWorkoutSessionExerciseEntity, newItem: GymWorkoutSessionExerciseEntity) =
            oldItem == newItem
    }

    class VH(
        val binding: ItemGymWorkoutSessionExerciseBinding,
        val setsAdapter: GymWorkoutSetAdapter
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGymWorkoutSessionExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        val setsAdapter = GymWorkoutSetAdapter(onUpdate = onUpdateSet).apply {
            setWeightUnit(weightUnit)
        }

        binding.setsRecycler.layoutManager = LinearLayoutManager(parent.context)
        binding.setsRecycler.adapter = setsAdapter

        return VH(binding, setsAdapter)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding

        b.name.text = item.nameItSnapshot
        holder.setsAdapter.setWeightUnit(weightUnit)

        observeSets(item.id).observe(lifecycleOwner) { sets ->
            holder.setsAdapter.submitList(sets)
        }

        b.addSetButton.setOnClickListener {
            onAddSet(item.id)
        }
    }
}