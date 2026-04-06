package com.titanbiosync.gym.ui.progress

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentExerciseProgressDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ExerciseProgressDetailFragment : Fragment(R.layout.fragment_exercise_progress_detail) {

    private var _binding: FragmentExerciseProgressDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExerciseProgressDetailViewModel by viewModels()
    private val args: ExerciseProgressDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentExerciseProgressDetailBinding.bind(view)

        binding.exerciseNameTitle.text = args.exerciseName

        setupChipListeners()
        setupChart()

        viewModel.chartUi.observe(viewLifecycleOwner) { ui ->
            val hasData = ui.points.isNotEmpty()
            binding.progressChart.isVisible = hasData
            binding.noDataText.isVisible = !hasData

            if (hasData) {
                bindChart(ui.points, ui.metric, ui.yLabel)
            } else {
                binding.progressChart.clear()
            }
        }
    }

    private fun setupChipListeners() {
        binding.chipE1rm.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.setMetric(ProgressMetric.E1RM)
        }
        binding.chipWeight.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.setMetric(ProgressMetric.MAX_WEIGHT)
        }
        binding.chipVolume.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.setMetric(ProgressMetric.VOLUME)
        }

        binding.chip30d.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.setRange(ProgressRange.DAYS_30)
        }
        binding.chip90d.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.setRange(ProgressRange.DAYS_90)
        }
        binding.chip365d.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.setRange(ProgressRange.DAYS_365)
        }
        binding.chipAll.setOnCheckedChangeListener { _, checked ->
            if (checked) viewModel.setRange(ProgressRange.ALL_TIME)
        }
    }

    private fun setupChart() {
        binding.progressChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                valueFormatter = DateAxisFormatter()
            }

            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
            }
            axisRight.isEnabled = false

            extraBottomOffset = 8f
        }
    }

    private fun bindChart(points: List<ExerciseSessionPoint>, metric: ProgressMetric, yLabel: String) {
        val entries = points.mapIndexed { i, pt ->
            val y = when (metric) {
                ProgressMetric.E1RM -> pt.bestE1rm ?: pt.maxWeightKg
                ProgressMetric.MAX_WEIGHT -> pt.maxWeightKg
                ProgressMetric.VOLUME -> pt.totalVolume
            }
            Entry(i.toFloat(), y).also { it.data = pt.sessionDate }
        }

        val lineColor = requireContext().getColor(R.color.chart_line_color)
        val lineFillColor = requireContext().getColor(R.color.chart_fill_color)

        val dataSet = LineDataSet(entries, yLabel).apply {
            this.color = lineColor
            lineWidth = 2f
            setDrawCircles(entries.size <= 30)
            setCircleColor(lineColor)
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = lineFillColor
            fillAlpha = 40
            axisDependency = YAxis.AxisDependency.LEFT
        }

        binding.progressChart.data = LineData(dataSet)

        // Update x-axis formatter with actual timestamps
        val timestamps = points.map { it.sessionDate }
        (binding.progressChart.xAxis.valueFormatter as? DateAxisFormatter)?.setTimestamps(timestamps)

        binding.progressChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class DateAxisFormatter : ValueFormatter() {
        private var timestamps: List<Long> = emptyList()
        private val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())

        fun setTimestamps(ts: List<Long>) {
            timestamps = ts
        }

        override fun getFormattedValue(value: Float): String {
            val idx = value.toInt()
            return if (idx in timestamps.indices) fmt.format(Date(timestamps[idx])) else ""
        }
    }
}
