package com.sultonuzdev.netspeed.domain.usecases

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.utils.UsagePeriods
import kotlinx.coroutines.flow.first

/** Something worth telling the user about, produced by [CheckAlertsUseCase]. */
sealed interface Alert {
    data class Roaming(val mobileUsedThisCycle: Long) : Alert
    data class BackgroundData(val appLabel: String, val bytes: Long) : Alert
    data class AppLimit(val appLabel: String, val used: Long, val limit: Long) : Alert
}

/**
 * Decides which warnings are due, and remembers what has already been said.
 *
 * Every alert here is a one-shot: roaming announces itself once per roaming session, an app's
 * limit once per billing cycle, an app's background usage once per day. A monitor that checks
 * every few minutes would otherwise repeat the same warning indefinitely.
 */
class CheckAlertsUseCase(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val getAppUsageUseCase: GetAppUsageUseCase,
    private val getAccurateUsageUseCase: GetAccurateUsageUseCase
) {

    suspend fun check(): List<Alert> = buildList {
        checkRoaming()?.let { add(it) }

        // The remaining checks read per-app figures, which need usage access.
        if (!getAppUsageUseCase.hasUsageAccess()) return@buildList

        addAll(checkAppLimits())
        addAll(checkBackgroundData())
    }

    // --- roaming ---------------------------------------------------------------------------

    private suspend fun checkRoaming(): Alert.Roaming? {
        if (!preferencesManager.roamingAlert.first()) return null

        val roaming = isRoaming()
        val alreadyAlerted = preferencesManager.alertedRoaming.first()

        // Leaving a roaming network re-arms the warning for the next trip.
        if (!roaming) {
            if (alreadyAlerted) preferencesManager.updateAlertedRoaming(false)
            return null
        }
        if (alreadyAlerted) return null

        preferencesManager.updateAlertedRoaming(true)
        val resetDay = preferencesManager.monthlyResetDate.first()
        val used = getAccurateUsageUseCase.cycleTotal(resetDay)?.mobileUsage ?: 0L
        return Alert.Roaming(used)
    }

    private fun isRoaming(): Boolean {
        return try {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            // Only mobile can roam; NOT_ROAMING absent on a cellular link means it is.
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
        } catch (e: Exception) {
            false
        }
    }

    // --- per-app allowances ------------------------------------------------------------------

    private suspend fun checkAppLimits(): List<Alert.AppLimit> {
        val limits = preferencesManager.appLimits.first()
        if (limits.isEmpty()) return emptyList()

        val resetDay = preferencesManager.monthlyResetDate.first()
        val cycleKey = UsagePeriods.dayKey(UsagePeriods.billingCycleBounds(resetDay).first)
        val alerted = preferencesManager.alertedApps.first()
            .filter { it.startsWith("$cycleKey:") }
            .toMutableSet()

        val usage = runCatching { getAppUsageUseCase.forCycle(resetDay) }.getOrDefault(emptyList())

        val due = usage.mapNotNull { app ->
            val limit = limits[app.uid] ?: return@mapNotNull null
            val marker = "$cycleKey:${app.uid}"
            if (app.totalBytes < limit || marker in alerted) return@mapNotNull null
            alerted += marker
            Alert.AppLimit(app.appLabel, app.totalBytes, limit)
        }

        // Rewriting the set also drops markers from previous cycles, so it cannot grow forever.
        if (due.isNotEmpty()) preferencesManager.updateAlertedApps(alerted)
        return due
    }

    // --- background data ----------------------------------------------------------------------

    private suspend fun checkBackgroundData(): List<Alert.BackgroundData> {
        if (!preferencesManager.backgroundDataAlert.first()) return emptyList()

        val threshold = preferencesManager.backgroundDataThreshold.first()
        if (threshold <= 0L) return emptyList()

        val today = UsagePeriods.dayKey()
        val alerted = preferencesManager.alertedBackground.first()
            .filter { it.startsWith("$today:") }
            .toMutableSet()

        // Only the day's heaviest apps are worth the per-uid detail queries this needs.
        val candidates = runCatching { getAppUsageUseCase.forToday() }
            .getOrDefault(emptyList())
            .filter { it.totalBytes >= threshold }
            .take(MAX_APPS_INSPECTED)

        val due = candidates.mapNotNull { app ->
            val marker = "$today:${app.uid}"
            if (marker in alerted) return@mapNotNull null

            val detail = runCatching { getAppUsageUseCase.detailForToday(app.uid) }.getOrNull()
                ?: return@mapNotNull null
            if (!detail.hasStateBreakdown || detail.backgroundBytes < threshold) {
                return@mapNotNull null
            }

            alerted += marker
            Alert.BackgroundData(app.appLabel, detail.backgroundBytes)
        }

        if (due.isNotEmpty()) preferencesManager.updateAlertedBackground(alerted)
        return due
    }

    private companion object {
        const val MAX_APPS_INSPECTED = 5
    }
}
