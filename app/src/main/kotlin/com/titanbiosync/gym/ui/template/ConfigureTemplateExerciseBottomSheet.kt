package com.titanbiosync.gym.ui.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.titanbiosync.R
import com.titanbiosync.databinding.BottomSheetConfigureTemplateExerciseBinding
import com.titanbiosync.databinding.ItemTemplateExerciseSetInputBinding
import com.titanbiosync.gym.ui.picker.ExercisePickerViewModel

class ConfigureTemplateExerciseBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetConfigureTemplateExerciseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExercisePickerViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetConfigureTemplateExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val exerciseId = requireArguments().getString(ARG_EXERCISE_ID).orEmpty()
        val exerciseNameIt = requireArguments().getString(ARG_EXERCISE_NAME_IT).orEmpty()

        binding.title.text =
            if (exerciseNameIt.isNotBlank()) "Configura: $exerciseNameIt" else "Configura esercizio"

        // default: 3 set, 10 reps
        repeat(3) { addSetRow(initialReps = 10) }
        updateRemoveEnabled()

        binding.addSetButton.setOnClickListener {
            addSetRow(initialReps = null)
            updateRemoveEnabled()
        }

        binding.removeSetButton.setOnClickListener {
            val c = binding.setsContainer
            if (c.childCount > 1) c.removeViewAt(c.childCount - 1)
            updateRemoveEnabled()
        }

        binding.saveButton.setOnClickListener {
            val repsBySet = collectReps()

            val restSeconds = binding.restSeconds.text?.toString()?.trim()
                .takeIf { !it.isNullOrBlank() }
                ?.toIntOrNull()

            val notes = binding.notes.text?.toString()?.trim()
                .takeIf { !it.isNullOrBlank() }

            viewModel.addExerciseConfigured(
                exerciseId = exerciseId,
                repsBySet = repsBySet,
                restSeconds = restSeconds,
                notes = notes
            ) {
                dismissAllowingStateLoss()
                findNavController().popBackStack()
            }
        }
    }

    private fun addSetRow(initialReps: Int?) {
        val row = ItemTemplateExerciseSetInputBinding.inflate(layoutInflater, binding.setsContainer, false)
        val index = binding.setsContainer.childCount + 1
        row.setLabel.text = "Serie $index"
        initialReps?.let { row.repsInput.setText(it.toString()) }
        binding.setsContainer.addView(row.root)
    }

    private fun updateRemoveEnabled() {
        binding.removeSetButton.isEnabled = binding.setsContainer.childCount > 1
    }

    private fun collectReps(): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until binding.setsContainer.childCount) {
            val row = binding.setsContainer.getChildAt(i)
            val repsInput: EditText = row.findViewById(R.id.repsInput)
            result += (repsInput.text?.toString()?.trim()?.toIntOrNull() ?: 0)
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_EXERCISE_ID = "exerciseId"
        private const val ARG_EXERCISE_NAME_IT = "exerciseNameIt"

        fun newInstance(exerciseId: String, exerciseNameIt: String?): ConfigureTemplateExerciseBottomSheet {
            return ConfigureTemplateExerciseBottomSheet().apply {
                arguments = bundleOf(
                    ARG_EXERCISE_ID to exerciseId,
                    ARG_EXERCISE_NAME_IT to (exerciseNameIt ?: "")
                )
            }
        }
    }
}