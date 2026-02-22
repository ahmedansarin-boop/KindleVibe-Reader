package com.kindlevibe.reader.viewmodel

import androidx.lifecycle.ViewModel
import com.kindlevibe.reader.data.repo.BookmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val bookmarkRepository: BookmarkRepository = BookmarkRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Settings ViewModel implementation to be added
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
