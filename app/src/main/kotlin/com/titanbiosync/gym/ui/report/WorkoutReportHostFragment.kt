package com.titanbiosync.gym.ui.report

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentWorkoutReportHostBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WorkoutReportHostFragment : Fragment(R.layout.fragment_workout_report_host) {

    private var _binding: FragmentWorkoutReportHostBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentWorkoutReportHostBinding.bind(view)

        val sessionId = requireArguments().getString("sessionId").orEmpty()
        requireArguments().putString("sessionId", sessionId)

        val adapter = WorkoutReportPagerAdapter(this, sessionId)
        binding.pager.adapter = adapter

        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Summary"
                1 -> "Exercises"
                2 -> "Muscles"
                else -> ""
            }
        }.attach()

        val goToLibrary: () -> Unit = {
            val navOptions = NavOptions.Builder()
                // Ripulisce lo stack fino alla dashboard (startDestination)
                .setPopUpTo(R.id.dashboardFragment, false)
                .build()

            findNavController().navigate(R.id.gymLibraryFragment, null, navOptions)
        }

        binding.toolbar.setNavigationOnClickListener { goToLibrary() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goToLibrary()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}