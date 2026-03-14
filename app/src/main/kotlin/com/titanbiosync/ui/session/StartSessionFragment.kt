package com.titanbiosync.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentStartSessionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StartSessionFragment : Fragment() {

    private var _binding: FragmentStartSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StartSessionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupStateSubscription()
    }

    private fun setupClickListeners() {
        binding.runningCard.setOnClickListener {
            viewModel.startSession("running")
        }

        binding.cyclingCard.setOnClickListener {
            viewModel.startSession("cycling")
        }

        // Workout rimosso (UI + logica)
        // binding.workoutCard.setOnClickListener { viewModel.startSession("workout") }
    }

    private fun setupStateSubscription() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.isVisible = state.isLoading
                binding.statusText.text = if (state.isLoading) {
                    getString(R.string.start_session_starting)
                } else {
                    ""
                }

                state.startedSession?.let { session ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.start_session_started, session.type),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    val action = StartSessionFragmentDirections
                        .actionStartSessionToSessionDetail(session.id)
                    findNavController().navigate(action)
                    viewModel.resetState()
                }

                state.error?.let { error ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.start_session_error, error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}