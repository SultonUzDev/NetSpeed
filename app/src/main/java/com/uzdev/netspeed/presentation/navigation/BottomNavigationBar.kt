package com.uzdev.netspeed.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.uzdev.netspeed.R
import com.uzdev.netspeed.ui.theme.ColorBottomNavigationBar
import com.uzdev.netspeed.ui.theme.ColorBottomNavigationSelected
import com.uzdev.netspeed.ui.theme.ColorBottomNavigationUnSelected

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    BottomNavigation {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val navItems = listOf(NavRoute.Home, NavRoute.Result, NavRoute.Settings)

        navItems.forEach { navItems ->
            BottomNavigationItem(
                selected = currentRoute == navItems.route,
                onClick = {
                    navController.navigate(navItems.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                icon = { Icon(navItems.icon, contentDescription = null) },
                label = {
                    Text(
                        text = navItems.label,
                        fontFamily = FontFamily(Font(R.font.ameston_sanf))
                    )
                },


                selectedContentColor = ColorBottomNavigationSelected,
                unselectedContentColor = ColorBottomNavigationUnSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorBottomNavigationBar)

            )
        }


    }

}