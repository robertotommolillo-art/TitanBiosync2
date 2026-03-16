package com.titanbiosync.ui.device

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentDeviceListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeviceListFragment : Fragment() {

    private var _binding: FragmentDeviceListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeviceListViewModel by viewModels()
    private lateinit var deviceAdapter: DeviceAdapter

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.onPermissionsGranted()
            startScan()
        } else {
            viewModel.onPermissionsDenied()
            val canAskAgain = viewModel.requiredPermissions.any { perm ->
                shouldShowRequestPermissionRationale(perm)
            }
            if (!canAskAgain) {
                // "Don't ask again" selected — direct user to Settings
                Snackbar.make(
                    binding.root,
                    getString(R.string.device_permissions_settings_rationale),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(getString(R.string.device_open_settings)) { openAppSettings() }
                    .show()
            } else {
                showError(getString(R.string.device_permissions_required))
            }
        }
    }

    // Bluetooth enable launcher
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            startScan()
        } else {
            showError(getString(R.string.device_bluetooth_must_be_enabled))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { address ->
            viewModel.connectToDevice(address)
        }

        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = deviceAdapter
        }
    }

    private fun setupClickListeners() {
        binding.scanButton.setOnClickListener {
            if (viewModel.uiState.value.isScanning) {
                viewModel.stopScan()
            } else {
                checkPermissionsAndScan()
            }
        }

        binding.disconnectButton.setOnClickListener {
            viewModel.disconnect()
        }

        binding.grantPermissionsButton.setOnClickListener {
            checkPermissionsAndScan()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update scan button
                if (state.isScanning) {
                    binding.scanButton.text = "⏹️ Stop Scan"
                    binding.scanProgressBar.isVisible = true
                } else {
                    binding.scanButton.text = "🔍 Start Scan"
                    binding.scanProgressBar.isVisible = false
                }

                // Update connected device card
                if (state.connectedDevice != null) {
                    binding.connectedDeviceCard.isVisible = true
                    binding.connectedDeviceName.text = state.connectedDevice.name
                } else {
                    binding.connectedDeviceCard.isVisible = false
                }

                // Update device list
                deviceAdapter.submitList(state.devices)

                // Show/hide empty state
                binding.emptyStateLayout.isVisible = state.devices.isEmpty() && !state.isScanning
                binding.devicesRecyclerView.isVisible = state.devices.isNotEmpty()

                // Show info/error messages and grant button
                val permissionsMissing = !state.permissionsGranted
                binding.grantPermissionsButton.isVisible = permissionsMissing

                when {
                    !state.bluetoothEnabled -> {
                        binding.infoText.isVisible = true
                        binding.infoText.text = getString(R.string.device_bluetooth_not_enabled)
                    }
                    permissionsMissing -> {
                        binding.infoText.isVisible = true
                        binding.infoText.text = getString(R.string.device_permissions_info)
                    }
                    state.error != null -> {
                        binding.infoText.isVisible = true
                        binding.infoText.text = "⚠️ ${state.error}"
                    }
                    else -> {
                        binding.infoText.isVisible = false
                    }
                }

                // Handle transient errors via Snackbar
                state.error?.let { error ->
                    showError(error)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun checkPermissionsAndScan() {
        val state = viewModel.uiState.value

        // Check Bluetooth enabled
        if (!state.bluetoothEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }

        // Check permissions
        if (!state.permissionsGranted) {
            requestPermissionLauncher.launch(viewModel.requiredPermissions)
            return
        }

        startScan()
    }

    private fun startScan() {
        viewModel.startScan()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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
        viewModel.stopScan()
        _binding = null
    }
}