package com.uzdev.netspeed.presentation.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uzdev.netspeed.data.preference.NetDataStoreManager
import com.uzdev.netspeed.data.repo.NetworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val netDataStoreManager: NetDataStoreManager
) : ViewModel() {

    init {
        getPingDuration()
    }

    private val _pingDuration = mutableStateOf("")
    val pingDuration: State<String> = _pingDuration
    private fun getPingDuration() {
        viewModelScope.launch {
            netDataStoreManager.getPingDuration().onEach {
                _pingDuration.value = it
            }
        }
    }

    fun savePingDuration(duration: String) {
        viewModelScope.launch(Dispatchers.IO) {
            netDataStoreManager.savePingDuration(duration = duration)
        }
    }

    fun clearNetworkHistory() {
        viewModelScope.launch {
            networkRepository.clearHistory()
        }
    }

}