package com.titanbiosync.gym.ui.progress

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentProgressListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProgressListFragment : Fragment(R.layout.fragment_progress_list) {

    private var _binding: FragmentProgressListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProgressListViewModel by viewModels()

    private val adapter = ProgressExerciseAdapter { exerciseUi ->
        val action = ProgressListFragmentDirections
            .actionProgressListToExerciseProgressDetail(
                exerciseId = exerciseUi.exerciseId,
                exerciseName = exerciseUi.exerciseName
            )
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentProgressListBinding.bind(view)

        binding.exerciseList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ProgressListFragment.adapter
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        viewModel.exercises.observe(viewLifecycleOwner) { exercises ->
            adapter.submitList(exercises)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
