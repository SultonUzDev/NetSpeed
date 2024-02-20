package com.uzdev.netspeed.presentation.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uzdev.netspeed.data.network.NetworkSpeedChecker
import com.uzdev.netspeed.data.network.NetworkUsageManager
import com.uzdev.netspeed.data.preference.NetDataStoreManager
import com.uzdev.netspeed.data.repo.NetworkRepository
import com.uzdev.netspeed.domain.model.NetworkHistory
import com.uzdev.netspeed.domain.model.Speed
import com.uzdev.netspeed.utils.NetworkType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkUsageManager: NetworkUsageManager,
    private val networkRepository: NetworkRepository,
    private val netDataStoreManager: NetDataStoreManager
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkUsageState())
    val state: StateFlow<NetworkUsageState> = _state

    private var netJob: Job? = null

    init {
        viewModelScope.launch {
            netDataStoreManager.getPingDuration().collect() {
                _pingDuration.value = it
            }
        }
    }


    private val _pingDuration = mutableStateOf("")
    val pingDuration: State<String> = _pingDuration


    fun savePingDuration(duration: String) {
        viewModelScope.launch(Dispatchers.IO) {
            netDataStoreManager.savePingDuration(duration = duration)
        }
    }

    fun startChecking(type: NetworkType, duration: String) {
        netJob?.cancel()
        val d = duration.toInt() * 1000L
        var t: Long = 0
        val step: Long = 100
        var maxDownload = 0L
        var maxUpload = 0L
        var maxTotal = 0L


        netJob = viewModelScope.launch {
            while (true) {
                val now = networkUsageManager.getUsageNow(type)
                val speeds =
                    NetworkSpeedChecker.calculateSpeed(now.timeTaken, now.downloads, now.uploads)

                val totalSpeed = speeds[0]
                val downloadSpeed = speeds[1]
                val uploadSpeed = speeds[2]
                val total = totalSpeed.speed

                val download = downloadSpeed.speed
                val upload = uploadSpeed.speed

                _state.value = state.value.copy(
                    maxDownloadSpeed = Speed(speed = maxDownload),
                    maxUploadSpeed = Speed(speed = maxUpload),
                    maxTotalSpeed = Speed(speed = maxTotal),
                    duration = t
                )

                if (maxTotal < total) {
                    maxTotal = total
                }

                if (maxDownload < download) {
                    maxDownload = download
                }
                if (maxUpload < upload) {
                    maxUpload = upload
                }

                delay(step)

                if (t == d) {
                    stopChecking()
                    networkRepository.insertNetworkResult(
                        NetworkHistory(
                            0,
                            type = type.toString(),
                            time = System.currentTimeMillis(),
                            download = maxDownload,
                            upload = maxUpload,
                            pingDuration = t,
                        )
                    )
                    break
                }
                t += step


            }

        }
    }


    fun stopChecking() {
        netJob?.cancel("finished")
    }


}