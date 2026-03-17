package com.titanbiosync.auth.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.titanbiosync.R
import com.titanbiosync.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // If a user is already signed in, go straight to the Dashboard
        if (viewModel.isAlreadySignedIn()) {
            findNavController().navigate(R.id.action_login_to_dashboard)
            return
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text?.toString().orEmpty()
            val password = binding.passwordEditText.text?.toString().orEmpty()
            viewModel.signIn(email, password)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.loginButton.isEnabled = !state.isLoading
                    binding.errorText.isVisible = state.error != null
                    binding.errorText.text = state.error

                    if (state.loggedInUser != null) {
                        findNavController().navigate(
                            R.id.action_login_to_dashboard
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
