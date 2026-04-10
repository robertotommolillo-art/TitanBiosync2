package com.titanbiosync.coach.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.titanbiosync.databinding.FragmentAiCoachBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiCoachFragment : Fragment() {

    private var _binding: FragmentAiCoachBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AiCoachViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiCoachBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupInputArea()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.chatRecycler.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.chatRecycler.adapter = adapter
    }

    private fun setupInputArea() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.messageInput.text?.clear()
            }
        }

        binding.generateButton.setOnClickListener {
            showGenerateDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.chatRecycler.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.sendButton.isEnabled = !loading
            binding.generateButton.isEnabled = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    .setAction("OK") { viewModel.clearError() }
                    .show()
            }
        }

        viewModel.pendingPlan.observe(viewLifecycleOwner) { plan ->
            if (plan != null) {
                showPlanConfirmationDialog(plan.title, plan.exercises.size)
            }
        }

        viewModel.savedTemplateId.observe(viewLifecycleOwner) { templateId ->
            if (templateId != null) {
                Snackbar.make(
                    binding.root,
                    "Scheda salvata nella cartella \"${AiCoachRepository.AI_FOLDER_NAME}\"!",
                    Snackbar.LENGTH_LONG
                ).show()
                viewModel.clearSavedTemplateId()
            }
        }
    }

    private fun showGenerateDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(
            com.titanbiosync.R.layout.dialog_generate_workout,
            null
        )
        val inputField = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.titanbiosync.R.id.specInput
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Genera scheda")
            .setMessage("Descrivi la scheda che vuoi generare (es. 'push 4 giorni, ipertrofia, intermedio'):")
            .setView(dialogView)
            .setPositiveButton("Genera") { _, _ ->
                val spec = inputField?.text?.toString().orEmpty()
                if (spec.isNotBlank()) {
                    viewModel.generateWorkout(spec)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showPlanConfirmationDialog(title: String, exerciseCount: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Salva scheda")
            .setMessage(
                "Vuoi salvare la scheda \"$title\" ($exerciseCount esercizi) " +
                        "nella cartella \"${AiCoachRepository.AI_FOLDER_NAME}\"?"
            )
            .setPositiveButton("Salva in Gym") { _, _ -> viewModel.confirmSavePlan() }
            .setNegativeButton("Annulla") { _, _ -> viewModel.discardPlan() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
