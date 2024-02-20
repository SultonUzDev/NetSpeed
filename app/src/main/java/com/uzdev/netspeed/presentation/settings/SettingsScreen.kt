package com.uzdev.netspeed.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uzdev.netspeed.R
import com.uzdev.netspeed.data.preference.NetDataStoreManager
import com.uzdev.netspeed.ui.theme.BackgroundColor
import com.uzdev.netspeed.ui.theme.MainTextColor
import com.uzdev.netspeed.ui.theme.PurpleGrey40

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {

    val ctx = LocalContext.current
    val ping = NetDataStoreManager(ctx)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Settings",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MainTextColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            fontFamily = FontFamily(Font(R.font.ameston_sanf)),
            textAlign = TextAlign.Center
        )
        Divider(color = PurpleGrey40, thickness = 1.dp)
        val openDialog = remember { mutableStateOf(false) }
        ItemSettingScreen(
            title = "Clear history",
            icon = R.drawable.ic_delete,
            onClick = {
                openDialog.value = true
            }
        )

        if (openDialog.value) {
            AskClearing(
                onDismissRequest = { openDialog.value = false },
                onConfirmation = {
                    settingsViewModel.clearNetworkHistory()
                    openDialog.value = false
                },
            )
        }


        Divider(color = PurpleGrey40, thickness = 1.dp)
        ItemSettingScreen(
            title = "Change ping duration",
            icon = R.drawable.ic_duration,
            onClick = {
                settingsViewModel.savePingDuration(duration = 10.toString())
            }
        )
        Divider(color = PurpleGrey40, thickness = 1.dp)
        ItemSettingScreen(
            title = "About",
            icon = R.drawable.ic_about,
            onClick = { Toast.makeText(ctx, "About", Toast.LENGTH_SHORT).show() }
        )
        Divider(color = PurpleGrey40, thickness = 1.dp)
        ItemSettingScreen(
            title = "More apps",
            icon = R.drawable.ic_new,
            onClick = {
                Toast.makeText(ctx, "More apps", Toast.LENGTH_SHORT).show()
            }
        )
        Divider(color = PurpleGrey40, thickness = 1.dp)
    }


}

@Composable
fun AskClearing(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Rounded.Warning, contentDescription = "", tint = Color.Red)
        },
        title = {
            Text(text = "Are you sure?")
        },
        text = {
            Text(text = "If you clear history, you cannot restore it")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )

}

@Composable
fun ItemSettingScreen(title: String, icon: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(8.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically),
            color = MainTextColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            fontFamily = FontFamily(Font(R.font.super_dream))
        )
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .align(Alignment.CenterVertically)
        )
    }
}