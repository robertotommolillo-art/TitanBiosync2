package com.titanbiosync.gym.ui.report

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.titanbiosync.R
import com.titanbiosync.databinding.ItemExerciseChartCardBinding
import com.titanbiosync.gym.domain.ExerciseSessionPoint

class ExerciseChartAdapter :
    ListAdapter<ExerciseChartUi, ExerciseChartAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseChartCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemExerciseChartCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExerciseChartUi) {
            binding.exerciseName.text = item.exerciseName

            bindSparkline(item.sparklinePoints)

            // Best metric
            binding.statsBestValue.text = if (item.currentBestE1rm != null) {
                binding.statsBestLabel.text = binding.root.context.getString(R.string.chart_label_best_e1rm)
                "%.1f kg".format(item.currentBestE1rm)
            } else {
                binding.statsBestLabel.text = binding.root.context.getString(R.string.chart_label_best)
                "%.1f kg".format(item.currentMaxWeightKg)
            }

            // Volume
            binding.statsVolumeValue.text = "%.0f kg".format(item.currentVolume)

            // Delta
            val delta = item.deltaPrimary
            if (delta == null) {
                binding.statsDeltaValue.text = binding.root.context.getString(R.string.chart_delta_no_prev)
            } else {
                val sign = if (delta >= 0f) "+" else ""
                binding.statsDeltaValue.text = "%s%.1f kg".format(sign, delta)
                val colorRes = if (delta >= 0f) R.color.progress_positive else R.color.progress_negative
                val color = ContextCompat.getColor(binding.root.context, colorRes)
                binding.statsDeltaValue.setTextColor(color)
            }
        }

        private fun bindSparkline(points: List<ExerciseSessionPoint>) {
            val chart = binding.sparklineChart
            chart.setTouchEnabled(false)
            chart.description.isEnabled = false
            chart.legend.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.setDrawBorders(false)
            chart.xAxis.isEnabled = false
            chart.axisLeft.isEnabled = false
            chart.axisRight.isEnabled = false
            chart.extraBottomOffset = 4f

            if (points.isEmpty()) {
                chart.clear()
                return
            }

            val values = points.map { it.primaryValue }
            val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }

            val lineColor = ContextCompat.getColor(binding.root.context, R.color.chart_line_color)
            val lineFillColor = ContextCompat.getColor(binding.root.context, R.color.chart_fill_color)

            val dataSet = LineDataSet(entries, "").apply {
                this.color = lineColor
                lineWidth = 2f
                setDrawCircles(points.size <= 12)
                setCircleColor(lineColor)
                circleRadius = 3f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = lineFillColor
                fillAlpha = 40
                axisDependency = YAxis.AxisDependency.LEFT
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ExerciseChartUi>() {
            override fun areItemsTheSame(old: ExerciseChartUi, new: ExerciseChartUi) =
                old.exerciseId == new.exerciseId

            override fun areContentsTheSame(old: ExerciseChartUi, new: ExerciseChartUi) =
                old == new
        }
    }
}
