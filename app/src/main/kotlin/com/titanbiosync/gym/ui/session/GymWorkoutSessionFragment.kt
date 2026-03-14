package com.titanbiosync.gym.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentGymWorkoutSessionBinding
import com.titanbiosync.gym.domain.WeightUnit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GymWorkoutSessionFragment : Fragment() {

    private var _binding: FragmentGymWorkoutSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GymWorkoutSessionViewModel by viewModels()
    private lateinit var adapter: GymWorkoutSessionExerciseAdapter

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
            }
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.sessionId.text = viewModel.getSessionId()

        binding.endButton.setOnClickListener {
            viewModel.endSession {
                val args = Bundle().apply { putString("sessionId", viewModel.getSessionId()) }
                findNavController().navigate(R.id.workoutReportHostFragment, args)
            }
        }

        viewModel.exercises.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
        }

        viewModel.weightUnit.observe(viewLifecycleOwner) { unitOrNull ->
            val unit = unitOrNull ?: WeightUnit.KG
            binding.unitHint.text = "Unità peso: ${unit.key}"
            adapter.setWeightUnit(unit)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}