package com.sultonuzdev.netspeed.data.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.sultonuzdev.netspeed.R
import com.sultonuzdev.netspeed.data.datastore.PreferencesManager
import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
import com.sultonuzdev.netspeed.utils.Constants.ACTION_STOP_MONITORING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Quick Settings tile that starts and stops monitoring.
 *
 * Reads the persisted monitoring flag rather than tracking its own state, so the tile agrees with
 * the notification and with what happens after a reboot.
 */
class SpeedTileService : TileService(), KoinComponent {

    private val preferencesManager: PreferencesManager by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val running = preferencesManager.monitoringEnabled.first()
            val action = if (running) ACTION_STOP_MONITORING else ACTION_START_MONITORING

            withContext(Dispatchers.Main) {
                val intent = Intent(this@SpeedTileService, SpeedMonitorService::class.java)
                    .apply { this.action = action }
                try {
                    ContextCompat.startForegroundService(this@SpeedTileService, intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // The service writes the flag itself; reflect the intent immediately so the tile does
            // not appear unresponsive while that write lands.
            setTileState(active = !running)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun refreshTile() {
        scope.launch {
            setTileState(preferencesManager.monitoringEnabled.first())
        }
    }

    private suspend fun setTileState(active: Boolean) = withContext(Dispatchers.Main) {
        val tile: Tile = qsTile ?: return@withContext
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon =
            Icon.createWithResource(this@SpeedTileService, R.drawable.ic_speed_notification_bg)
        try {
            tile.updateTile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
