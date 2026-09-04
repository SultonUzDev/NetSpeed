package com.sultonuzdev.netspeed.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
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

    val isDynamicColor: StateFlow<Boolean> = preferencesManager.dynamicColor
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

    /**
     * Whether opening the app should start monitoring.
     *
     * True on a fresh install so the app works without being switched on, and thereafter only if
     * monitoring was left running -- otherwise turning it off in Settings would silently undo
     * itself the next time the app was opened.
     */
    suspend fun shouldAutoStartMonitoring(): Boolean {
        val firstLaunch = preferencesManager.isFirstLaunch.first()
        if (firstLaunch) {
            preferencesManager.updateFirstLaunch(false)
            return true
        }
        return preferencesManager.monitoringEnabled.first()
    }

    fun completeFirstLaunch() {
        viewModelScope.launch {
            preferencesManager.updateFirstLaunch(false)
        }
    }
}