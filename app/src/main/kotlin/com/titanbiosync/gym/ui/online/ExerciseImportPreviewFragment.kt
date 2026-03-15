package com.titanbiosync.gym.ui.online

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentExerciseImportPreviewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseImportPreviewFragment : Fragment() {

    private var _binding: FragmentExerciseImportPreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseImportPreviewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseImportPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.load()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resolved.collect { r ->
                if (r == null) return@collect
                binding.title.text = r.nameIt
                binding.source.text = "Fonte: ${r.sourceName ?: "n/a"}"
                binding.meta.text = "Categoria: ${r.category}\nEquipment: ${r.equipment ?: "-"}\nMeccanica: ${r.mechanics ?: "-"}\nLivello: ${r.level ?: "-"}"
                binding.muscles.text = "Muscoli:\n" + r.muscles.joinToString("\n") { "• ${it.muscleId} (${it.role}, w=${it.weight})" }
                binding.description.text = r.descriptionIt ?: ""
            }
        }

        binding.saveButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Salvare l'esercizio?")
                .setMessage("Verrà aggiunto al tuo catalogo locale.")
                .setPositiveButton("Salva") { _, _ ->
                    viewModel.save(
                        onDone = { exerciseId, nameIt ->
                            returnToPickerWithResult(exerciseId, nameIt)
                        },
                        onError = { t ->
                            AlertDialog.Builder(requireContext())
                                .setTitle("Errore")
                                .setMessage(t.message ?: t.toString())
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    )
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    /** Passes the imported exercise result to ExercisePickerFragment via SavedStateHandle
     *  and pops the back stack back to the picker. */
    private fun returnToPickerWithResult(exerciseId: String, nameIt: String) {
        val navController = findNavController()

        // Try to place the result on the picker entry's SavedStateHandle so it can
        // observe and open the configure bottom sheet automatically.
        try {
            val pickerEntry = navController.getBackStackEntry(R.id.exercisePickerFragment)
            pickerEntry.savedStateHandle[KEY_IMPORTED_EXERCISE_ID] = exerciseId
            pickerEntry.savedStateHandle[KEY_IMPORTED_EXERCISE_NAME_IT] = nameIt
        } catch (_: IllegalArgumentException) {
            // exercisePickerFragment not in back stack — fall through and just pop
        }

        val popped = navController.popBackStack(R.id.exercisePickerFragment, false)
        if (!popped) {
            // Fallback: pop at least the current fragment (e.g. deep-linked entry point)
            navController.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_IMPORTED_EXERCISE_ID = "imported_exercise_id"
        const val KEY_IMPORTED_EXERCISE_NAME_IT = "imported_exercise_name_it"
    }
}