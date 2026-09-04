package com.sultonuzdev.netspeed.presentation.screens.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.domain.usecases.RunSpeedTestUseCase
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

class SpeedTestViewModel(
    private val runSpeedTestUseCase: RunSpeedTestUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    private var testJob: Job? = null

    /**
     * Results are reported in the unit the user picked, except AUTO — a dial that changes unit
     * mid-sweep is unreadable, so AUTO resolves to Mbps, the convention for speed tests.
     */
    private var reportingUnit = SpeedUnit.MBPS

    init {
        observeUnit()
    }

    private fun observeUnit() {
        viewModelScope.launch {
            preferencesManager.speedUnit.collect { unit ->
                reportingUnit = if (unit == SpeedUnit.AUTO) SpeedUnit.MBPS else unit
            }
        }
    }

    fun startTest() {
        if (_uiState.value.isRunning) return

        testJob?.cancel()
        _uiState.update {
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
                    _uiState.update { current ->
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

                _uiState.update {
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
                _uiState.update {
                    it.copy(phase = SpeedTestPhase.FAILED, error = describeFailure(e))
                }
            }
        }
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

    fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _uiState.update { it.copy(phase = SpeedTestPhase.IDLE, liveBytesPerSecond = 0.0) }
    }

    override fun onCleared() {
        super.onCleared()
        testJob?.cancel()
    }
}
