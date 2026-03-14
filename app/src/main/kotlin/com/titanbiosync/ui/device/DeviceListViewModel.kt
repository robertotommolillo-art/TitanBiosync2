package com.titanbiosync.ui.device

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.ble.BleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListUiState(
    val devices: List<BleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectedDevice: BleDevice? = null,
    val bluetoothEnabled: Boolean = true,
    val permissionsGranted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

    // Permessi richiesti in base alla versione Android
    val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

    init {
        observeBleState()
        checkInitialState()
    }

    private fun observeBleState() {
        viewModelScope.launch {
            bleManager.scannedDevices.collect { devices ->
                _uiState.value = _uiState.value.copy(devices = devices)
            }
        }

        viewModelScope.launch {
            bleManager.isScanning.collect { isScanning ->
                _uiState.value = _uiState.value.copy(isScanning = isScanning)
            }
        }

        viewModelScope.launch {
            bleManager.connectedDevice.collect { device ->
                _uiState.value = _uiState.value.copy(connectedDevice = device)
            }
        }
    }

    private fun checkInitialState() {
        val bluetoothEnabled = bleManager.isBluetoothEnabled()
        val permissionsGranted = bleManager.hasRequiredPermissions()

        _uiState.value = _uiState.value.copy(
            bluetoothEnabled = bluetoothEnabled,
            permissionsGranted = permissionsGranted
        )
    }

    fun startScan() {
        if (!_uiState.value.bluetoothEnabled) {
            _uiState.value = _uiState.value.copy(error = "Bluetooth is not enabled")
            return
        }

        if (!_uiState.value.permissionsGranted) {
            _uiState.value = _uiState.value.copy(error = "Permissions not granted")
            return
        }

        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectToDevice(address: String) {
        bleManager.connectToDevice(address)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(
            permissionsGranted = true,
            error = null
        )
    }

    fun onPermissionsDenied() {
        _uiState.value = _uiState.value.copy(
            permissionsGranted = false,
            error = "Bluetooth permissions are required to scan for devices"
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}