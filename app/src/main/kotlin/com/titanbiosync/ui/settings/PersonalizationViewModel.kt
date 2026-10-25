package com.titanbiosync.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.domain.repository.UserRepository
import com.titanbiosync.ui.profile.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalizationViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentTheme = MutableLiveData<String>()
    val currentTheme: LiveData<String> = _currentTheme

    private val _currentSimilarityThreshold = MutableLiveData<Int>()
    val currentSimilarityThreshold: LiveData<Int> = _currentSimilarityThreshold

    init {
        viewModelScope.launch {
            userRepository.observeCurrentProfile().collectLatest { user ->
                val prefs = UserPreferences.fromJson(user?.preferencesJson)
                _currentTheme.postValue(prefs.themeId)
                _currentSimilarityThreshold.postValue(prefs.aiCoachSimilarityThreshold)
            }
        }
    }

    fun setTheme(themeId: String) {
        viewModelScope.launch {
            val user = userRepository.getCurrentProfile() ?: return@launch
            val prefs = UserPreferences.fromJson(user.preferencesJson)
            val newPrefs = prefs.copy(themeId = themeId)
            
            userRepository.upsert(user.copy(
                preferencesJson = UserPreferences.toJson(newPrefs)
            ))
        }
    }

    fun setAiCoachSimilarityThreshold(threshold: Int) {
        val safeThreshold = threshold.coerceIn(40, 50)
        viewModelScope.launch {
            val user = userRepository.getCurrentProfile() ?: return@launch
            val prefs = UserPreferences.fromJson(user.preferencesJson)
            val newPrefs = prefs.copy(aiCoachSimilarityThreshold = safeThreshold)
            userRepository.upsert(
                user.copy(
                    preferencesJson = UserPreferences.toJson(newPrefs)
                )
            )
        }
    }
}
