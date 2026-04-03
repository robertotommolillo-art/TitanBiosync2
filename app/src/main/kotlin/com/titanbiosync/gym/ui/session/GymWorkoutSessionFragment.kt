package com.titanbiosync.gym.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
            onUpdateSet = { set, reps, weightKg, completed ->
                viewModel.updateSet(set, reps, weightKg, completed)
            },
            onSetCompleted = { exerciseName, setIndex ->
                viewModel.onSetCompleted(exerciseName, setIndex)
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

        // Rest timer banner
        binding.restTimerMinus.setOnClickListener { viewModel.adjustRestTimer(-15) }
        binding.restTimerPlus.setOnClickListener { viewModel.adjustRestTimer(+15) }
        binding.restTimerStop.setOnClickListener { viewModel.stopRestTimer() }
        binding.restTimerCountdown.setOnClickListener { showRestDurationPicker() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.restTimerState.collect { state ->
                if (state.isRunning) {
                    binding.restTimerBanner.visibility = View.VISIBLE
                    binding.restTimerCountdown.text = formatRestTime(state.remainingSec)
                    binding.restTimerLabel.text = buildRestLabel(state)
                } else {
                    binding.restTimerBanner.visibility = View.GONE
                }
            }
        }
    }

    /** Opens a simple dialog to choose a preset rest duration. */
    private fun showRestDurationPicker() {
        val presets = GymWorkoutSessionViewModel.REST_DURATION_PRESETS
        val labels = presets.map { sec ->
            val mm = sec / 60
            val ss = sec % 60
            when {
                mm > 0 && ss > 0 -> getString(R.string.rest_timer_duration_format_ms, mm, ss)
                mm > 0 -> getString(R.string.rest_timer_duration_format_m, mm)
                else -> getString(R.string.rest_timer_duration_format_s, sec)
            }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.rest_timer_duration_picker_title)
            .setItems(labels) { _, which ->
                viewModel.setRestDuration(presets[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Formats remaining seconds as mm:ss. */
    private fun formatRestTime(totalSeconds: Int): String {
        val mm = totalSeconds / 60
        val ss = totalSeconds % 60
        return "%02d:%02d".format(mm, ss)
    }

    /** Builds the label line showing exercise name + set number. */
    private fun buildRestLabel(state: RestTimerState): String {
        val setNum = state.setIndex + 1
        return if (state.exerciseName.isNotBlank()) {
            getString(R.string.rest_timer_label_with_exercise, state.exerciseName, setNum)
        } else {
            getString(R.string.rest_timer_label_no_exercise, setNum)
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