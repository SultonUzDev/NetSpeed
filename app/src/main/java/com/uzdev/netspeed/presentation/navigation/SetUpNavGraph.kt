package com.uzdev.netspeed.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uzdev.netspeed.presentation.home.HomeScreen
import com.uzdev.netspeed.presentation.result.ResultScreen
import com.uzdev.netspeed.presentation.settings.SettingsScreen

@Composable
fun SetUpNavGraph(navController: NavHostController, modifier: Modifier) {

    NavHost(
        navController = navController,
        startDestination = NavRoute.Home.route,
        modifier = modifier
    ) {
        composable(NavRoute.Home.route) {
            HomeScreen()
        }
        composable(NavRoute.Result.route) {
            ResultScreen()
        }
        composable(NavRoute.Settings.route) {
            SettingsScreen()
        }
    }
}