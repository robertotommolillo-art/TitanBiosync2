package com.titanbiosync.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentProfileSetupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileSetupFragment : Fragment() {

    private var _binding: FragmentProfileSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileSetupViewModel by viewModels()

    private var selectedAvatarUri: Uri? = null

    /** Goal key values matching [UserPreferences] constants, parallel to goal display labels. */
    private val goalKeys = listOf(
        UserPreferences.GOAL_HYPERTROPHY,
        UserPreferences.GOAL_STRENGTH,
        UserPreferences.GOAL_FAT_LOSS
    )

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Take persistable permission so the URI survives app restarts
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers don't support persistable permissions; URI may still work in session
            }
            selectedAvatarUri = uri
            binding.avatarImage.setImageURI(uri)
            binding.avatarImage.isVisible = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate sex dropdown
        val sexOptions = listOf(
            getString(R.string.profile_setup_sex_male),
            getString(R.string.profile_setup_sex_female),
            getString(R.string.profile_setup_sex_other)
        )
        binding.sexAutoComplete.setAdapter(
            android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                sexOptions
            )
        )

        // Populate goal dropdown (optional)
        val goalLabels = listOf(
            getString(R.string.profile_setup_goal_hypertrophy),
            getString(R.string.profile_setup_goal_strength),
            getString(R.string.profile_setup_goal_fat_loss)
        )
        binding.goalAutoComplete.setAdapter(
            android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                goalLabels
            )
        )

        // Default weight unit selection: kg
        binding.weightUnitToggle.check(R.id.btnWeightKg)

        // Default height unit selection: cm
        binding.heightUnitToggle.check(R.id.btnHeightCm)

        // Update height hint when unit changes
        binding.heightUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                binding.heightInputLayout.hint = getString(
                    if (checkedId == R.id.btnHeightFt) R.string.profile_setup_height_ft
                    else R.string.profile_setup_height_cm
                )
            }
        }

        binding.pickAvatarButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            val firstName = binding.firstNameEditText.text?.toString().orEmpty()
            val lastName = binding.lastNameEditText.text?.toString().orEmpty()
            val ageStr = binding.ageEditText.text?.toString().orEmpty()
            val heightStr = binding.heightEditText.text?.toString().orEmpty()
            val sex = binding.sexAutoComplete.text?.toString().takeIf { it?.isNotBlank() == true }

            val age = ageStr.toIntOrNull()
            val height = heightStr.toFloatOrNull()

            val weightUnit = if (binding.weightUnitToggle.checkedButtonId == R.id.btnWeightLb)
                UserPreferences.WEIGHT_LB else UserPreferences.WEIGHT_KG
            val heightUnit = if (binding.heightUnitToggle.checkedButtonId == R.id.btnHeightFt)
                UserPreferences.HEIGHT_FT else UserPreferences.HEIGHT_CM

            val goalLabel = binding.goalAutoComplete.text?.toString().orEmpty()
            val goalKey = if (goalLabel.isBlank()) null else goalKeys.getOrNull(goalLabels.indexOf(goalLabel))

            viewModel.saveProfile(
                firstName = firstName,
                lastName = lastName,
                age = age,
                height = height,
                sex = sex,
                avatarUri = selectedAvatarUri?.toString(),
                preferences = UserPreferences(
                    weightUnit = weightUnit,
                    heightUnit = heightUnit,
                    goal = goalKey
                )
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.saveButton.isEnabled = !state.isLoading

                    binding.errorText.isVisible = state.error != null
                    binding.errorText.text = state.error

                    if (state.profileExists && !state.savedSuccessfully) {
                        // Profile already set up — skip straight to Dashboard
                        navigateToDashboard()
                        return@collect
                    }

                    if (state.savedSuccessfully) {
                        navigateToDashboard()
                    }
                }
            }
        }
    }

    private fun navigateToDashboard() {
        findNavController().navigate(R.id.action_profileSetup_to_dashboard)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
