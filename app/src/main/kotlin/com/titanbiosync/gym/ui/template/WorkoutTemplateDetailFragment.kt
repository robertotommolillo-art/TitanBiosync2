package com.titanbiosync.gym.ui.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentWorkoutTemplateDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutTemplateDetailFragment : Fragment() {

    private var _binding: FragmentWorkoutTemplateDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutTemplateDetailViewModel by viewModels()

    private val adapter = TemplateExerciseAdapter { row ->
        val msg = if (row.supersetGroupId != null) {
            "Verranno rimossi entrambi gli esercizi della superserie."
        } else {
            row.nameIt
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Rimuovere esercizio?")
            .setMessage(msg)
            .setPositiveButton("Rimuovi") { _, _ ->
                viewModel.removeExerciseAt(row.position, row.supersetGroupId)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutTemplateDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.exercisesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.exercisesRecycler.adapter = adapter

        binding.addFab.setOnClickListener {
            Toast.makeText(requireContext(), "CLICK SINGLE FAB", Toast.LENGTH_SHORT).show()
            val args = Bundle().apply {
                putString("templateId", viewModel.getTemplateId())
                putString("mode", "SINGLE")
            }
            findNavController().navigate(R.id.exercisePickerFragment, args)
        }


        binding.startWorkoutButton.setOnClickListener {
            viewModel.startWorkout { sessionId ->
                val args = Bundle().apply { putString("sessionId", sessionId) }
                findNavController().navigate(R.id.gymWorkoutSessionFragment, args)
            }
        }

        binding.editButton.setOnClickListener {
            val currentName = binding.title.text?.toString().orEmpty()

            val input = EditText(requireContext()).apply {
                setText(currentName)
                setSelection(currentName.length)
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Rinomina scheda")
                .setView(input)
                .setPositiveButton("Salva") { _, _ ->
                    viewModel.renameTemplate(input.text?.toString().orEmpty())
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        binding.deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminare scheda?")
                .setMessage("Verrà eliminata la scheda e tutti i suoi esercizi.")
                .setPositiveButton("Elimina") { _, _ ->
                    viewModel.deleteTemplate {
                        findNavController().popBackStack()
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        viewModel.template.observe(viewLifecycleOwner) { template ->
            binding.title.text = template?.name ?: "Scheda"
        }

        viewModel.rows.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            binding.empty.visibility = if (rows.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.startWorkoutButton.isEnabled = !rows.isNullOrEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}