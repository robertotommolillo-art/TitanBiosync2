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
import com.titanbiosync.databinding.FragmentSessionSummaryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SessionSummaryFragment : Fragment() {

    private var _binding: FragmentSessionSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionSummaryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.doneButton.setOnClickListener {
            findNavController().popBackStack()
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.summary?.let { summary ->
                    binding.sessionType.text = summary.sessionType.replaceFirstChar { it.uppercase() }
                    
                    val date = Date(summary.startTime)
                    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    binding.sessionDate.text = format.format(date)

                    val durationSeconds = summary.durationMs / 1000
                    val minutes = durationSeconds / 60
                    val seconds = durationSeconds % 60
                    binding.durationValue.text = String.format("%02d:%02d", minutes, seconds)

                    binding.caloriesValue.text = summary.calories.toString()
                    binding.avgHrValue.text = summary.avgHr.toString()
                    binding.maxHrValue.text = summary.maxHr.toString()

                    val zones = summary.zones
                    binding.zonesText.text = """
                        Zone 1: %.1f%%
                        Zone 2: %.1f%%
                        Zone 3: %.1f%%
                        Zone 4: %.1f%%
                        Zone 5: %.1f%%
                    """.trimIndent().format(
                        zones.zone1Percent, zones.zone2Percent, zones.zone3Percent,
                        zones.zone4Percent, zones.zone5Percent
                    )

                    // Update GPS metrics if available
                    if (summary.distanceKm != null) {
                        binding.gpsMetricsCard.isVisible = true
                        binding.distanceText.text = String.format("%.2f", summary.distanceKm)
                        binding.elevationText.text = String.format("%.0f", summary.elevationGainM ?: 0.0)
                        binding.avgSpeedText.text = String.format("%.1f", summary.avgSpeedKmh ?: 0.0)

                        val paceMinutes = summary.avgPaceMinKm?.toInt() ?: 0
                        val paceSeconds = ((summary.avgPaceMinKm ?: 0.0) % 1 * 60).toInt()
                        binding.avgPaceText.text = String.format("%d:%02d", paceMinutes, paceSeconds)
                    } else {
                        binding.gpsMetricsCard.isVisible = false
                    }
                }

                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}