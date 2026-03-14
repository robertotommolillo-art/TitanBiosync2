package com.titanbiosync.gym.ui.report

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WorkoutReportPagerAdapter(
    host: Fragment,
    private val sessionId: String
) : FragmentStateAdapter(host) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val args = Bundle().apply { putString("sessionId", sessionId) }

        return when (position) {
            0 -> WorkoutReportSummaryFragment().apply { arguments = args }
            1 -> WorkoutReportExercisesFragment().apply { arguments = args }
            2 -> WorkoutReportMusclesFragment().apply { arguments = args }
            else -> Fragment()
        }
    }
}