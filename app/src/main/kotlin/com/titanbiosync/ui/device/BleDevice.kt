package com.titanbiosync.ui.device

data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val isConnected: Boolean = false
)