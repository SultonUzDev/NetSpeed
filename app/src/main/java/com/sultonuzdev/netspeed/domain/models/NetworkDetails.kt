package com.sultonuzdev.netspeed.domain.models

/** One labelled fact about the active connection. */
data class NetworkDetail(val label: String, val value: String, val section: String)

/**
 * What can be told about the current network without asking for a runtime permission.
 *
 * Deliberately excludes anything gated behind location (Wi-Fi SSID/BSSID) or READ_PHONE_STATE
 * (data network generation, subscriber identifiers) so that opening this never triggers a
 * permission prompt.
 */
data class NetworkDetails(
    val title: String,
    /** Only set when there is nothing to list; a heading alone is enough otherwise. */
    val subtitle: String = "",
    val items: List<NetworkDetail>
) {
    val isEmpty: Boolean get() = items.isEmpty()
}
