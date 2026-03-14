package com.titanbiosync.ui.session

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.snackbar.Snackbar
import com.titanbiosync.R
import com.titanbiosync.ble.BleManager
import com.titanbiosync.databinding.FragmentSessionDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SessionDetailFragment : Fragment() {

    private var _binding: FragmentSessionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionDetailViewModel by viewModels()

    @Inject
    lateinit var bleManager: BleManager

    private var googleMap: GoogleMap? = null
    private var mapFragment: SupportMapFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChart()
        setupMap()
        setupClickListeners()
        subscribeToState()
        observeBleHeartRate()
    }

    private fun setupMap() {
        mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map

            try {
                map.isMyLocationEnabled = true
            } catch (_: SecurityException) {
                // Permission not granted yet
            }

            map.uiSettings.isZoomControlsEnabled = true
        }
    }

    private fun setupChart() {
        binding.heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.DKGRAY
            }

            axisLeft.apply {
                textColor = Color.DKGRAY
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun setupClickListeners() {
        binding.endSessionButton.setOnClickListener {
            viewModel.endSession()
        }

        binding.simulateDataButton.setOnClickListener {
            viewModel.simulateLiveData()
        }
    }

    private fun subscribeToState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.isVisible = state.isLoading

                binding.sessionTypeText.text = "Type: ${state.session?.type ?: "Unknown"}"

                val durationText = if (state.session != null) {
                    val durationMs =
                        (state.session.endedAt ?: System.currentTimeMillis()) - state.session.startedAt
                    val minutes = (durationMs / 1000 / 60).toInt()
                    val seconds = (durationMs / 1000 % 60).toInt()
                    "Duration: ${minutes}m ${seconds}s"
                } else {
                    "Duration: Active"
                }
                binding.sessionDurationText.text = durationText

                binding.sessionStatusText.text =
                    if (state.sessionEnded) "Status: Ended" else "Status: Active"

                updateChart(state.sensorReadings.mapNotNull { it.value.toFloatOrNull() })

                val hrReadings = state.sensorReadings
                    .filter { it.sensorType == "heart_rate" }
                    .mapNotNull { it.value.toFloatOrNull() }

                if (hrReadings.isNotEmpty()) {
                    val currentHr = hrReadings.lastOrNull() ?: 0f
                    val avgHr = hrReadings.average().toInt()
                    val maxHr = hrReadings.maxOrNull()?.toInt() ?: 0

                    binding.currentHrText.text = "${currentHr.toInt()} bpm"
                    binding.avgHrText.text = avgHr.toString()
                    binding.maxHrText.text = maxHr.toString()
                }

                binding.dataPointsText.text = state.sensorReadings.size.toString()

                if (state.sessionEnded) {
                    val action = SessionDetailFragmentDirections
                        .actionSessionDetailToSummary(viewModel.getSessionId())
                    findNavController().navigate(action)
                }

                state.error?.let { error ->
                    Snackbar.make(binding.root, "Error: $error", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeBleHeartRate() {
        viewLifecycleOwner.lifecycleScope.launch {
            bleManager.heartRate.collect { hr ->
                hr?.let { viewModel.recordHeartRate(it) }
            }
        }
    }

    private fun updateChart(hrValues: List<Float>) {
        if (hrValues.isEmpty()) return

        val entries = hrValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Heart Rate (bpm)").apply {
            color = Color.rgb(233, 30, 99)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(233, 30, 99))
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = Color.rgb(233, 30, 99)
            fillAlpha = 50
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.heartRateChart.apply {
            data = LineData(dataSet)
            notifyDataSetChanged()
            invalidate()
            setVisibleXRangeMaximum(20f)
            moveViewToX(data.entryCount.toFloat())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}