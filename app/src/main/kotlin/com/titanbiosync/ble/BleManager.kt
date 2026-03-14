package com.titanbiosync.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.titanbiosync.ui.device.BleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    private val context: Context
) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentDevice: BluetoothDevice? = null

    // State flows
    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleDevice?>(null)
    val connectedDevice: StateFlow<BleDevice?> = _connectedDevice.asStateFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    // UUIDs standard per Heart Rate Service
    companion object {
        private const val TAG = "BleManager"
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    // Check if Bluetooth is enabled
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    // Check permissions
    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            // Android < 12
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    // BLE Scan Callback
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name
            val address = device.address
            val rssi = result.rssi

            // Filtra solo dispositivi con nome (evita spam)
            if (name != null) {
                val bleDevice = BleDevice(
                    address = address,
                    name = name,
                    rssi = rssi
                )

                // Aggiungi alla lista se non esiste già
                val currentList = _scannedDevices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.address == address }

                if (existingIndex >= 0) {
                    currentList[existingIndex] = bleDevice
                } else {
                    currentList.add(bleDevice)
                }

                _scannedDevices.value = currentList
                Log.d(TAG, "Device found: $name ($address) RSSI: $rssi")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            _isScanning.value = false
        }
    }

    // Start BLE scan
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Missing required permissions")
            return
        }

        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE scanner not available")
            return
        }

        // Clear previous results
        _scannedDevices.value = emptyList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        _isScanning.value = true
        Log.d(TAG, "BLE scan started")
    }

    // Stop BLE scan
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasRequiredPermissions()) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.stopScan(scanCallback)
        _isScanning.value = false
        Log.d(TAG, "BLE scan stopped")
    }

    // GATT Callback
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectedDevice.value = BleDevice(
                        address = gatt.device.address,
                        name = gatt.device.name,
                        rssi = 0,
                        isConnected = true
                    )
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectedDevice.value = null
                    _heartRate.value = null
                    gatt.close()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                // Trova Heart Rate Service
                val hrService = gatt.getService(HEART_RATE_SERVICE_UUID)
                if (hrService != null) {
                    val hrCharacteristic = hrService.getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)
                    if (hrCharacteristic != null) {
                        // Enable notifications
                        gatt.setCharacteristicNotification(hrCharacteristic, true)

                        // Write descriptor to enable notifications
                        val descriptor = hrCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                            Log.d(TAG, "Heart Rate notifications enabled")
                        }
                    }
                } else {
                    Log.w(TAG, "Heart Rate Service not found on device")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_CHAR_UUID) {
                val heartRate = parseHeartRate(characteristic)
                _heartRate.value = heartRate
                Log.d(TAG, "Heart Rate: $heartRate bpm")
            }
        }
    }

    // Parse Heart Rate measurement (standard BLE Heart Rate Profile)
    private fun parseHeartRate(characteristic: BluetoothGattCharacteristic): Int {
        val flag = characteristic.properties
        val format = if (flag and 0x01 != 0) {
            BluetoothGattCharacteristic.FORMAT_UINT16
        } else {
            BluetoothGattCharacteristic.FORMAT_UINT8
        }
        return characteristic.getIntValue(format, 1) ?: 0
    }

    // Connect to device
    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "Missing required permissions")
            return
        }

        stopScan()

        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            Log.e(TAG, "Device not found: $address")
            return
        }

        currentDevice = device
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        Log.d(TAG, "Connecting to ${device.name} ($address)")
    }

    // Disconnect from device
    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (!hasRequiredPermissions()) return

        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        currentDevice = null
        _connectedDevice.value = null
        _heartRate.value = null
        Log.d(TAG, "Disconnected")
    }

    // Cleanup
    fun cleanup() {
        stopScan()
        disconnect()
    }
}