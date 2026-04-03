package com.titanbiosync.gym.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentGymWorkoutSessionBinding
import com.titanbiosync.gym.domain.WeightUnit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GymWorkoutSessionFragment : Fragment() {

    private var _binding: FragmentGymWorkoutSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GymWorkoutSessionViewModel by viewModels()
    private lateinit var adapter: GymWorkoutSessionExerciseAdapter

    private var timerJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGymWorkoutSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = GymWorkoutSessionExerciseAdapter(
            lifecycleOwner = viewLifecycleOwner,
            observeSets = { sessionExerciseId -> viewModel.observeSets(sessionExerciseId) },
            onAddSet = { sessionExerciseId -> viewModel.addSet(sessionExerciseId) },
            onUpdateSet = { set, reps, weightKg, completed, rpe ->
                viewModel.updateSet(set, reps, weightKg, completed, rpe)
            }
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.endButton.setOnClickListener {
            viewModel.endSession {
                val args = Bundle().apply { putString("sessionId", viewModel.getSessionId()) }
                findNavController().navigate(R.id.workoutReportHostFragment, args)
            }
        }

        viewModel.exercises.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scrollToNewSet.collect { sessionExerciseId ->
                adapter.scrollToNewSetAndFocus(binding.recycler, sessionExerciseId)
            }
        }

        viewModel.weightUnit.observe(viewLifecycleOwner) { unitOrNull ->
            val unit = unitOrNull ?: WeightUnit.KG
            binding.unitHint.text = "Unità peso: ${unit.key}"
            adapter.setWeightUnit(unit)
        }

        // Start the live elapsed-time timer once we know startedAt.
        viewModel.startedAt.observe(viewLifecycleOwner) { startedAt ->
            timerJob?.cancel()
            if (startedAt != null) {
                timerJob = viewLifecycleOwner.lifecycleScope.launch {
                    while (isActive) {
                        val now = System.currentTimeMillis()
                        val elapsedSec = (now - startedAt) / 1000L
                        binding.timerText.text = formatElapsed(elapsedSec)
                        // Sleep until the start of the next whole second to avoid drift.
                        val msUntilNextTick = 1000L - (now % 1000L)
                        delay(msUntilNextTick)
                    }
                }
            }
        }
    }

    /** Formats elapsed seconds as mm:ss (or hh:mm:ss when ≥ 1 hour). */
    private fun formatElapsed(totalSeconds: Long): String {
        val hh = totalSeconds / 3600
        val mm = (totalSeconds % 3600) / 60
        val ss = totalSeconds % 60
        return if (hh > 0) {
            "%02d:%02d:%02d".format(hh, mm, ss)
        } else {
            "%02d:%02d".format(mm, ss)
        }
    }

    override fun onDestroyView() {
        timerJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}