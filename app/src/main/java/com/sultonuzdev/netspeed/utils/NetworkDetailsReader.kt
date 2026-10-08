package com.sultonuzdev.netspeed.utils

import android.content.Context
import com.sultonuzdev.netspeed.R
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.sultonuzdev.netspeed.domain.models.NetworkDetail
import com.sultonuzdev.netspeed.domain.models.NetworkDetails
import java.net.Inet4Address
import java.net.Inet6Address

/**
 * Reads what the platform will tell us about the active connection using only normal permissions.
 *
 * Everything here comes from ACCESS_NETWORK_STATE and ACCESS_WIFI_STATE, both install-time
 * permissions. Fields that would require a runtime prompt are left out entirely rather than shown
 * as blanks: SSID and BSSID need location, and the mobile data generation (LTE/5G) needs
 * READ_PHONE_STATE.
 */
// Group headings. Held as ids and resolved per item, because the sheet groups by the
// rendered heading and that has to be the translated one.
private val CONNECTION = R.string.details_section_connection
private val ADDRESSES = R.string.details_section_addresses
private val RADIO = R.string.details_section_radio
private val CARRIER = R.string.details_section_carrier

class NetworkDetailsReader(
    private val context: Context,
) {

    fun read(): NetworkDetails {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return offline()

        val network = connectivityManager.activeNetwork ?: return offline()
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return offline()
        val linkProperties = connectivityManager.getLinkProperties(network)

        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        val items = buildList {
            addAll(commonItems(capabilities, linkProperties))
            when {
                isWifi -> addAll(wifiItems(context, capabilities))
                isCellular -> addAll(cellularItems(context))
            }
        }

        return NetworkDetails(
            title = context.getString(
                when {
                    isWifi -> R.string.transport_wifi
                    isCellular -> R.string.connection_mobile_data
                    else -> R.string.details_network
                }
            ),
            items = items
        )
    }

    private fun offline() = NetworkDetails(
        title = context.getString(R.string.connection_none),
        subtitle = context.getString(R.string.details_nothing_connected),
        items = emptyList()
    )

    private fun commonItems(
        capabilities: NetworkCapabilities,
        linkProperties: LinkProperties?
    ): List<NetworkDetail> = buildList {
        val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        add(NetworkDetail(context.getString(R.string.details_internet), context.getString(if (validated) R.string.details_reachable else R.string.details_not_verified), context.getString(CONNECTION)))
        val metered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        add(NetworkDetail(context.getString(R.string.details_metered), context.getString(if (metered) R.string.details_yes else R.string.details_no), context.getString(CONNECTION)))
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
            add(NetworkDetail(context.getString(R.string.details_roaming), context.getString(R.string.details_yes), context.getString(CONNECTION)))
        }

        // The platform's own estimate for the link, not a measurement.
        add(NetworkDetail(context.getString(R.string.details_estimated_downlink), formatKbps(capabilities.linkDownstreamBandwidthKbps), context.getString(CONNECTION)))
        add(NetworkDetail(context.getString(R.string.details_estimated_uplink), formatKbps(capabilities.linkUpstreamBandwidthKbps), context.getString(CONNECTION)))

        linkProperties?.interfaceName?.let { add(NetworkDetail(context.getString(R.string.details_interface), it, context.getString(CONNECTION))) }

        linkProperties?.linkAddresses
            ?.map { it.address }
            ?.let { addresses ->
                addresses.filterIsInstance<Inet4Address>().firstOrNull()?.hostAddress
                    ?.let { add(NetworkDetail(context.getString(R.string.details_ipv4), it, context.getString(ADDRESSES))) }
                addresses.filterIsInstance<Inet6Address>().firstOrNull()?.hostAddress
                    ?.substringBefore('%')
                    ?.let { add(NetworkDetail(context.getString(R.string.details_ipv6), it, context.getString(ADDRESSES))) }
            }

        linkProperties?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.take(2)
            ?.takeIf { it.isNotEmpty() }
            ?.let { add(NetworkDetail(context.getString(R.string.details_dns), it.joinToString(", "), context.getString(ADDRESSES))) }
    }

    private fun wifiItems(
        context: Context,
        capabilities: NetworkCapabilities
    ): List<NetworkDetail> {
        val wifiInfo = wifiInfoOf(context, capabilities) ?: return emptyList()

        return buildList {
            // SSID and BSSID are intentionally absent: both are redacted without location.
            wifiInfo.linkSpeed.takeIf { it > 0 }
                ?.let { add(NetworkDetail(context.getString(R.string.details_link_speed), context.getString(R.string.measure_megabits_per_second, it), context.getString(RADIO))) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiInfo.txLinkSpeedMbps.takeIf { it > 0 }
                    ?.let { add(NetworkDetail(context.getString(R.string.details_tx_link_speed), context.getString(R.string.measure_megabits_per_second, it), context.getString(RADIO))) }
                wifiInfo.rxLinkSpeedMbps.takeIf { it > 0 }
                    ?.let { add(NetworkDetail(context.getString(R.string.details_rx_link_speed), context.getString(R.string.measure_megabits_per_second, it), context.getString(RADIO))) }
            }

            wifiInfo.frequency.takeIf { it > 0 }?.let { frequency ->
                add(NetworkDetail(context.getString(R.string.details_frequency), context.getString(R.string.measure_mhz, frequency), context.getString(RADIO)))
                bandOf(frequency)?.let { add(NetworkDetail(context.getString(R.string.details_band), it, context.getString(RADIO))) }
            }

            wifiInfo.rssi.takeIf { it != 0 && it > -127 }
                ?.let { add(NetworkDetail(context.getString(R.string.details_signal), context.getString(R.string.measure_dbm, it), context.getString(RADIO))) }
        }
    }

    /**
     * Prefers the capabilities' transport info on API 29+, which is the non-deprecated route and
     * is scoped to the network we already resolved.
     */
    private fun wifiInfoOf(context: Context, capabilities: NetworkCapabilities): WifiInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (capabilities.transportInfo as? WifiInfo)?.let { return it }
        }
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            wifiManager?.connectionInfo
        } catch (e: Exception) {
            null
        }
    }

    private fun cellularItems(context: Context): List<NetworkDetail> = buildList {
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    ?: return@buildList

            // networkOperatorName and simOperatorName carry no permission requirement; the data
            // network generation would, so it is not reported here.
            telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() }
                ?.let { add(NetworkDetail(context.getString(R.string.details_operator), it, context.getString(CARRIER))) }
            telephonyManager.simOperatorName?.takeIf { it.isNotBlank() }
                ?.let { add(NetworkDetail(context.getString(R.string.details_sim_operator), it, context.getString(CARRIER))) }
        } catch (e: Exception) {
            // Nothing reportable on this device.
        }
    }

    private fun bandOf(frequencyMhz: Int): String? = when {
        frequencyMhz >= 5925 -> context.getString(R.string.band_6_ghz)
        frequencyMhz >= 4900 -> context.getString(R.string.band_5_ghz)
        frequencyMhz >= 2400 -> context.getString(R.string.band_2_4_ghz)
        else -> null
    }

    private fun formatKbps(kbps: Int): String = when {
        kbps <= 0 -> context.getString(R.string.value_unknown)
        kbps >= 1000 -> context.getString(R.string.measure_megabits_per_second, kbps / 1000)
        else -> context.getString(R.string.measure_kilobits_per_second, kbps)
    }
}
