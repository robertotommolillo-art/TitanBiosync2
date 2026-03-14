package com.titanbiosync.gym.ui.picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentExercisePickerBinding
import com.titanbiosync.gym.ui.template.ConfigureTemplateExerciseBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExercisePickerFragment : Fragment() {

    private var _binding: FragmentExercisePickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExercisePickerViewModel by viewModels()

    private var currentQuery: String = ""

    private val adapter = ExercisePickerAdapter { exercise ->
        ConfigureTemplateExerciseBottomSheet
            .newInstance(exerciseId = exercise.id, exerciseNameIt = exercise.nameIt)
            .show(childFragmentManager, "ConfigureTemplateExerciseBottomSheet")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExercisePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.searchOnlineButton.setOnClickListener {
            val q = currentQuery.trim()
            if (q.isBlank()) return@setOnClickListener

            val args = Bundle().apply { putString("query", q) }
            findNavController().navigate(R.id.onlineExerciseSearchFragment, args)
        }

        binding.search.doOnTextChanged { text, _, _, _ ->
            currentQuery = text?.toString().orEmpty()
            viewModel.setQuery(currentQuery)
        }

        viewModel.exercises.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)

            val showOnline = items.isNullOrEmpty() && currentQuery.trim().isNotBlank()
            binding.searchOnlineButton.visibility = if (showOnline) View.VISIBLE else View.GONE
            binding.searchOnlineButton.text = "Cerca online \"$currentQuery\""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}