package com.titanbiosync.gym.ui.online

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbiosync.gym.online.data.OnlineExerciseDataSource
import com.titanbiosync.gym.online.model.OnlineExerciseCandidate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineExerciseSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSource: OnlineExerciseDataSource
) : ViewModel() {

    private val query: String = savedStateHandle["query"] ?: ""

    private val _items = MutableStateFlow<List<OnlineExerciseCandidate>>(emptyList())
    val items = _items.asStateFlow()

    fun getQuery(): String = query

    fun load() {
        viewModelScope.launch {
            _items.value = dataSource.search(query)
        }
    }
}