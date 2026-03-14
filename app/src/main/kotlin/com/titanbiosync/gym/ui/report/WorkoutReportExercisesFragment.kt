package com.titanbiosync.gym.ui.report

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentWorkoutReportExercisesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutReportExercisesFragment : Fragment(R.layout.fragment_workout_report_exercises) {

    private var _binding: FragmentWorkoutReportExercisesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutReportViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: WorkoutExercisesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentWorkoutReportExercisesBinding.bind(view)

        adapter = WorkoutExercisesAdapter()
        binding.exercisesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.exercisesRecycler.adapter = adapter

        viewModel.exercises.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            binding.exercisesEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(sessionId: String) = WorkoutReportExercisesFragment().apply {
            arguments = Bundle().apply { putString("sessionId", sessionId) }
        }
    }
}