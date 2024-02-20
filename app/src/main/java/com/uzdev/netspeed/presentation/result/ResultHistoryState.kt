package com.uzdev.netspeed.presentation.result

import com.uzdev.netspeed.domain.model.NetworkHistory

data class ResultHistoryState(
    val networkHistoryList: List<NetworkHistory> = emptyList()
)
