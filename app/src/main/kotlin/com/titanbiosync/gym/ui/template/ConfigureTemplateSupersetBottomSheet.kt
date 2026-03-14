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
import com.titanbiosync.databinding.BottomSheetConfigureTemplateSupersetBinding
import com.titanbiosync.databinding.ItemTemplateExerciseSetInputBinding
import com.titanbiosync.gym.ui.picker.ExercisePickerViewModel
import java.util.UUID

class ConfigureTemplateSupersetBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetConfigureTemplateSupersetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExercisePickerViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetConfigureTemplateSupersetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val firstId = requireArguments().getString(ARG_FIRST_ID).orEmpty()
        val firstName = requireArguments().getString(ARG_FIRST_NAME).orEmpty()
        val secondId = requireArguments().getString(ARG_SECOND_ID).orEmpty()
        val secondName = requireArguments().getString(ARG_SECOND_NAME).orEmpty()

        binding.firstTitle.text = "A: $firstName"
        binding.secondTitle.text = "B: $secondName"

        // default: 3 round, 10 reps per entrambi
        repeat(3) { addRoundRow(initialRepsA = 10, initialRepsB = 10) }
        updateRemoveEnabled()

        binding.addSetButton.setOnClickListener {
            addRoundRow(initialRepsA = null, initialRepsB = null)
            updateRemoveEnabled()
        }

        binding.removeSetButton.setOnClickListener {
            if (binding.firstSetsContainer.childCount > 1) {
                binding.firstSetsContainer.removeViewAt(binding.firstSetsContainer.childCount - 1)
                binding.secondSetsContainer.removeViewAt(binding.secondSetsContainer.childCount - 1)
            }
            updateRemoveEnabled()
        }

        binding.saveButton.setOnClickListener {
            val repsA = collectReps(binding.firstSetsContainer)
            val repsB = collectReps(binding.secondSetsContainer)

            val restSecondsAfter = binding.restSeconds.text?.toString()?.trim()
                .takeIf { !it.isNullOrBlank() }
                ?.toIntOrNull()

            val notes = binding.notes.text?.toString()?.trim()
                .takeIf { !it.isNullOrBlank() }

            val supersetGroupId = UUID.randomUUID().toString()

            viewModel.addSupersetConfigured(
                firstExerciseId = firstId,
                secondExerciseId = secondId,
                repsBySetA = repsA,
                repsBySetB = repsB,
                restSecondsAfter = restSecondsAfter,
                notes = notes,
                supersetGroupId = supersetGroupId
            ) {
                dismissAllowingStateLoss()
                findNavController().popBackStack()
            }
        }
    }

    private fun addRoundRow(initialRepsA: Int?, initialRepsB: Int?) {
        run {
            val row = ItemTemplateExerciseSetInputBinding.inflate(layoutInflater, binding.firstSetsContainer, false)
            val index = binding.firstSetsContainer.childCount + 1
            row.setLabel.text = "Round $index"
            initialRepsA?.let { row.repsInput.setText(it.toString()) }
            binding.firstSetsContainer.addView(row.root)
        }
        run {
            val row = ItemTemplateExerciseSetInputBinding.inflate(layoutInflater, binding.secondSetsContainer, false)
            val index = binding.secondSetsContainer.childCount + 1
            row.setLabel.text = "Round $index"
            initialRepsB?.let { row.repsInput.setText(it.toString()) }
            binding.secondSetsContainer.addView(row.root)
        }
    }

    private fun updateRemoveEnabled() {
        binding.removeSetButton.isEnabled = binding.firstSetsContainer.childCount > 1
    }

    private fun collectReps(container: ViewGroup): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
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
        private const val ARG_FIRST_ID = "firstId"
        private const val ARG_FIRST_NAME = "firstName"
        private const val ARG_SECOND_ID = "secondId"
        private const val ARG_SECOND_NAME = "secondName"

        fun newInstance(
            first: ExercisePickerViewModel.PickedExercise,
            second: ExercisePickerViewModel.PickedExercise
        ): ConfigureTemplateSupersetBottomSheet {
            return ConfigureTemplateSupersetBottomSheet().apply {
                arguments = bundleOf(
                    ARG_FIRST_ID to first.exerciseId,
                    ARG_FIRST_NAME to first.nameIt,
                    ARG_SECOND_ID to second.exerciseId,
                    ARG_SECOND_NAME to second.nameIt
                )
            }
        }
    }
}