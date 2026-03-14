package com.titanbiosync.gym.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.data.local.entities.gym.ExerciseMediaEntity
import com.titanbiosync.databinding.FragmentGymExercisesBinding
import com.titanbiosync.gym.ui.filters.GymFilterLabels
import com.titanbiosync.gym.ui.filters.MusclePickerDialog
import com.titanbiosync.gym.ui.filters.SingleChoiceDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GymExercisesFragment : Fragment() {

    private var _binding: FragmentGymExercisesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GymExercisesViewModel by viewModels()

    private val adapter = GymExerciseAdapter { exercise ->
        viewModel.selectExercise(exercise.id)
    }

    private var currentVideos: List<ExerciseMediaEntity> = emptyList()

    private var equipmentOptions: List<String> = emptyList()
    private var levelOptions: List<String> = emptyList()
    private var mechanicsOptions: List<String> = emptyList()
    private var categoryOptions: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGymExercisesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.setQuery(text?.toString().orEmpty())
        }

        // --- Video button ---
        binding.btnVideo.isVisible = false
        binding.btnVideo.setOnClickListener {
            val videos = currentVideos
            if (videos.isEmpty()) return@setOnClickListener

            ExerciseVideoDialogFragment
                .newInstance(videos = videos)
                .show(parentFragmentManager, "exercise_video")
        }

        // --- Filters: mode ANY/ALL ---
        binding.rgMuscleMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.rbAll.id -> GymExercisesViewModel.MuscleMatchMode.ALL
                else -> GymExercisesViewModel.MuscleMatchMode.ANY
            }
            viewModel.setFilters { it.copy(muscleMatchMode = mode) }
        }

        // --- Filters: clear ---
        binding.btnClearFilters.setOnClickListener {
            viewModel.clearFilters()
            binding.rbAny.isChecked = true
        }

        // --- Filters: pick muscles ---
        binding.btnMuscles.setOnClickListener {
            val muscles = viewModel.muscles.value.orEmpty()
            val current = viewModel.filters.value

            MusclePickerDialog.show(
                context = requireContext(),
                muscles = muscles,
                selectedIds = current?.muscleIds?.toSet().orEmpty()
            ) { ids ->
                viewModel.setFilters { it.copy(muscleIds = ids) }
            }
        }

        // --- Filters: equipment (labeled) ---
        binding.btnEquipment.setOnClickListener {
            val values = equipmentOptions
            if (values.isEmpty()) return@setOnClickListener

            val labeled = values.map { GymFilterLabels.equipmentLabel(it) to it }
            val current = viewModel.filters.value?.equipment
            SingleChoiceDialog.showLabeled(
                context = requireContext(),
                title = "Equipment",
                options = labeled,
                currentValue = current
            ) { selectedValue ->
                viewModel.setFilters { it.copy(equipment = selectedValue) }
            }
        }

        // --- Filters: level (labeled) ---
        binding.btnLevel.setOnClickListener {
            val values = levelOptions
            if (values.isEmpty()) return@setOnClickListener

            val labeled = values.map { GymFilterLabels.levelLabel(it) to it }
            val current = viewModel.filters.value?.level
            SingleChoiceDialog.showLabeled(
                context = requireContext(),
                title = "Livello",
                options = labeled,
                currentValue = current
            ) { selectedValue ->
                viewModel.setFilters { it.copy(level = selectedValue) }
            }
        }

        // --- Filters: mechanics (labeled) ---
        binding.btnMechanics.setOnClickListener {
            val values = mechanicsOptions
            if (values.isEmpty()) return@setOnClickListener

            val labeled = values.map { GymFilterLabels.mechanicsLabel(it) to it }
            val current = viewModel.filters.value?.mechanics
            SingleChoiceDialog.showLabeled(
                context = requireContext(),
                title = "Meccanica",
                options = labeled,
                currentValue = current
            ) { selectedValue ->
                viewModel.setFilters { it.copy(mechanics = selectedValue) }
            }
        }

        // --- Filters: category (labeled) ---
        binding.btnCategory.setOnClickListener {
            val values = categoryOptions
            if (values.isEmpty()) return@setOnClickListener

            val labeled = values.map { GymFilterLabels.categoryLabel(it) to it }
            val current = viewModel.filters.value?.category
            SingleChoiceDialog.showLabeled(
                context = requireContext(),
                title = "Categoria",
                options = labeled,
                currentValue = current
            ) { selectedValue ->
                viewModel.setFilters { it.copy(category = selectedValue) }
            }
        }

        // --- Observe exercises ---
        viewModel.exercises.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // --- Observe current videos ---
        viewModel.currentVideos.observe(viewLifecycleOwner) { videos ->
            currentVideos = videos
            binding.btnVideo.isVisible = videos.isNotEmpty()
        }

        // --- Observe filter options from DB ---
        viewModel.equipmentOptions.observe(viewLifecycleOwner) { equipmentOptions = it }
        viewModel.levelOptions.observe(viewLifecycleOwner) { levelOptions = it }
        viewModel.mechanicsOptions.observe(viewLifecycleOwner) { mechanicsOptions = it }
        viewModel.categoryOptions.observe(viewLifecycleOwner) { categoryOptions = it }

        // --- Filters summary + aggiorna testo bottoni (label IT) ---
        viewModel.filters.observe(viewLifecycleOwner) { f ->
            binding.tvFiltersSummary.text = buildFiltersSummary(f)

            binding.btnEquipment.text =
                if (f.equipment == null) "Equipment" else "Eq: ${GymFilterLabels.equipmentLabel(f.equipment)}"
            binding.btnLevel.text =
                if (f.level == null) "Level" else "Lv: ${GymFilterLabels.levelLabel(f.level)}"
            binding.btnMechanics.text =
                if (f.mechanics == null) "Mechanics" else "Me: ${GymFilterLabels.mechanicsLabel(f.mechanics)}"
            binding.btnCategory.text =
                if (f.category == null) "Category" else "Cat: ${GymFilterLabels.categoryLabel(f.category)}"
            binding.btnMuscles.text =
                if (f.muscleIds.isEmpty()) "Muscoli" else "Muscoli (${f.muscleIds.size})"
        }
    }

    private fun buildFiltersSummary(f: GymExercisesViewModel.Filters): String {
        val parts = mutableListOf<String>()

        if (f.muscleIds.isNotEmpty()) parts += "muscoli=${f.muscleIds.size} (${f.muscleMatchMode.name})"

        if (f.category != null) parts += "cat=${GymFilterLabels.categoryLabel(f.category)}"
        if (f.equipment != null) parts += "equip=${GymFilterLabels.equipmentLabel(f.equipment)}"
        if (f.mechanics != null) parts += "mech=${GymFilterLabels.mechanicsLabel(f.mechanics)}"
        if (f.level != null) parts += "lvl=${GymFilterLabels.levelLabel(f.level)}"

        // role (se lo userai più avanti)
        if (f.role != null) parts += "ruolo=${f.role}"

        return if (parts.isEmpty()) "Filtri: nessuno" else "Filtri: " + parts.joinToString(" • ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}