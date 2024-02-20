package com.uzdev.netspeed

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.uzdev.netspeed.presentation.home.HomeViewModel
import com.uzdev.netspeed.presentation.navigation.BottomNavigationBar
import com.uzdev.netspeed.presentation.navigation.SetUpNavGraph
import com.uzdev.netspeed.ui.theme.NetSpeedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NetSpeedTheme {

                val navHostController = rememberNavController()
                Scaffold(bottomBar = {
                    BottomNavigationBar(navController = navHostController)
                }) { paddingValues ->
                    SetUpNavGraph(
                        navController = navHostController,
                        Modifier.padding(paddingValues)
                    )
                }


            }
        }
    }
}

