package com.sultonuzdev.netspeed.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import com.sultonuzdev.netspeed.presentation.components.BottomNavigation
import com.sultonuzdev.netspeed.presentation.screens.history.HistoryScreen
import com.sultonuzdev.netspeed.presentation.screens.settings.SettingsScreen
import com.sultonuzdev.netspeed.presentation.screens.speed.SpeedScreen
import com.sultonuzdev.netspeed.presentation.screens.speedtest.SpeedTestScreen
import com.sultonuzdev.netspeed.presentation.screens.usage.UsageScreen
import com.sultonuzdev.netspeed.presentation.theme.*
import com.sultonuzdev.netspeed.utils.BatteryOptimizationHelper
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            requestBatteryOptimizationIfNeeded()
        }
    }

    private val batteryOptimizationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        startSpeedMonitorService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition {
            false
        }

        requestNecessaryPermissions()

        setContent {
            val isDarkTheme by mainViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val isDynamicColor by mainViewModel.isDynamicColor.collectAsStateWithLifecycle()
            val currentPage by mainViewModel.currentPage.collectAsStateWithLifecycle()
            val systemUiController = rememberSystemUiController()

            LaunchedEffect(isDarkTheme) {
                systemUiController.setSystemBarsColor(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    darkIcons = !isDarkTheme
                )
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

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Mobile signal strength only; the reader degrades to "no reading" if declined.
            add(Manifest.permission.READ_PHONE_STATE)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            requestBatteryOptimizationIfNeeded()
        }
    }

    private fun requestBatteryOptimizationIfNeeded() {
        BatteryOptimizationHelper.requestBatteryOptimizationPermission(
            this,
            batteryOptimizationLauncher
        ) {
            startSpeedMonitorService()
        }
    }

    private fun startSpeedMonitorService() {
        val intent = Intent(this, SpeedMonitorService::class.java).apply {
            action = ACTION_START_MONITORING
        }

        startForegroundService(intent)
    }
}

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

    // The speed test is a mode of the Speed tab rather than a fifth tab: it is something you
    // start and finish, not a place you browse. Saveable so a rotation does not drop you out of
    // a running test.
    var showSpeedTest by rememberSaveable { mutableStateOf(false) }

    // Nothing was intercepting back, so the system default applied and the Activity finished --
    // pressing back inside the speed test dropped the user out of the app entirely.
    BackHandler(enabled = showSpeedTest) {
        showSpeedTest = false
    }

    // From any tab other than the first, back returns to it rather than leaving the app. The two
    // handlers are mutually exclusive, so their registration order does not matter.
    BackHandler(enabled = !showSpeedTest && currentPage != 0) {
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
            when (currentPage) {
                0 -> if (showSpeedTest) {
                    SpeedTestScreen(onBack = { showSpeedTest = false })
                } else {
                    SpeedScreen(onRunSpeedTest = { showSpeedTest = true })
                }

                1 -> UsageScreen()
                2 -> HistoryScreen()
                3 -> SettingsScreen()
            }


            Box(modifier = Modifier.align(Alignment.BottomCenter)){
                // The speed test is a full-screen mode with its own back button; the nav bar both
                // covered its content and invited switching tabs mid-test.
                if (!showSpeedTest) {
                    // NavigationBar applies the navigation-bar inset itself, so adding it here too
                    // would pad the bar down by the gesture bar's height twice.
                    BottomNavigation(
                        currentPage = currentPage,
                        onPageSelected = onPageSelected
                    )
                }
            }
        }
    }
}