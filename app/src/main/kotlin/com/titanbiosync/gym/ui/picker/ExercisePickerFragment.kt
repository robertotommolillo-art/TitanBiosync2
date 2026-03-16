package com.titanbiosync.gym.ui.picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentExercisePickerBinding
import com.titanbiosync.gym.ui.online.ExerciseImportPreviewFragment
import com.titanbiosync.gym.ui.template.ConfigureTemplateExerciseBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExercisePickerFragment : Fragment() {

    private var _binding: FragmentExercisePickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExercisePickerViewModel by viewModels()

    private var currentQuery: String = ""

    private val adapter = ExercisePickerAdapter { exercise ->
        ConfigureTemplateExerciseBottomSheet
            .newInstance(exerciseId = exercise.id, exerciseNameIt = exercise.nameIt)
            .show(childFragmentManager, "ConfigureTemplateExerciseBottomSheet")
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

        // --- ascolta risultato import online ---
        val navController = findNavController()
        val handle = navController.currentBackStackEntry?.savedStateHandle

        handle?.getLiveData<String>("imported_exercise_id")
            ?.observe(viewLifecycleOwner, Observer { exerciseId ->
                if (exerciseId.isNullOrBlank()) return@Observer

                val nameIt = handle.get<String>("imported_exercise_name_it").orEmpty()

                // Consuma il risultato (evita ri-trigger al rotate)
                handle.remove<String>("imported_exercise_id")
                handle.remove<String>("imported_exercise_name_it")

                ConfigureTemplateExerciseBottomSheet
                    .newInstance(exerciseId = exerciseId, exerciseNameIt = nameIt)
                    .show(childFragmentManager, "ConfigureTemplateExerciseBottomSheet")
            })

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