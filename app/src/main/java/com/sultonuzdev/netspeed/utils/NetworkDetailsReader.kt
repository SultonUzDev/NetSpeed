package com.sultonuzdev.netspeed.utils

import android.content.Context
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
object NetworkDetailsReader {

    fun read(context: Context): NetworkDetails {
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
            title = when {
                isWifi -> "Wi-Fi"
                isCellular -> "Mobile data"
                else -> "Network"
            },
            subtitle = "Available without extra permissions",
            items = items
        )
    }

    private fun offline() = NetworkDetails(
        title = "No connection",
        subtitle = "Nothing is connected right now",
        items = emptyList()
    )

    private fun commonItems(
        capabilities: NetworkCapabilities,
        linkProperties: LinkProperties?
    ): List<NetworkDetail> = buildList {
        add(
            NetworkDetail(
                "Internet",
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    "Reachable"
                } else {
                    "Connected, not verified"
                }
            )
        )
        add(
            NetworkDetail(
                "Metered",
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    "No"
                } else {
                    "Yes"
                }
            )
        )
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
            add(NetworkDetail("Roaming", "Yes"))
        }

        // The platform's own estimate for the link, not a measurement.
        add(
            NetworkDetail(
                "Estimated downlink",
                formatKbps(capabilities.linkDownstreamBandwidthKbps)
            )
        )
        add(
            NetworkDetail(
                "Estimated uplink",
                formatKbps(capabilities.linkUpstreamBandwidthKbps)
            )
        )

        linkProperties?.interfaceName?.let { add(NetworkDetail("Interface", it)) }

        linkProperties?.linkAddresses
            ?.map { it.address }
            ?.let { addresses ->
                addresses.filterIsInstance<Inet4Address>().firstOrNull()?.hostAddress
                    ?.let { add(NetworkDetail("IPv4 address", it)) }
                addresses.filterIsInstance<Inet6Address>().firstOrNull()?.hostAddress
                    ?.substringBefore('%')
                    ?.let { add(NetworkDetail("IPv6 address", it)) }
            }

        linkProperties?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?.take(2)
            ?.takeIf { it.isNotEmpty() }
            ?.let { add(NetworkDetail("DNS", it.joinToString(", "))) }
    }

    private fun wifiItems(
        context: Context,
        capabilities: NetworkCapabilities
    ): List<NetworkDetail> {
        val wifiInfo = wifiInfoOf(context, capabilities) ?: return emptyList()

        return buildList {
            // SSID and BSSID are intentionally absent: both are redacted without location.
            wifiInfo.linkSpeed.takeIf { it > 0 }
                ?.let { add(NetworkDetail("Link speed", "$it Mbps")) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiInfo.txLinkSpeedMbps.takeIf { it > 0 }
                    ?.let { add(NetworkDetail("Tx link speed", "$it Mbps")) }
                wifiInfo.rxLinkSpeedMbps.takeIf { it > 0 }
                    ?.let { add(NetworkDetail("Rx link speed", "$it Mbps")) }
            }

            wifiInfo.frequency.takeIf { it > 0 }?.let { frequency ->
                add(NetworkDetail("Frequency", "$frequency MHz"))
                bandOf(frequency)?.let { add(NetworkDetail("Band", it)) }
            }

            wifiInfo.rssi.takeIf { it != 0 && it > -127 }
                ?.let { add(NetworkDetail("Signal", "$it dBm")) }
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
                ?.let { add(NetworkDetail("Operator", it)) }
            telephonyManager.simOperatorName?.takeIf { it.isNotBlank() }
                ?.let { add(NetworkDetail("SIM operator", it)) }
        } catch (e: Exception) {
            // Nothing reportable on this device.
        }
    }

    private fun bandOf(frequencyMhz: Int): String? = when {
        frequencyMhz >= 5925 -> "6 GHz"
        frequencyMhz >= 4900 -> "5 GHz"
        frequencyMhz >= 2400 -> "2.4 GHz"
        else -> null
    }

    private fun formatKbps(kbps: Int): String = when {
        kbps <= 0 -> "Unknown"
        kbps >= 1000 -> "${kbps / 1000} Mbps"
        else -> "$kbps Kbps"
    }
}
