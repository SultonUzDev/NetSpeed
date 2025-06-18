package com.sultonuzdev.netspeed.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val isDarkTheme: StateFlow<Boolean> = preferencesManager.darkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val isFirstLaunch: StateFlow<Boolean> = preferencesManager.isFirstLaunch
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun completeFirstLaunch() {
        viewModelScope.launch {
            preferencesManager.updateFirstLaunch(false)
        }
    }
}