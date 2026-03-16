package com.titanbiosync.ui.session

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentStartSessionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StartSessionFragment : Fragment() {

    private var _binding: FragmentStartSessionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StartSessionViewModel by viewModels()

    /** Session type queued while waiting for the permission result. */
    private var pendingSessionType: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val type = pendingSessionType ?: return@registerForActivityResult
        pendingSessionType = null

        if (!fineGranted) {
            val canAskAgain = locationPermissions().any { perm ->
                shouldShowRequestPermissionRationale(perm)
            }
            if (!canAskAgain) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.start_session_location_settings_rationale),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(getString(R.string.device_open_settings)) { openAppSettings() }
                    .show()
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.start_session_location_denied),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
        // Start session regardless: GPS is optional.
        viewModel.startSession(type)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupStateSubscription()
    }

    private fun setupClickListeners() {
        binding.runningCard.setOnClickListener {
            launchSession("running")
        }

        binding.cyclingCard.setOnClickListener {
            launchSession("cycling")
        }
    }

    /**
     * Starts a session after optionally requesting location permission.
     * If the GPS toggle is ON and permission has not been granted, the permission
     * prompt is shown first. The session is started regardless of the result (GPS is
     * optional — the tracker simply won't provide coordinates when denied).
     */
    private fun launchSession(type: String) {
        val wantsGps = binding.locationSwitch.isChecked
        if (wantsGps && !hasLocationPermission()) {
            pendingSessionType = type
            locationPermissionLauncher.launch(locationPermissions())
        } else {
            viewModel.startSession(type)
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun setupStateSubscription() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.isVisible = state.isLoading
                binding.statusText.text = if (state.isLoading) {
                    getString(R.string.start_session_starting)
                } else {
                    ""
                }

                state.startedSession?.let { session ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.start_session_started, session.type),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    val action = StartSessionFragmentDirections
                        .actionStartSessionToSessionDetail(session.id)
                    findNavController().navigate(action)
                    viewModel.resetState()
                }

                state.error?.let { error ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.start_session_error, error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private fun locationPermissions(): Array<String> = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}