package com.uzdev.netspeed.presentation.result

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uzdev.netspeed.data.repo.NetworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ResultHistoryViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _state = mutableStateOf(ResultHistoryState())
    val state: State<ResultHistoryState> = _state

    private var netJob: Job? = null

    init {
        getResultHistory()
    }

    private fun getResultHistory() {
        netJob?.cancel()
        netJob = networkRepository.getNetworkTestHistory().onEach { networkHistories ->
            _state.value = state.value.copy(networkHistoryList = networkHistories)
        }.launchIn(viewModelScope)
    }

}