package com.titanbiosync.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentPersonalizationBinding
import com.titanbiosync.ui.profile.UserPreferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonalizationFragment : Fragment() {

    private var _binding: FragmentPersonalizationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PersonalizationViewModel by viewModels()
    private var isApplyingThresholdSelection = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalizationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.currentTheme.observe(viewLifecycleOwner) { themeId ->
            updateRadioButtons(themeId)
        }

        viewModel.currentSimilarityThreshold.observe(viewLifecycleOwner) { threshold ->
            updateSimilarityThresholdSelection(threshold)
        }

        binding.similarityThresholdGroup.setOnCheckedChangeListener { _, checkedId ->
            if (isApplyingThresholdSelection) return@setOnCheckedChangeListener
            when (checkedId) {
                R.id.threshold40 -> viewModel.setAiCoachSimilarityThreshold(40)
                R.id.threshold45 -> viewModel.setAiCoachSimilarityThreshold(45)
                R.id.threshold50 -> viewModel.setAiCoachSimilarityThreshold(50)
            }
        }

        binding.themeDefaultCard.setOnClickListener {
            viewModel.setTheme(UserPreferences.THEME_DEFAULT)
            requireActivity().recreate()
        }
        binding.themeCyberCard.setOnClickListener {
            viewModel.setTheme(UserPreferences.THEME_CYBER)
            requireActivity().recreate()
        }
        binding.themeCrimsonCard.setOnClickListener {
            viewModel.setTheme(UserPreferences.THEME_CRIMSON)
            requireActivity().recreate()
        }
        binding.themeForestCard.setOnClickListener {
            viewModel.setTheme(UserPreferences.THEME_FOREST)
            requireActivity().recreate()
        }
    }

    private fun updateRadioButtons(themeId: String) {
        binding.radioDefault.isChecked = themeId == UserPreferences.THEME_DEFAULT
        binding.radioCyber.isChecked = themeId == UserPreferences.THEME_CYBER
        binding.radioCrimson.isChecked = themeId == UserPreferences.THEME_CRIMSON
        binding.radioForest.isChecked = themeId == UserPreferences.THEME_FOREST
    }

    private fun updateSimilarityThresholdSelection(threshold: Int) {
        isApplyingThresholdSelection = true
        when (threshold.coerceIn(40, 50)) {
            in 40..44 -> binding.threshold40.isChecked = true
            in 45..49 -> binding.threshold45.isChecked = true
            else -> binding.threshold50.isChecked = true
        }
        isApplyingThresholdSelection = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
