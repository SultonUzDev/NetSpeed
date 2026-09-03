package com.sultonuzdev.netspeed.presentation.screens.speed


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.domain.repository.NetworkRepository
import com.sultonuzdev.netspeed.domain.usecases.GetNetworkSpeedUseCase
import com.sultonuzdev.netspeed.utils.FormattedSpeed
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.SpeedDisplayMode
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
    private var displayMode = SpeedDisplayMode.DOWNLOAD

    init {
        observeSpeedUnit()
        observeDisplayMode()
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

    /** The circle answers to the same setting as the notification and the overlay. */
    private fun observeDisplayMode() {
        viewModelScope.launch {
            preferencesManager.speedDisplayMode.collect { mode -> displayMode = mode }
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
                val combinedBps = speed.downloadSpeed + speed.uploadSpeed
                val combined = SpeedFormatter.format(combinedBps, speedUnit)

                // The circle, its caption, and the sparkline all follow the display mode, so the
                // screen cannot claim "Download" while the status bar shows something else.
                val heroFormatted: FormattedSpeed
                val heroLabel: String
                val heroSecondary: String?
                val heroBps: Double
                when (displayMode) {
                    SpeedDisplayMode.UPLOAD -> {
                        heroFormatted = upload
                        heroLabel = "Upload"
                        heroSecondary = null
                        heroBps = speed.uploadSpeed
                    }

                    SpeedDisplayMode.COMBINED -> {
                        heroFormatted = combined
                        heroLabel = "Total"
                        heroSecondary = null
                        heroBps = combinedBps
                    }

                    SpeedDisplayMode.BOTH -> {
                        heroFormatted = download
                        heroLabel = "Download"
                        heroSecondary = "\u2191 $upload"
                        heroBps = speed.downloadSpeed
                    }

                    SpeedDisplayMode.DOWNLOAD -> {
                        heroFormatted = download
                        heroLabel = "Download"
                        heroSecondary = null
                        heroBps = speed.downloadSpeed
                    }
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        heroSpeed = heroFormatted.value,
                        heroUnit = heroFormatted.unit,
                        heroLabel = heroLabel,
                        heroSecondary = heroSecondary,
                        downloadSpeed = download.value,
                        downloadUnit = download.unit,
                        uploadSpeed = upload.value,
                        uploadUnit = upload.unit,
                        ping = speed.ping,
                        peakDownload = if (speed.downloadSpeed > currentState.peakDownloadValue) {
                            download.toString()
                        } else currentState.peakDownload,
                        peakUpload = if (speed.uploadSpeed > currentState.peakUploadValue) {
                            upload.toString()
                        } else currentState.peakUpload,
                        peakDownloadValue = maxOf(speed.downloadSpeed, currentState.peakDownloadValue),
                        peakUploadValue = maxOf(speed.uploadSpeed, currentState.peakUploadValue),
                        sessionTime = NetworkUtils.formatTime(System.currentTimeMillis() / 1000 - currentState.sessionStartTime),
                        recentDownload = (currentState.recentDownload +
                                heroBps.toFloat()).takeLast(SPARKLINE_SAMPLES)
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

    fun resetPeakValues() {
        _uiState.update { currentState ->
            currentState.copy(
                peakDownload = "0 B/s",
                peakUpload = "0 B/s",
                peakDownloadValue = 0.0,
                peakUploadValue = 0.0
            )
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
