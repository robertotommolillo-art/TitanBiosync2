package com.titanbiosync.gym.ui.report

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentWorkoutReportMusclesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutReportMusclesFragment : Fragment(R.layout.fragment_workout_report_muscles) {

    private var _binding: FragmentWorkoutReportMusclesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutReportViewModel by viewModels({ requireParentFragment() })
    private lateinit var musclesAdapter: WorkoutMusclesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentWorkoutReportMusclesBinding.bind(view)

        musclesAdapter = WorkoutMusclesAdapter()
        binding.musclesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.musclesRecycler.adapter = musclesAdapter

        viewModel.muscles.observe(viewLifecycleOwner) { rows ->
            musclesAdapter.submitList(rows)
            binding.musclesEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(sessionId: String) = WorkoutReportMusclesFragment().apply {
            arguments = Bundle().apply { putString("sessionId", sessionId) }
        }
    }
}