package com.titanbiosync.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.repository.AuthRepository
import com.titanbiosync.gym.domain.WeightUnit
import com.titanbiosync.gym.domain.WeightUnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val weightUnitRepository: WeightUnitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val weightUnit = weightUnitRepository.observe().asLiveData()

    fun setUseLb(useLb: Boolean) {
        viewModelScope.launch {
            weightUnitRepository.set(if (useLb) WeightUnit.LB else WeightUnit.KG)
        }
    }

    fun logout() {
        authRepository.signOut()
    }
}