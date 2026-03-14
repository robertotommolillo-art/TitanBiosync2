package com.titanbiosync.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.titanbiosync.databinding.FragmentSettingsBinding
import com.titanbiosync.gym.domain.WeightUnit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Evita loop quando aggiorniamo programmaticamente lo switch
        binding.weightUnitSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setUseLb(isChecked)
        }

        viewModel.weightUnit.observe(viewLifecycleOwner) { unit ->
            val useLb = (unit == WeightUnit.LB)

            if (binding.weightUnitSwitch.isChecked != useLb) {
                // rimuovi listener temporaneamente per non triggerare setUseLb
                binding.weightUnitSwitch.setOnCheckedChangeListener(null)
                binding.weightUnitSwitch.isChecked = useLb
                binding.weightUnitSwitch.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.setUseLb(isChecked)
                }
            }

            binding.currentValue.text = "Attuale: ${unit.key}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}