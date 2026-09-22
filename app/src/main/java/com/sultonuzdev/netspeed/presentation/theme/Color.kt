package com.sultonuzdev.netspeed.presentation.theme

import androidx.compose.ui.graphics.Color

/*
 * One hue family for both themes. The dark palette used to be neon cyan (#00FFFF) while light was
 * a Material blue, so the app changed identity with the system theme. Both now derive from the
 * same cobalt; the dark side lifts it for contrast on a near-black ground rather than swapping hue.
 *
 * Primary is download and everything interactive. Tertiary is upload -- a mauve, far enough from
 * cobalt that the two figures read apart at a glance, close enough in value that neither shouts.
 * Every on-colour pair below clears WCAG AA (4.5:1); most clear AAA.
 */

// Dark Theme Colors
val DarkPrimary = Color(0xFF9DB9FF)
val DarkOnPrimary = Color(0xFF002D77)
val DarkPrimaryContainer = Color(0xFF1F4AA6)
val DarkOnPrimaryContainer = Color(0xFFDCE5FF)

val DarkSecondary = Color(0xFFB3C4E3)
val DarkOnSecondary = Color(0xFF1C2E48)
val DarkSecondaryContainer = Color(0xFF334660)
val DarkOnSecondaryContainer = Color(0xFFDDE5F5)

val DarkTertiary = Color(0xFFF0B3D6)
val DarkOnTertiary = Color(0xFF52223F)
val DarkTertiaryContainer = Color(0xFF6E3A5A)
val DarkOnTertiaryContainer = Color(0xFFFFD8EE)

val DarkBackground = Color(0xFF0D1017)
val DarkOnBackground = Color(0xFFE3E6EE)
val DarkSurface = Color(0xFF12161E)
val DarkOnSurface = Color(0xFFE3E6EE)
val DarkSurfaceVariant = Color(0xFF232936)
val DarkOnSurfaceVariant = Color(0xFFB6BDCB)
val DarkOutline = Color(0xFF7F8697)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)

// Light Theme Colors
val LightPrimary = Color(0xFF2457C5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDCE5FF)
val LightOnPrimaryContainer = Color(0xFF0A2A6E)

val LightSecondary = Color(0xFF4B5F80)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFDDE5F5)
val LightOnSecondaryContainer = Color(0xFF0F1F38)

val LightTertiary = Color(0xFF8A4D74)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFD8EE)
val LightOnTertiaryContainer = Color(0xFF3A0A2C)

val LightBackground = Color(0xFFF6F7FB)
val LightOnBackground = Color(0xFF151A22)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF151A22)
val LightSurfaceVariant = Color(0xFFE4E8F1)
val LightOnSurfaceVariant = Color(0xFF464C59)
val LightOutline = Color(0xFF767C8A)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

// App-specific custom colors that work for both themes
val Success = Color(0xFF3FA66B)
val Warning = Color(0xFFE08A1E)

// Dark theme custom colors
val DarkBackgroundVariant = DarkSurfaceVariant
val DarkCardBackground = Color(0x1A9DB9FF)
val DarkCardBorder = Color(0x4D9DB9FF)

// Light theme custom colors
val LightCardBackground = Color(0x1A2457C5)
val LightCardBorder = Color(0x4D2457C5)
