package com.sultonuzdev.netspeed.presentation.screens.speed


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.domain.repository.NetworkRepository
import com.sultonuzdev.netspeed.domain.usecases.GetNetworkSpeedUseCase
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.SpeedFormatter
import com.sultonuzdev.netspeed.utils.SpeedUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpeedViewModel(
    private val getNetworkSpeedUseCase: GetNetworkSpeedUseCase,
    private val networkRepository: NetworkRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState.asStateFlow()

    /** Cached so the per-sample formatting below stays synchronous. */
    private var speedUnit = SpeedUnit.AUTO

    init {
        observeSpeedUnit()
        startMonitoring()
        observeNetworkSpeed()
        observeNetworkInfo()
    }

    /** The unit preference applies app-wide, so the screen and the notification agree. */
    private fun observeSpeedUnit() {
        viewModelScope.launch {
            preferencesManager.speedUnit.collect { unit -> speedUnit = unit }
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            networkRepository.startMonitoring()
        }
    }

    private fun observeNetworkSpeed() {
        viewModelScope.launch {
            getNetworkSpeedUseCase().collect { speed ->
                val download = SpeedFormatter.format(speed.downloadSpeed, speedUnit)
                val upload = SpeedFormatter.format(speed.uploadSpeed, speedUnit)
                _uiState.update { currentState ->
                    currentState.copy(
                        downloadSpeed = download.value,
                        downloadUnit = download.unit,
                        uploadSpeed = upload.value,
                        uploadUnit = upload.unit,
                        recentDownload = (currentState.recentDownload +
                                speed.downloadSpeed.toFloat()).takeLast(SPARKLINE_SAMPLES)
                    )
                }
            }
        }
    }

    private fun observeNetworkInfo() {
        viewModelScope.launch {
            networkRepository.getNetworkInfo().collect { networkInfo ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isConnected = networkInfo.isConnected,
                        networkType = networkInfo.networkType.name,
                        networkName = networkInfo.networkName,
                        signalStrength = networkInfo.signalStrength
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            networkRepository.stopMonitoring()
        }
    }

    private companion object {
        /** About a minute of history at the default one-second cadence. */
        const val SPARKLINE_SAMPLES = 60
    }
}
