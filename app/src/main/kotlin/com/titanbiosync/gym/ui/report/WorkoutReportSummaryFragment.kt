package com.titanbiosync.gym.ui.report

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentWorkoutReportSummaryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutReportSummaryFragment : Fragment(R.layout.fragment_workout_report_summary) {

    private var _binding: FragmentWorkoutReportSummaryBinding? = null
    private val binding get() = _binding!!

    // ViewModel condiviso dal parent (Host)
    private val viewModel: WorkoutReportViewModel by viewModels({ requireParentFragment() })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Non è strettamente necessario leggere qui sessionId: lo gestisce il parent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentWorkoutReportSummaryBinding.bind(view)

        viewModel.summary.observe(viewLifecycleOwner) { s ->
            binding.summaryText.text = buildString {
                append("Sessione: ${s.sessionId}\n")
                append("Durata: ${s.durationMinutes} min\n")
                append("Set completati: ${s.completedSets}\n")
                append("Volume: ${"%.0f".format(s.totalVolume)} kg\n")
                if (s.previousVolume != null) {
                    val delta = s.totalVolume - s.previousVolume
                    append("Volume precedente: ${"%.0f".format(s.previousVolume)} kg\n")
                    append("Delta: ${"%.0f".format(delta)} kg\n")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(sessionId: String) = WorkoutReportSummaryFragment().apply {
            arguments = Bundle().apply { putString("sessionId", sessionId) }
        }
    }
}