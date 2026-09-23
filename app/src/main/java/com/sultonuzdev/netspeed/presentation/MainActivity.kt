package com.sultonuzdev.netspeed.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import com.sultonuzdev.netspeed.presentation.components.BottomNavigation
import com.sultonuzdev.netspeed.presentation.screens.history.HistoryScreen
import com.sultonuzdev.netspeed.presentation.screens.settings.SettingsScreen
import com.sultonuzdev.netspeed.presentation.screens.speed.SpeedScreen
import com.sultonuzdev.netspeed.presentation.screens.usage.UsageScreen
import com.sultonuzdev.netspeed.presentation.theme.*
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition {
            false
        }

        // No permission dialog on launch. Live speed comes from TrafficStats, which needs no
        // permission at all, so the screen this opens on is already the whole point of the app.
        // Notifications, phone state and battery exemption are asked for by the feature that
        // needs them, at the moment it is used -- see PermissionPrimer.
        startSpeedMonitorService()

        setContent {
            val isDarkTheme by mainViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val isDynamicColor by mainViewModel.isDynamicColor.collectAsStateWithLifecycle()
            val currentPage by mainViewModel.currentPage.collectAsStateWithLifecycle()
            // Re-applied on theme change so the status/nav bar icons flip with the app, not
            // with the system setting.
            LaunchedEffect(isDarkTheme) {
                val bars = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                ) { isDarkTheme }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }

            // Binds the composition to the Koin instance started in the Application. Without
            // it koinViewModel() still resolves, but only by falling back to the default context
            // and logging a warning on every composition.
            KoinAndroidContext {
                NetSpeedTheme(darkTheme = isDarkTheme, dynamicColor = isDynamicColor) {
                    NetSpeedApp(
                        currentPage = currentPage,
                        onPageSelected = mainViewModel::setCurrentPage
                    )
                }
            }
        }
    }

    private fun startSpeedMonitorService() {
        lifecycleScope.launch {
            if (mainViewModel.shouldAutoStartMonitoring()) startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, SpeedMonitorService::class.java).apply {
            action = ACTION_START_MONITORING
        }

        startForegroundService(intent)
    }
}

/** Widest the content is allowed to run; beyond this a tablet gets margins, not longer rows. */
private val ContentMaxWidth = 600.dp

@Composable
fun NetSpeedApp(
    currentPage: Int,
    onPageSelected: (Int) -> Unit
) {
    // Derived from the active scheme rather than the app's fixed palette, so a Material You
    // wallpaper palette reaches the background too.
    val backgroundColors = listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.netSpeedColors.backgroundVariant,
        MaterialTheme.colorScheme.surface
    )

    // From any tab other than the first, back returns to it rather than leaving the app.
    // Without a handler the system default applies and the Activity simply finishes.
    BackHandler(enabled = currentPage != 0) {
        onPageSelected(0)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = backgroundColors,
                    radius = 1000f
                )
            ),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        ) {
            // Every screen is a single column of cards, which on a tablet stretched to the full
            // 1600px: rows metres apart, a dial marooned in the middle of an empty page. Capping
            // the content and centring it keeps the phone layout untouched (it is already
            // narrower than the cap) and gives large screens a readable measure instead.
            Box(
                modifier = Modifier
                    // widthIn before fillMaxSize: the other way round the fill has already
                    // pinned the width to the whole screen and the cap has nothing left to do.
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                when (currentPage) {
                    0 -> SpeedScreen()
                    1 -> UsageScreen()
                    2 -> HistoryScreen()
                    3 -> SettingsScreen()
                }
            }

            // The bar follows the content rather than the screen, so it stays under the thumb
            // instead of spreading across the full width of a tablet.
            Box(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .align(Alignment.BottomCenter)
            ) {
                BottomNavigation(
                    currentPage = currentPage,
                    onPageSelected = onPageSelected
                )
            }
        }
    }
}