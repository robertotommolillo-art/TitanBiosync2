package com.titanbiosync.ui.device

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.databinding.DeviceItemBinding

class DeviceAdapter(
    private val onConnectClick: (String) -> Unit
) : ListAdapter<BleDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = DeviceItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding, onConnectClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: DeviceItemBinding,
        private val onConnectClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BleDevice) {
            // Updated property names to match BleDevice data class
            binding.deviceName.text = device.name 
            binding.deviceAddress.text = device.address
            binding.deviceSignal.text = "RSSI: ${device.rssi} dBm"

            if (device.isConnected) {
                binding.connectButton.text = "Connected"
                binding.connectButton.isEnabled = false
            } else {
                binding.connectButton.text = "Connect"
                binding.connectButton.isEnabled = true
                binding.connectButton.setOnClickListener {
                    onConnectClick(device.address)
                }
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<BleDevice>() {
        override fun areItemsTheSame(oldItem: BleDevice, newItem: BleDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BleDevice, newItem: BleDevice): Boolean {
            return oldItem == newItem
        }
    }
}