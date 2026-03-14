package com.titanbiosync.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        binding.quickStatSessionsCard.setOnClickListener { showComingSoon(it) }
        binding.quickStatMinutesCard.setOnClickListener { showComingSoon(it) }
        binding.quickStatDevicesCard.setOnClickListener { showComingSoon(it) }
        binding.quickStatStreakCard.setOnClickListener { showComingSoon(it) }

        setupStateSubscription()
    }

    private fun showComingSoon(anchor: View) {
        Snackbar.make(anchor, R.string.common_coming_soon, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupStateSubscription() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.viewSessionButton.isVisible = false

                if (state.isLoading) {
                    binding.userNameText.setText(R.string.common_loading)
                }

                state.error?.let { error ->
                    binding.userNameText.setText(R.string.dashboard_error_title)
                    binding.userEmailText.text = error
                    Toast.makeText(
                        context,
                        getString(R.string.dashboard_error_message, error),
                        Toast.LENGTH_LONG
                    ).show()
                }

                state.user?.let { user ->
                    binding.userNameText.text = user.displayName
                    binding.userEmailText.text = user.email
                }

                if (state.activeSession != null) {
                    // CHIP: sessione attiva
                    binding.activeSessionStatusChip.setText(R.string.dashboard_session_status_active)

                    binding.activeSessionType.text = getString(
                        R.string.dashboard_active_session_type,
                        state.activeSession.type
                    )

                    binding.viewSessionButton.isVisible = true
                    binding.viewSessionButton.setOnClickListener {
                        val action = DashboardFragmentDirections
                            .actionDashboardToSessionDetail(state.activeSession.id)
                        findNavController().navigate(action)
                    }
                } else {
                    // CHIP: nessuna sessione
                    binding.activeSessionStatusChip.setText(R.string.dashboard_session_status_inactive)

                    binding.activeSessionType.setText(R.string.dashboard_no_active_session)
                }

                // Quick stats
                binding.quickStatSessionsValue.text = state.todaySessionsCount.toString()
                binding.quickStatMinutesValue.text = state.todayActiveMinutes.toString()
                binding.quickStatDevicesValue.text = state.connectedDevicesCount.toString()
                binding.quickStatStreakValue.text = state.streakDays?.toString()
                    ?: getString(R.string.common_dash)

                // Summary cards
                binding.todayStatsText.text = getString(
                    R.string.dashboard_today_stats,
                    state.todaySessionsCount,
                    state.todayActiveMinutes
                )

                binding.devicesCountText.text = getString(
                    R.string.dashboard_devices_connected,
                    state.connectedDevicesCount
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}