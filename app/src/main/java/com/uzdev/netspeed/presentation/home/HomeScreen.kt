package com.uzdev.netspeed.presentation.home

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uzdev.netspeed.R
import com.uzdev.netspeed.ui.theme.BackgroundColor
import com.uzdev.netspeed.ui.theme.Color1
import com.uzdev.netspeed.ui.theme.Color2
import com.uzdev.netspeed.ui.theme.Color3
import com.uzdev.netspeed.ui.theme.DescriptorTextColor
import com.uzdev.netspeed.ui.theme.MainTextColor
import com.uzdev.netspeed.utils.Utils
import com.uzdev.netspeed.utils.formatSpeed
import com.uzdev.netspeed.utils.formatSpeedUnitType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val state = homeViewModel.state.collectAsState()
    val mDownloadSpeed = state.value.maxDownloadSpeed
    val mUploadSpeed = state.value.maxUploadSpeed
    val mTotalSpeed = state.value.maxTotalSpeed
    val duration = state.value.duration
    val context = LocalContext.current
    val pingDuration by homeViewModel::pingDuration
    Log.d("mlog", "It is ${pingDuration.value}: ");

    Column(
        modifier = Modifier
            .background(BackgroundColor)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,

        ) {


        TopHomeScreen(
            downloadSpeed = mDownloadSpeed.speed,
            uploadSpeed = mUploadSpeed.speed,
            pingDuration = duration.toString()
        )


        SpeedometerScreen(
            currentSpeed = mTotalSpeed.speed.formatSpeed().toFloat(),
            scoreType = mTotalSpeed.speed.formatSpeedUnitType(),

            modifier = Modifier
                .padding(
                    top = 60.dp, end = 10.dp, start = 10.dp
                )
                .requiredSize(380.dp)
        )



        OutlinedButton(
            onClick = {


                if (Utils.isOnline(context)) {
                    val type = Utils.checkInternetType(context = context)
                    homeViewModel.startChecking(type, pingDuration.value)
                } else {
                    Toast.makeText(
                        context,
                        "Please, make sure internet connected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Test",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.oh_my_notes))
            )
        }

        OutlinedButton(
            onClick = {
                homeViewModel.stopChecking()
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Stop",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.oh_my_notes))
            )
        }
    }


}

/*
TopScreen of HomeScreen
 */
@Composable
fun TopHomeScreen(
    downloadSpeed: Long,
    uploadSpeed: Long,
    pingDuration: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val modifier = Modifier.weight(1f)
        ItemTopHomeScreen(title = "Ping", descMain = pingDuration, desc = "ms", modifier = modifier)
        ItemTopHomeScreen(
            title = "Download",
            descMain = downloadSpeed.formatSpeed(),
            desc = "${downloadSpeed.formatSpeedUnitType()}/s",
            icon = R.drawable.ic_down,
            modifier = modifier
        )
        ItemTopHomeScreen(
            title = "Upload",
            descMain = uploadSpeed.formatSpeed(),
            desc = "${uploadSpeed.formatSpeedUnitType()}/s",
            icon = R.drawable.ic_up,
            modifier = modifier
        )

    }
}

@Composable
fun ItemTopHomeScreen(
    title: String,
    descMain: String,
    desc: String,
    icon: Int? = null,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            androidx.compose.material3.Text(
                text = title,
                color = MainTextColor,
                fontFamily = FontFamily(Font(R.font.super_dream)),
                fontSize = 18.sp
            )
            if (icon != null) Image(painter = painterResource(id = icon), contentDescription = null)
        }


        androidx.compose.material3.Text(text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MainTextColor,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.oh_my_notes))
                )
            ) {
                append(descMain)
            }
            append(" ")
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Medium,
                    color = DescriptorTextColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.oh_my_notes))
                )
            ) {
                append(desc)
            }

        })


    }
}

/* SpeedometerScreen */
val INDICATOR_LENGTH = 14.dp
val MAJOR_INDICATOR_LENGTH = 18.dp
val INDICATOR_INITIAL_OFFSET = 6.dp

@SuppressLint("SuspiciousIndentation")
@Composable
fun SpeedometerScreen(
    @FloatRange(from = 0.0, to = 240.0) currentSpeed: Float,
    scoreType: String,
    modifier: Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val indicatorColor = Color1

    Canvas(modifier = modifier) {


        drawArc(
            color = Color.Red,
            startAngle = 30f,
            sweepAngle = -240f,
            useCenter = false,
            style = Stroke(width = 2.0.dp.toPx())
        )

        for (angle in 300 downTo 60 step 2) {
            val speed = 300 - angle

            val startOffset =
                pointOnCircle(
                    thetaInDegrees = angle.toDouble(),
                    radius = size.height / 2 - INDICATOR_INITIAL_OFFSET.toPx(),
                    cX = center.x,
                    cY = center.y
                )

            if (speed % 10 == 0) {
                val markerOffset = pointOnCircle(
                    thetaInDegrees = angle.toDouble(),
                    radius = size.height / 2 - MAJOR_INDICATOR_LENGTH.toPx(),
                    cX = center.x,
                    cY = center.y
                )
                speedMarker(startOffset, markerOffset, SolidColor(indicatorColor), 4.dp.toPx())
            } else if (speed % 5 == 0) {
                val endOffset = pointOnCircle(
                    thetaInDegrees = angle.toDouble(),
                    radius = size.height / 2 - INDICATOR_LENGTH.toPx(),
                    cX = center.x,
                    cY = center.y
                )
                speedMarker(startOffset, endOffset, SolidColor(Color2), 2.dp.toPx())
            } else {
                val endOffset = pointOnCircle(
                    thetaInDegrees = angle.toDouble(),
                    radius = size.height / 2 - INDICATOR_LENGTH.toPx(),
                    cX = center.x,
                    cY = center.y
                )
                speedMarker(startOffset, endOffset, SolidColor(Color3), 1.dp.toPx())
            }
        }

        if (currentSpeed < 240) {
            speedSmallIndicator(speedAngle = 300 - currentSpeed)

        } else {
            speedSmallIndicator(speedAngle = 60f)
        }

        speed(currentSpeed, textMeasurer, units = scoreType)
    }


}

private fun DrawScope.speed(
    speed: Float,
    textMeasurer: TextMeasurer,
    units: String
) {

    val speedText = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = Color.Yellow,
                letterSpacing = 1.sp,
                fontFamily = FontFamily(Font(R.font.ameston_sanf)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        ) {
            append("$speed $units/s")
        }


    }
    val textLayoutResult = textMeasurer.measure(
        text = speedText,
        style = TextStyle.Default.copy(
            textAlign = TextAlign.Center,
        )
    )

    drawContext.canvas.save()
    drawContext.canvas.translate(
        center.x - 60,
        center.y
    )
    drawText(textLayoutResult)

    drawContext.canvas.restore()
}


private fun DrawScope.speedMarker(
    startPoint: Offset,
    endPoint: Offset,
    brush: Brush,
    strokeWidth: Float,
) {
    drawLine(brush = brush, start = startPoint, end = endPoint, strokeWidth = strokeWidth)
}


private fun DrawScope.speedSmallIndicator(
    speedAngle: Float
) {
    val startOffset = pointOnCircle(
        thetaInDegrees = speedAngle.toDouble(),
        radius = (size.height / 2 - INDICATOR_LENGTH.toPx()),
        cX = center.x,
        cY = center.y
    )

    val endOffset = pointOnCircle(
        thetaInDegrees = speedAngle.toDouble(),
        radius = (size.height / 2 - INDICATOR_LENGTH.toPx()) - 30.dp.toPx(),
        cX = center.x,
        cY = center.y
    )

    drawLine(
        color = Color.Yellow,
        start = startOffset,
        end = endOffset,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
}


private fun pointOnCircle(
    thetaInDegrees: Double,
    radius: Float,
    cX: Float,
    cY: Float,
): Offset {
    val x = cX + (radius * sin(Math.toRadians(thetaInDegrees)).toFloat())
    val y = cY + (radius * cos(Math.toRadians(thetaInDegrees)).toFloat())

    return Offset(x, y)
}
