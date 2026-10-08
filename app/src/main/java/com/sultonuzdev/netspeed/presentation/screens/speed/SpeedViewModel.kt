package com.sultonuzdev.netspeed.presentation.screens.speed


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.domain.models.SpeedTestPhase
import com.sultonuzdev.netspeed.data.repository.NetworkRepository
import com.sultonuzdev.netspeed.data.repository.SpeedTestRepository
import com.sultonuzdev.netspeed.presentation.screens.speed.contract.SpeedTestUiState
import com.sultonuzdev.netspeed.presentation.screens.speed.contract.SpeedUiState
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.utils.StringProvider
import com.sultonuzdev.netspeed.utils.NetworkDetailsReader
import com.sultonuzdev.netspeed.utils.SpeedFormatter
import com.sultonuzdev.netspeed.utils.SpeedUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

class SpeedViewModel(
    private val networkRepository: NetworkRepository,
    private val preferencesManager: PreferencesManager,
    private val speedTestRepository: SpeedTestRepository,
    private val networkDetailsReader: NetworkDetailsReader,
    private val strings: StringProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState.asStateFlow()

    /** Set once the status-bar offer has been turned down, so it does not come back. */
    val notificationPromptDismissed: StateFlow<Boolean> =
        preferencesManager.notificationPromptDismissed
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun dismissNotificationPrompt() {
        viewModelScope.launch { preferencesManager.dismissNotificationPrompt() }
    }


    private val _uiTestState = MutableStateFlow(SpeedTestUiState())
    val speedTestState: StateFlow<SpeedTestUiState> = _uiTestState.asStateFlow()

    /**
     * Emitted the once, when a rating is worth asking for. A one-shot event rather than
     * state: replayed state would re-open the sheet on every recomposition and on every
     * return to the screen.
     */
    private val _reviewRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reviewRequests: SharedFlow<Unit> = _reviewRequests.asSharedFlow()

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
                val result = speedTestRepository.runTest { phase, bytesPerSecond ->
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
                                strings.get(
                                    R.string.value_with_unit,
                                    current.liveValue,
                                    current.liveUnit
                                )
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
                        pingResult = strings.get(R.string.measure_milliseconds, result.pingMillis),
                        jitterResult = strings.get(R.string.measure_milliseconds, result.jitterMillis)
                    )
                }

                // A finished test is the one moment the app has just visibly done its job, which
                // is the only honest place to ask for a rating. Counted rather than asked every
                // time, so it is never the first thing a new install sees.
                //
                // Guarded separately: this runs inside the same try as the test itself, so an
                // unwrapped failure writing the count would report a test that had just
                // succeeded as failed.
                runCatching {
                    if (preferencesManager.recordCompletedSpeedTest() == TESTS_BEFORE_REVIEW) {
                        _reviewRequests.tryEmit(Unit)
                    }
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
        is UnknownHostException -> strings.get(R.string.speed_test_error_no_internet)
        is IOException -> strings.get(R.string.speed_test_error_interrupted)
        // Deliberately not e.message: a platform exception message is untranslated, often in
        // English regardless of locale, and occasionally leaks a URL or a stack frame.
        else -> strings.get(R.string.speed_test_error_generic)
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
            networkRepository.getNetworkSpeed().collect { speed ->
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

        /** Fourth finished test, so the app has proven useful several times before it asks. */
        const val TESTS_BEFORE_REVIEW = 4
    }
}
