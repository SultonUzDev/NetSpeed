package com.sultonuzdev.netspeed.presentation.screens.speed


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.repository.NetworkRepository
import com.sultonuzdev.netspeed.domain.usecases.GetNetworkSpeedUseCase
import com.sultonuzdev.netspeed.domain.usecases.RunSpeedTestUseCase
import com.sultonuzdev.netspeed.presentation.screens.speed.contract.SpeedTestUiState
import com.sultonuzdev.netspeed.presentation.screens.speed.contract.SpeedUiState
import com.sultonuzdev.netspeed.utils.NetworkDetailsReader
import com.sultonuzdev.netspeed.utils.SpeedFormatter
import com.sultonuzdev.netspeed.utils.SpeedUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

class SpeedViewModel(
    private val getNetworkSpeedUseCase: GetNetworkSpeedUseCase,
    private val networkRepository: NetworkRepository,
    private val preferencesManager: PreferencesManager,

    private val runSpeedTestUseCase: RunSpeedTestUseCase,
    private val networkDetailsReader: NetworkDetailsReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState.asStateFlow()


    private val _uiTestState = MutableStateFlow(SpeedTestUiState())
    val speedTestState: StateFlow<SpeedTestUiState> = _uiTestState.asStateFlow()

    private var testJob: Job? = null


    /**
     * Results are reported in the unit the user picked, except AUTO — a dial that changes unit
     * mid-sweep is unreadable, so AUTO resolves to Mbps, the convention for speed tests.
     */
    private var reportingUnit = SpeedUnit.MBPS


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
            preferencesManager.speedUnit.collect { unit ->
                reportingUnit = if (unit == SpeedUnit.AUTO) SpeedUnit.MBPS else unit
                speedUnit = unit
            }
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            networkRepository.startMonitoring()
        }
    }

    fun startTest() {
        if (_uiTestState.value.isRunning) return

        testJob?.cancel()
        _uiTestState.update {
            it.copy(
                phase = SpeedTestPhase.PINGING,
                liveBytesPerSecond = 0.0,
                downloadResult = "—",
                uploadResult = "—",
                pingResult = "—",
                jitterResult = "—",
                error = null
            )
        }

        testJob = viewModelScope.launch {
            try {
                val result = runSpeedTestUseCase { phase, bytesPerSecond ->
                    val formatted = SpeedFormatter.format(bytesPerSecond, reportingUnit)
                    _uiTestState.update { current ->
                        // Results used to be published only once everything finished, so the
                        // download figure stayed "--" for the entire upload phase -- right after
                        // the user had watched it settle on the gauge. Freeze it at the moment
                        // the phase ends instead.
                        val leftDownload = current.phase == SpeedTestPhase.DOWNLOADING &&
                                phase != SpeedTestPhase.DOWNLOADING

                        current.copy(
                            phase = phase,
                            liveBytesPerSecond = bytesPerSecond,
                            liveValue = formatted.value,
                            liveUnit = formatted.unit,
                            downloadResult = if (leftDownload) {
                                "${current.liveValue} ${current.liveUnit}"
                            } else {
                                current.downloadResult
                            }
                        )
                    }
                }

                val download = SpeedFormatter.format(result.downloadBytesPerSecond, reportingUnit)
                val upload = SpeedFormatter.format(result.uploadBytesPerSecond, reportingUnit)

                _uiTestState.update {
                    it.copy(
                        phase = SpeedTestPhase.DONE,
                        liveBytesPerSecond = result.downloadBytesPerSecond,
                        liveValue = download.value,
                        liveUnit = download.unit,
                        downloadResult = download.toString(),
                        uploadResult = upload.toString(),
                        pingResult = "${result.pingMillis} ms",
                        jitterResult = "${result.jitterMillis} ms"
                    )
                }
            } catch (e: Exception) {
                _uiTestState.update {
                    it.copy(phase = SpeedTestPhase.FAILED, error = describeFailure(e))
                }
            }
        }
    }

    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _uiTestState.update { it.copy(phase = SpeedTestPhase.IDLE, liveBytesPerSecond = 0.0) }
    }


    /**
     * Turns a network exception into something a user can act on.
     *
     * UnknownHostException in particular reads as a broken URL but almost always means the device
     * has no working connection -- there is nothing to resolve against.
     */
    private fun describeFailure(e: Exception): String = when (e) {
        is UnknownHostException ->
            "No internet connection. The test server could not be reached."

        is IOException ->
            "Connection interrupted. Check your network and try again."

        else -> e.message ?: "Speed test failed."
    }



    fun readNetworkDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(networkDetails = networkDetailsReader.read()) }
        }
    }

    /**
     * Must be called when the sheet closes. The sheet is composed only while this is non-null;
     * hiding it without clearing this left it composed in a hidden state, and the next tap set
     * new details onto a sheet that was already there and would not re-show.
     */
    fun clearNetworkDetails() {
        _uiState.update { it.copy(networkDetails = null) }
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
        testJob?.cancel()
    }

    private companion object {
        /** About a minute of history at the default one-second cadence. */
        const val SPARKLINE_SAMPLES = 60
    }
}
