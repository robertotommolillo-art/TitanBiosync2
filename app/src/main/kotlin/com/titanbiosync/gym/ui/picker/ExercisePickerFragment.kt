package com.titanbiosync.gym.ui.picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentExercisePickerBinding
import com.titanbiosync.gym.ui.online.ExerciseImportPreviewFragment
import com.titanbiosync.gym.ui.template.ConfigureTemplateExerciseBottomSheet
import com.titanbiosync.gym.ui.template.ConfigureTemplateSupersetBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExercisePickerFragment : Fragment() {

    private var _binding: FragmentExercisePickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExercisePickerViewModel by viewModels()

    private var currentQuery: String = ""

    /** First exercise chosen when user starts a superset. Null = normal pick mode. */
    private var pendingSupersetFirst: ExercisePickerViewModel.PickedExercise? = null

    private val adapter = ExercisePickerAdapter { exercise ->
        val pending = pendingSupersetFirst
        if (pending != null) {
            // Step 2 of superset: open superset configuration sheet
            val second = ExercisePickerViewModel.PickedExercise(
                exerciseId = exercise.id,
                nameIt = exercise.nameIt
            )
            clearSupersetState()
            ConfigureTemplateSupersetBottomSheet
                .newInstance(first = pending, second = second)
                .show(childFragmentManager, "ConfigureTemplateSupersetBottomSheet")
        } else {
            // Ask user: Normal or Superset?
            AlertDialog.Builder(requireContext())
                .setTitle(exercise.nameIt)
                .setMessage("Come vuoi aggiungere questo esercizio?")
                .setPositiveButton("Normale") { _, _ ->
                    ConfigureTemplateExerciseBottomSheet
                        .newInstance(exerciseId = exercise.id, exerciseNameIt = exercise.nameIt)
                        .show(childFragmentManager, "ConfigureTemplateExerciseBottomSheet")
                }
                .setNeutralButton("Superserie") { _, _ ->
                    pendingSupersetFirst = ExercisePickerViewModel.PickedExercise(
                        exerciseId = exercise.id,
                        nameIt = exercise.nameIt
                    )
                    showSupersetBanner(exercise.nameIt)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExercisePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.cancelSupersetButton.setOnClickListener {
            clearSupersetState()
        }

        binding.searchOnlineButton.setOnClickListener {
            val q = currentQuery.trim()
            if (q.isBlank()) return@setOnClickListener

            val args = Bundle().apply { putString("query", q) }
            findNavController().navigate(R.id.onlineExerciseSearchFragment, args)
        }

        binding.search.doOnTextChanged { text, _, _, _ ->
            currentQuery = text?.toString().orEmpty()
            viewModel.setQuery(currentQuery)
        }

        viewModel.exercises.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)

            val showOnline = items.isNullOrEmpty() && currentQuery.trim().isNotBlank()
            binding.searchOnlineButton.visibility = if (showOnline) View.VISIBLE else View.GONE
            binding.searchOnlineButton.text = "Cerca online \"$currentQuery\""
        }

        // When returning from online import, open configure sheet for the imported exercise.
        observeImportedExercise()
    }

    /** Shows the superset banner with the name of the first exercise. */
    private fun showSupersetBanner(firstName: String) {
        binding.supersetBannerText.text =
            "Superserie: \"$firstName\" selezionato. Scegli il 2° esercizio."
        binding.supersetBanner.visibility = View.VISIBLE
    }

    /** Clears the pending superset state and hides the banner. */
    private fun clearSupersetState() {
        pendingSupersetFirst = null
        binding.supersetBanner.visibility = View.GONE
    }

    /** Observes the SavedStateHandle result set by [ExerciseImportPreviewFragment] and, when
     *  an imported exercise id arrives, opens [ConfigureTemplateExerciseBottomSheet] once. */
    private fun observeImportedExercise() {
        val savedState = findNavController().currentBackStackEntry?.savedStateHandle ?: return

        savedState.getLiveData<String>(ExerciseImportPreviewFragment.KEY_IMPORTED_EXERCISE_ID)
            .observe(viewLifecycleOwner) { exerciseId ->
                if (exerciseId.isNullOrEmpty()) return@observe

                val nameIt = savedState.get<String>(
                    ExerciseImportPreviewFragment.KEY_IMPORTED_EXERCISE_NAME_IT
                ).orEmpty()

                // Consume the result so it doesn't fire again on rotation / re-observe.
                savedState.remove<String>(ExerciseImportPreviewFragment.KEY_IMPORTED_EXERCISE_ID)
                savedState.remove<String>(ExerciseImportPreviewFragment.KEY_IMPORTED_EXERCISE_NAME_IT)

                ConfigureTemplateExerciseBottomSheet
                    .newInstance(exerciseId = exerciseId, exerciseNameIt = nameIt)
                    .show(childFragmentManager, "ConfigureTemplateExerciseBottomSheet")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}