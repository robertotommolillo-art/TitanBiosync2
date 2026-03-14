package com.titanbiosync.gym.ui.online

import android.os.Bundle
import android.os.Bundle.EMPTY
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentOnlineExerciseSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnlineExerciseSearchFragment : Fragment() {

    private var _binding: FragmentOnlineExerciseSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnlineExerciseSearchViewModel by viewModels()

    private val adapter = OnlineExerciseCandidateAdapter { candidate ->
        val args = Bundle().apply {
            putString("candidateId", candidate.candidateId)
        }
        findNavController().navigate(R.id.exerciseImportPreviewFragment, args)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnlineExerciseSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.subtitle.text = "Query: ${viewModel.getQuery()}"

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.load()
            viewModel.items.collect { items ->
                adapter.submitList(items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}