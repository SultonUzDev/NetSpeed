package com.uzdev.netspeed.presentation.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uzdev.netspeed.R
import com.uzdev.netspeed.domain.model.NetworkHistory
import com.uzdev.netspeed.ui.theme.BackgroundColor
import com.uzdev.netspeed.ui.theme.DescriptorTextColor
import com.uzdev.netspeed.ui.theme.MainTextColor
import com.uzdev.netspeed.ui.theme.PurpleGrey40
import com.uzdev.netspeed.utils.NetworkType
import com.uzdev.netspeed.utils.convertLongToDate
import com.uzdev.netspeed.utils.convertLongToTime
import com.uzdev.netspeed.utils.formatSpeed
import com.uzdev.netspeed.utils.formatSpeedUnitType

@Composable
fun ResultScreen(
    resultHistoryViewModel: ResultHistoryViewModel = hiltViewModel()
) {

    val state = resultHistoryViewModel.state
    val histories = state.value.networkHistoryList

    Column(
        modifier = Modifier
            .background(BackgroundColor)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val modifier = Modifier.align(Alignment.CenterVertically)
            TopItems("Type", R.drawable.ic_all, modifier)
            TopItems(stringResource(R.string.date_amp_time), R.drawable.ic_time, modifier)
            TopItems("Download", R.drawable.ic_download, modifier)
            TopItems("Upload", R.drawable.ic_upload, modifier)
            TopItems("Ping", R.drawable.ic_network_ping, modifier)
        }

        Divider(color = PurpleGrey40, thickness = 1.dp)

        LazyColumn {
            itemsIndexed(histories) { index, item ->
                ItemResultHistory(networkHistory = item)
                if (index < histories.lastIndex) {
                    Divider(color = PurpleGrey40, thickness = 1.dp)
                }
            }
        }


    }

}


@Composable
fun TopItems(
    title: String,
    icon: Int,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = MainTextColor,
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.butler_extra_bold)),

        )
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 4.dp)
        )

    }
}

@Composable
fun ItemResultHistory(networkHistory: NetworkHistory) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val modifier = Modifier.align(Alignment.CenterVertically)

        val date = networkHistory.time.convertLongToDate()
        val time = networkHistory.time.convertLongToTime()

        ImageTypeView(type = networkHistory.type, modifier)
        ItemTextView(text = date, desc = time, modifier = modifier)
        ItemTextView(
            text = networkHistory.download.formatSpeed(),
            desc = networkHistory.download.formatSpeedUnitType() + "/s",
            modifier = modifier
        )

        ItemTextView(
            text = networkHistory.upload.formatSpeed(),
            desc = networkHistory.upload.formatSpeedUnitType() + "/s",
            modifier = modifier
        )

        val duration = networkHistory.pingDuration / 1000
        val desc = "s"

        ItemTextView(text = duration.toString(), desc = desc, modifier = modifier)

    }

}

@Composable
fun ItemTextView(text: String, desc: String, modifier: Modifier) {
    val builtString = buildAnnotatedString {
        withStyle(SpanStyle(color = MainTextColor, fontWeight = FontWeight.SemiBold)) {
            append(text)
        }
        withStyle(SpanStyle(color = DescriptorTextColor, fontWeight = FontWeight.Medium)) {
            append("\n" + desc)
        }

    }
    Text(text = builtString, modifier = modifier, textAlign = TextAlign.Center)

}


@Composable
fun ImageTypeView(type: String, modifier: Modifier) {
    var icon = 0
    when (type) {
        NetworkType.ALL.toString() -> {
            icon = R.drawable.ic_network
        }

        NetworkType.WIFI.toString() -> {
            icon = R.drawable.ic_wifi
        }

        NetworkType.MOBILE.toString() -> {
            icon = R.drawable.ic_cellular
        }
    }
    Image(
        painter = painterResource(id = icon), contentDescription = null,
        modifier = modifier.size(20.dp)
    )
}

