package com.sultonuzdev.netspeed.presentation.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    // Upload's colour; left undefined it fell back to Material's stock pink.
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnError
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    // Upload's colour; left undefined it fell back to Material's stock pink.
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    onError = LightOnError
)

@Immutable
data class NetSpeedCustomColors(
    val backgroundVariant: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val success: Color,
    val warning: Color
)

private val DarkCustomColors = NetSpeedCustomColors(
    backgroundVariant = DarkBackgroundVariant,
    cardBackground = DarkCardBackground,
    cardBorder = DarkCardBorder,
    success = Success,
    warning = Warning
)

val LocalNetSpeedCustomColors = staticCompositionLocalOf { DarkCustomColors }

/** Material You wallpaper-derived colours are only available from Android 12. */
val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Card fills and borders derived from whichever scheme is active, so they track a Material You
 * palette instead of staying fixed to the app's own cyan. Success and warning stay put: they carry
 * meaning, and recolouring them to match a wallpaper would lose it.
 */
private fun customColorsFor(scheme: ColorScheme) = NetSpeedCustomColors(
    backgroundVariant = scheme.surfaceVariant,
    cardBackground = scheme.primary.copy(alpha = 0.10f),
    cardBorder = scheme.primary.copy(alpha = 0.30f),
    success = Success,
    warning = Warning
)

@Composable
fun NetSpeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicScheme(darkTheme)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    // Derived from the active scheme in every case, not just the dynamic one. The hand-picked
    // light values were #F5F5F5 on a #FFFBFE background with an #E0E0E0 border -- a card that
    // was all but invisible against the page behind it.
    val customColors = customColorsFor(colorScheme)

    CompositionLocalProvider(
        LocalNetSpeedCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension property to access custom colors
val MaterialTheme.netSpeedColors: NetSpeedCustomColors
    @Composable
    get() = LocalNetSpeedCustomColors.current

/**
 * Wallpaper-derived colours, isolated behind [RequiresApi] so the version guard is checkable.
 *
 * Lint's NewApi check could not see the guard at the call site in any form tried -- a property
 * getter ([supportsDynamicColor]), a local boolean, a `when` branch, or an inline `if` -- and
 * failed the build each time claiming these Android 12 APIs were reachable on minSdk 26. Moving
 * the calls behind an annotated function states the requirement where lint does read it, which
 * is better than suppressing the warning and losing the check everywhere else.
 */
// Suppressed, not ignored: the only caller guards on SDK_INT >= S, and @RequiresApi above
// states the contract. Lint does not honour @RequiresApi on a @Composable, so the check
// fires anyway; scoping the suppression to this one function keeps NewApi active everywhere else.
@SuppressLint("NewApi")
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun dynamicScheme(darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
