package com.titanbiosync.gym.ui.session

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.data.local.entities.gym.GymWorkoutSetLogEntity
import com.titanbiosync.databinding.ItemGymWorkoutSetBinding
import com.titanbiosync.gym.domain.WeightUnit
import com.titanbiosync.gym.domain.WeightUnitConverter
import com.titanbiosync.ui.common.DecimalDigitsInputFilter
import java.math.BigDecimal
import java.math.RoundingMode

class GymWorkoutSetAdapter(
    private val onUpdate: (set: GymWorkoutSetLogEntity, reps: Int?, weightKg: Float?, completed: Boolean) -> Unit
) : ListAdapter<GymWorkoutSetLogEntity, GymWorkoutSetAdapter.VH>(Diff) {

    private var weightUnit: WeightUnit = WeightUnit.KG
    private var pendingFocusOnLastItem = false
    private var attachedRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        attachedRecyclerView = null
    }

    /** Call this before the new item arrives; the adapter will focus reps on the next submitList commit. */
    fun requestFocusOnNextItem() {
        pendingFocusOnLastItem = true
    }

    override fun submitList(list: List<GymWorkoutSetLogEntity>?) {
        super.submitList(list) {
            if (pendingFocusOnLastItem) {
                pendingFocusOnLastItem = false
                val rv = attachedRecyclerView?.takeIf { it.isAttachedToWindow } ?: return@submitList
                val lastIdx = itemCount - 1
                if (lastIdx < 0) return@submitList
                rv.scrollToPosition(lastIdx)
                rv.post {
                    if (!rv.isAttachedToWindow) return@post
                    val vh = rv.findViewHolderForAdapterPosition(lastIdx) as? VH ?: return@post
                    vh.binding.repsInput.requestFocus()
                    val imm = vh.binding.repsInput.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(vh.binding.repsInput, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        weightUnit = unit
        notifyDataSetChanged()
    }

    object Diff : DiffUtil.ItemCallback<GymWorkoutSetLogEntity>() {
        override fun areItemsTheSame(oldItem: GymWorkoutSetLogEntity, newItem: GymWorkoutSetLogEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GymWorkoutSetLogEntity, newItem: GymWorkoutSetLogEntity) =
            oldItem == newItem
    }

    class VH(val binding: ItemGymWorkoutSetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGymWorkoutSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // Imposta una volta sola: max 2 decimali e un solo punto
        binding.weightInput.filters = arrayOf(DecimalDigitsInputFilter(maxDecimals = 2))

        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding

        b.setLabel.text = "Set ${item.setIndex + 1}"
        b.weightInput.hint = weightUnit.key

        val displayWeight = item.weightKg?.let { kg -> WeightUnitConverter.kgToDisplay(kg, weightUnit) }
        val displayWeightText = displayWeight?.let { formatWeight(it) }.orEmpty()

        if (b.weightInput.text?.toString().orEmpty() != displayWeightText) {
            b.weightInput.setText(displayWeightText)
        }

        val repsText = item.reps?.toString().orEmpty()
        if (b.repsInput.text?.toString().orEmpty() != repsText) {
            b.repsInput.setText(repsText)
        }

        b.doneCheck.setOnCheckedChangeListener(null)
        b.doneCheck.isChecked = item.completed
        b.doneCheck.setOnCheckedChangeListener { _, isChecked ->
            val reps = b.repsInput.text?.toString()?.toIntOrNull()
            val display = parseUserFloat(b.weightInput.text?.toString())
            val kg = display?.let { WeightUnitConverter.displayToKg(it, weightUnit) }
            onUpdate(item, reps, kg, isChecked)
        }

        b.weightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val reps = b.repsInput.text?.toString()?.toIntOrNull()
                val display = parseUserFloat(b.weightInput.text?.toString())
                val kg = display?.let { WeightUnitConverter.displayToKg(it, weightUnit) }
                onUpdate(item, reps, kg, b.doneCheck.isChecked)

                val formatted = display?.let { formatWeight(it) }.orEmpty()
                if (b.weightInput.text?.toString().orEmpty() != formatted) {
                    b.weightInput.setText(formatted)
                }
            }
        }

        b.repsInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val reps = b.repsInput.text?.toString()?.toIntOrNull()
                val display = parseUserFloat(b.weightInput.text?.toString())
                val kg = display?.let { WeightUnitConverter.displayToKg(it, weightUnit) }
                onUpdate(item, reps, kg, b.doneCheck.isChecked)
            }
        }

        b.weightInput.doAfterTextChanged { /* no-op */ }
        b.repsInput.doAfterTextChanged { /* no-op */ }
    }

    private fun parseUserFloat(text: String?): Float? {
        val raw = text?.trim().orEmpty()
        if (raw.isBlank()) return null
        if (raw == "." || raw == "-" || raw == "+") return null
        return raw.toFloatOrNull()
    }

    private fun formatWeight(value: Float): String {
        val bd = BigDecimal(value.toDouble())
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        return bd.toPlainString()
    }
}