package com.titanbiosync.gym.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()

    private var calendarIsSetup = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        setupHeader()
        setupCalendarIfNeeded()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resetToCurrentMonth()
        binding.calendarView.scrollToMonth(YearMonth.now())
    }

    private fun setupRecycler() {
        binding.sessionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.sessionsRecycler.adapter = HistorySessionsAdapter(
            onClick = { session ->
                findNavController().navigate(
                    R.id.gymWorkoutSessionFragment,
                    bundleOf("sessionId" to session.id)
                )
            }
        )
    }

    private fun setupHeader() {
        binding.prevMonthButton.setOnClickListener { viewModel.goToPreviousMonth() }
        binding.nextMonthButton.setOnClickListener { viewModel.goToNextMonth() }
    }

    private fun setupCalendarIfNeeded() {
        if (calendarIsSetup) return
        calendarIsSetup = true

        val currentMonth = viewModel.uiState.visibleMonth
        val startMonth = currentMonth.minusMonths(24)
        val endMonth = currentMonth.plusMonths(24)

        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek = DayOfWeek.MONDAY)
        binding.calendarView.scrollToMonth(currentMonth)

        binding.calendarView.dayBinder = object : MonthDayBinder<HistoryDayViewContainer> {
            override fun create(view: View) = HistoryDayViewContainer(view)

            override fun bind(container: HistoryDayViewContainer, data: CalendarDay) {
                val date = data.date
                container.dayText.text = date.dayOfMonth.toString()

                val isInMonth = data.position == DayPosition.MonthDate
                container.dayText.isVisible = isInMonth

                if (!isInMonth) {
                    container.dot.isVisible = false
                    container.root.isSelected = false
                    return
                }

                val state = viewModel.uiState
                val hasWorkout = state.daysWithSessions.contains(date)
                container.dot.isVisible = hasWorkout

                val selected = state.selectedDate == date
                container.root.isSelected = selected

                container.root.setOnClickListener { viewModel.selectDate(date) }
            }
        }

        binding.calendarView.monthScrollListener = { month ->
            viewModel.onMonthScrolled(month.yearMonth)
        }

        // Weekday labels (Mon..Sun)
        val locale = Locale.getDefault()
        val dows = daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY)
        binding.weekdayMon.text = dows[0].getDisplayName(TextStyle.SHORT, locale)
        binding.weekdayTue.text = dows[1].getDisplayName(TextStyle.SHORT, locale)
        binding.weekdayWed.text = dows[2].getDisplayName(TextStyle.SHORT, locale)
        binding.weekdayThu.text = dows[3].getDisplayName(TextStyle.SHORT, locale)
        binding.weekdayFri.text = dows[4].getDisplayName(TextStyle.SHORT, locale)
        binding.weekdaySat.text = dows[5].getDisplayName(TextStyle.SHORT, locale)
        binding.weekdaySun.text = dows[6].getDisplayName(TextStyle.SHORT, locale)
    }

    private fun observeState() {
        viewModel.uiStateLive.observe(viewLifecycleOwner) { state ->
            val locale = Locale.getDefault()
            val monthName = state.visibleMonth.month.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            binding.monthTitle.text = "$monthName ${state.visibleMonth.year}"

            (binding.sessionsRecycler.adapter as? HistorySessionsAdapter)
                ?.submitList(state.sessionsForSelectedDate)

            binding.selectedDateTitle.isVisible = state.selectedDate != null
            binding.selectedDateTitle.text = state.selectedDate?.toString().orEmpty()

            binding.emptyText.isVisible =
                state.selectedDate != null && state.sessionsForSelectedDate.isEmpty()

            binding.calendarView.notifyCalendarChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}