package com.sultonuzdev.netspeed.data.datasource

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import com.sultonuzdev.netspeed.domain.models.AppUsage
import com.sultonuzdev.netspeed.domain.models.AppUsageDetail
import com.sultonuzdev.netspeed.domain.models.UsageData
import com.sultonuzdev.netspeed.utils.UsageAccessHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads usage straight from the platform's own accounting via [NetworkStatsManager].
 *
 * This is the accurate path: the buckets are the same ones Settings > Network & internet shows,
 * so our numbers agree with the system (and, in turn, far more closely with the carrier) instead
 * of being re-derived from sampled [android.net.TrafficStats] deltas. It also survives reboots,
 * process death and midnight, none of which sampling handles on its own.
 *
 * Requires usage access; see [UsageAccessHelper].
 */
class NetworkStatsDataSource(private val context: Context) {

    private val statsManager: NetworkStatsManager? by lazy {
        try {
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
        } catch (e: Exception) {
            null
        }
    }

    private val packageManager: PackageManager get() = context.packageManager

    /**
     * Serialises access to [NetworkStatsManager].
     *
     * Its sessions are not dependable under concurrent use: overlapping queries can come back
     * empty rather than throwing. The Usage screen fans out several reads at once (a 30-day
     * history sweep, cycle totals, the cap check and the per-app breakdown), and without this the
     * per-app query would lose that race and render as "no traffic recorded".
     */
    private val queryLock = Mutex()

    fun hasUsageAccess(): Boolean = UsageAccessHelper.hasUsageAccess(context)

    /**
     * Device-wide wifi/mobile totals for the window. Mobile is queried with a null subscriber id,
     * which the platform reads as "every subscriber" — that is what makes dual-SIM devices add up
     * instead of reporting whichever SIM happened to be default, and it avoids the privileged
     * `getSubscriberId()` call that throws on API 29+.
     */
    suspend fun queryDeviceUsage(startMillis: Long, endMillis: Long): UsageData? =
        withContext(Dispatchers.IO) {
            val manager = statsManager ?: return@withContext null
            if (!hasUsageAccess()) return@withContext null

            val (wifi, mobile) = queryLock.withLock {
                queryTotalForType(manager, ConnectivityManager.TYPE_WIFI, startMillis, endMillis) to
                        queryTotalForType(
                            manager,
                            ConnectivityManager.TYPE_MOBILE,
                            startMillis,
                            endMillis
                        )
            }
            if (wifi == null && mobile == null) return@withContext null

            val wifiBytes = wifi ?: 0L
            val mobileBytes = mobile ?: 0L
            UsageData(
                date = "",
                wifiUsage = wifiBytes,
                mobileUsage = mobileBytes,
                totalUsage = wifiBytes + mobileBytes,
                sessionTime = 0L
            )
        }

    private fun queryTotalForType(
        manager: NetworkStatsManager,
        networkType: Int,
        startMillis: Long,
        endMillis: Long
    ): Long? {
        return try {
            @Suppress("DEPRECATION")
            val bucket = manager.querySummaryForDevice(networkType, null, startMillis, endMillis)
            bucket?.let { it.rxBytes + it.txBytes }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Per-app usage for the window, aggregated by uid and resolved to a launcher-visible label.
     *
     * Buckets are per-uid, not per-package, so shared-uid packages collapse into one entry; we
     * label those with the first package and note the sharing count, which is what the platform
     * itself does.
     */
    suspend fun queryAppUsage(startMillis: Long, endMillis: Long): List<AppUsage> =
        withContext(Dispatchers.IO) {
            val manager = statsManager ?: return@withContext emptyList()
            if (!hasUsageAccess()) return@withContext emptyList()

            val (wifiByUid, mobileByUid) = queryLock.withLock {
                queryUidTotals(manager, ConnectivityManager.TYPE_WIFI, startMillis, endMillis) to
                        queryUidTotals(
                            manager,
                            ConnectivityManager.TYPE_MOBILE,
                            startMillis,
                            endMillis
                        )
            }

            (wifiByUid.keys + mobileByUid.keys)
                .map { uid ->
                    val (pkg, label) = resolveUid(uid)
                    AppUsage(
                        uid = uid,
                        packageName = pkg,
                        appLabel = label,
                        wifiBytes = wifiByUid[uid] ?: 0L,
                        mobileBytes = mobileByUid[uid] ?: 0L
                    )
                }
                .filter { it.totalBytes > 0L }
                .sortedByDescending { it.totalBytes }
        }

    private fun queryUidTotals(
        manager: NetworkStatsManager,
        networkType: Int,
        startMillis: Long,
        endMillis: Long
    ): Map<Int, Long> {
        val totals = mutableMapOf<Int, Long>()
        var stats: NetworkStats? = null
        try {
            @Suppress("DEPRECATION")
            stats = manager.querySummary(networkType, null, startMillis, endMillis)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val bytes = bucket.rxBytes + bucket.txBytes
                if (bytes <= 0L) continue
                totals[bucket.uid] = (totals[bucket.uid] ?: 0L) + bytes
            }
        } catch (e: SecurityException) {
            return emptyMap()
        } catch (e: Exception) {
            return totals
        } finally {
            try {
                stats?.close()
            } catch (e: Exception) {
                // nothing useful to do if the cursor refuses to close
            }
        }
        return totals
    }

    /**
     * One app's usage split by transport and by foreground/background.
     *
     * Uses queryDetailsForUid rather than querySummary: only the details query reliably separates
     * buckets by state, which is where the foreground/background numbers come from. It is a
     * heavier call, so it runs on demand for a single app rather than for the whole list.
     */
    suspend fun queryAppDetail(
        uid: Int,
        startMillis: Long,
        endMillis: Long
    ): AppUsageDetail? = withContext(Dispatchers.IO) {
        val manager = statsManager ?: return@withContext null
        if (!hasUsageAccess()) return@withContext null

        val (pkg, label) = resolveUid(uid)

        val wifi = queryLock.withLock {
            queryUidDetail(manager, ConnectivityManager.TYPE_WIFI, uid, startMillis, endMillis)
        }
        val mobile = queryLock.withLock {
            queryUidDetail(manager, ConnectivityManager.TYPE_MOBILE, uid, startMillis, endMillis)
        }

        AppUsageDetail(
            uid = uid,
            packageName = pkg,
            appLabel = label,
            wifiBytes = wifi.total,
            mobileBytes = mobile.total,
            foregroundBytes = wifi.foreground + mobile.foreground,
            backgroundBytes = wifi.background + mobile.background
        )
    }

    private class StateTotals(
        var total: Long = 0L,
        var foreground: Long = 0L,
        var background: Long = 0L
    )

    private fun queryUidDetail(
        manager: NetworkStatsManager,
        networkType: Int,
        uid: Int,
        startMillis: Long,
        endMillis: Long
    ): StateTotals {
        val totals = StateTotals()
        var stats: NetworkStats? = null
        try {
            @Suppress("DEPRECATION")
            stats = manager.queryDetailsForUid(networkType, null, startMillis, endMillis, uid)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val bytes = bucket.rxBytes + bucket.txBytes
                if (bytes <= 0L) continue
                totals.total += bytes
                when (bucket.state) {
                    NetworkStats.Bucket.STATE_FOREGROUND -> totals.foreground += bytes
                    NetworkStats.Bucket.STATE_DEFAULT -> totals.background += bytes
                    // STATE_ALL means this device does not split by state; leave both at zero so
                    // the UI can tell "no split available" from "nothing in the background".
                    else -> Unit
                }
            }
        } catch (e: Exception) {
            return totals
        } finally {
            try {
                stats?.close()
            } catch (e: Exception) {
                // nothing useful to do if the cursor refuses to close
            }
        }
        return totals
    }

    /** Maps a uid to (packageName, displayLabel), covering the platform's synthetic uids. */
    private fun resolveUid(uid: Int): Pair<String, String> {
        when (uid) {
            NetworkStats.Bucket.UID_TETHERING -> return TETHERING_PACKAGE to "Tethering"
            NetworkStats.Bucket.UID_REMOVED -> return REMOVED_PACKAGE to "Removed apps"
            NetworkStats.Bucket.UID_ALL -> return ALL_PACKAGE to "All traffic"
            Process.SYSTEM_UID -> return "android" to "Android OS"
        }

        SYSTEM_UID_LABELS[uid]?.let { return "android" to it }

        val packages = try {
            packageManager.getPackagesForUid(uid)
        } catch (e: Exception) {
            null
        }

        if (!packages.isNullOrEmpty()) {
            val primary = packages.first()
            val label = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(primary, 0)
                ).toString()
            } catch (e: Exception) {
                primary
            }
            val suffix = if (packages.size > 1) " (+${packages.size - 1})" else ""
            return primary to (label + suffix)
        }

        // Last resort before giving up on a name. getNameForUid still answers for some uids that
        // getPackagesForUid will not surface -- shared user ids in particular, which it returns
        // as "shared:android.uid.something".
        val fallbackName = try {
            packageManager.getNameForUid(uid)
        } catch (e: Exception) {
            null
        }
        if (!fallbackName.isNullOrBlank()) {
            return fallbackName to prettifyUidName(fallbackName)
        }

        return "uid:$uid" to "System service (uid $uid)"
    }

    /** Turns "shared:android.uid.system" or "com.foo.bar" into something readable. */
    private fun prettifyUidName(name: String): String {
        val cleaned = name.removePrefix("shared:")
        return cleaned.substringAfterLast('.')
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
            .ifBlank { cleaned }
    }

    companion object {
        /** Well-known platform uids, which never map to an installed package. */
        private val SYSTEM_UID_LABELS = mapOf(
            0 to "Root",
            1001 to "Telephony",
            1013 to "Media server",
            1019 to "DRM service",
            1021 to "GPS",
            1073 to "Network stack",
            9999 to "Nobody"
        )

        const val TETHERING_PACKAGE = "com.sultonuzdev.netspeed.tethering"
        const val REMOVED_PACKAGE = "com.sultonuzdev.netspeed.removed"
        const val ALL_PACKAGE = "com.sultonuzdev.netspeed.all"
    }
}
