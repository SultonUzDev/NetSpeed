package com.sultonuzdev.netspeed.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sultonuzdev.netspeed.data.services.SpeedMonitorService
import com.sultonuzdev.netspeed.presentation.components.BottomNavigation
import com.sultonuzdev.netspeed.presentation.screens.settings.SettingsScreen
import com.sultonuzdev.netspeed.presentation.screens.speed.SpeedScreen
import com.sultonuzdev.netspeed.presentation.screens.usage.UsageScreen
import com.sultonuzdev.netspeed.presentation.theme.*
import com.sultonuzdev.netspeed.utils.BatteryOptimizationHelper
import com.sultonuzdev.netspeed.utils.Constants.ACTION_START_MONITORING
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
            val currentPage by mainViewModel.currentPage.collectAsStateWithLifecycle()
            val systemUiController = rememberSystemUiController()

            LaunchedEffect(isDarkTheme) {
                systemUiController.setSystemBarsColor(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    darkIcons = !isDarkTheme
                )
            }

            NetSpeedTheme(darkTheme = isDarkTheme) {
                NetSpeedApp(
                    currentPage = currentPage,
                    onPageSelected = mainViewModel::setCurrentPage,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(Manifest.permission.FOREGROUND_SERVICE)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
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
    onPageSelected: (Int) -> Unit,
    isDarkTheme: Boolean
) {
    // Get theme-appropriate colors
    val backgroundColors = if (isDarkTheme) {
        listOf(DarkBackground, DarkBackgroundVariant, DarkSurface)
    } else {
        listOf(LightBackground, LightBackgroundVariant, LightSurface)
    }

    val bottomNavBackground = if (isDarkTheme) {
        DarkBackground.copy(alpha = 0.95f)
    } else {
        LightBackground.copy(alpha = 0.95f)
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
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                bottomNavBackground
                            )
                        )
                    )
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()
                    )
            ) {
                BottomNavigation(
                    currentPage = currentPage,
                    onPageSelected = onPageSelected
                )
            }
        },
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
                0 -> SpeedScreen()
                1 -> UsageScreen()
                2 -> SettingsScreen()
            }
        }
    }
}