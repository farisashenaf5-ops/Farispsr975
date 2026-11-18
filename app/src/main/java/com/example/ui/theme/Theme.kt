package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PsrHardwareColorScheme =
  darkColorScheme(
    primary = PsrColors.LcdSelectedTop,
    onPrimary = PsrColors.LabelWhite,
    secondary = PsrColors.LedGreen,
    onSecondary = PsrColors.ChassisDark,
    tertiary = PsrColors.LedAmber,
    background = PsrColors.ChassisDark,
    onBackground = PsrColors.LabelWhite,
    surface = PsrColors.ChassisMid,
    onSurface = PsrColors.LabelWhite,
    surfaceVariant = PsrColors.ChassisLight,
    onSurfaceVariant = PsrColors.LabelMuted,
    outline = PsrColors.PanelBorder,
  )

@Composable
fun PsrS975Theme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = PsrHardwareColorScheme,
    typography = Typography,
    content = content,
  )
}
