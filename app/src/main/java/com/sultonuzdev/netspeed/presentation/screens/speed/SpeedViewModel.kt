package com.sultonuzdev.netspeed.presentation.screens.speed


import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.domain.repository.NetworkRepository
import com.sultonuzdev.netspeed.domain.usecases.GetNetworkSpeedUseCase
import com.sultonuzdev.netspeed.domain.usecases.SaveUsageDataUseCase
import com.sultonuzdev.netspeed.utils.DataUsageCalculator
import com.sultonuzdev.netspeed.utils.NetworkUtils
import com.sultonuzdev.netspeed.utils.NetworkUtils.formatBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpeedViewModel(
    private val getNetworkSpeedUseCase: GetNetworkSpeedUseCase,
    private val networkRepository: NetworkRepository,
    private val saveUsageDataUseCase: SaveUsageDataUseCase,
    application: Application

) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState.asStateFlow()

    init {
        DataUsageCalculator.initializeTracking(getApplication()) // Use getApplication()
        startMonitoring()
        observeNetworkSpeed()
        observeNetworkInfo()
        startUsageTracking()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            networkRepository.startMonitoring()
        }
    }

    private fun observeNetworkSpeed() {
        viewModelScope.launch {
            getNetworkSpeedUseCase().collect { speed ->
                val (downloadValue, downloadUnit) = NetworkUtils.formatSpeed(speed.downloadSpeed)
                val (uploadValue, uploadUnit) = NetworkUtils.formatSpeed(speed.uploadSpeed)

                _uiState.update { currentState ->
                    currentState.copy(
                        downloadSpeed = downloadValue,
                        downloadUnit = downloadUnit,
                        uploadSpeed = uploadValue,
                        uploadUnit = uploadUnit,
                        ping = speed.ping,
                        peakDownload = if (speed.downloadSpeed > currentState.peakDownloadValue) {
                            val (peakValue, peakUnit) = NetworkUtils.formatSpeed(speed.downloadSpeed)
                            "$peakValue $peakUnit"
                        } else currentState.peakDownload,
                        peakUpload = if (speed.uploadSpeed > currentState.peakUploadValue) {
                            val (peakValue, peakUnit) = NetworkUtils.formatSpeed(speed.uploadSpeed)
                            "$peakValue $peakUnit"
                        } else currentState.peakUpload,
                        peakDownloadValue = maxOf(speed.downloadSpeed, currentState.peakDownloadValue),
                        peakUploadValue = maxOf(speed.uploadSpeed, currentState.peakUploadValue),
                        sessionTime = NetworkUtils.formatTime(System.currentTimeMillis() / 1000 - currentState.sessionStartTime)
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

    private fun startUsageTracking() {
        viewModelScope.launch {
            while (true) {
                try {
                    updateUsageData()
                    delay(3000) // Update every 3 seconds
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(5000)
                }
            }
        }
    }

    private fun updateUsageData() {
        val todayUsage = DataUsageCalculator.updateUsage(getApplication()) // Use getApplication()

        // Calculate progress (5GB daily limit)
        val dailyLimit = 5L * 1024 * 1024 * 1024 // 5GB
        val wifiProgress = (todayUsage.wifiBytes.toFloat() / dailyLimit).coerceAtMost(1f)
        val mobileProgress = (todayUsage.mobileBytes.toFloat() / dailyLimit).coerceAtMost(1f)
        val totalProgress = (todayUsage.totalBytes.toFloat() / dailyLimit).coerceAtMost(1f)

        _uiState.update { currentState ->
            currentState.copy(
                todayWifiUsage = formatBytes(todayUsage.wifiBytes),
                todayMobileUsage = formatBytes(todayUsage.mobileBytes),
                todayTotalUsage = formatBytes(todayUsage.totalBytes),
                wifiProgress = wifiProgress,
                mobileProgress = mobileProgress,
                totalProgress = totalProgress
            )
        }

        // Save usage data to database
        val usageData = UsageData(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            wifiUsage = todayUsage.wifiBytes,
            mobileUsage = todayUsage.mobileBytes,
            totalUsage = todayUsage.totalBytes,
            sessionTime = System.currentTimeMillis() / 1000 - _uiState.value.sessionStartTime
        )
        viewModelScope.launch {
            saveUsageDataUseCase.updateUsage(usageData)
        }
    }

    fun resetTodayUsage() {
        DataUsageCalculator.resetTodayTracking()
        updateUsageData()
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
}