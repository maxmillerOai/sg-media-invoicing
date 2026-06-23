package org.example.project.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Communication-agency palette: deep indigo ink with a vivid violet→cyan brand gradient. */
object AgencyPalette {
    val Ink = Color(0xFF12132A)        // deep indigo, primary text / brand
    val InkSoft = Color(0xFF2A2C52)
    val Violet = Color(0xFF6D5DF6)
    val Cyan = Color(0xFF00C2FF)
    val Coral = Color(0xFFFF6B6B)
    val Amber = Color(0xFFFFB020)
    val Mint = Color(0xFF22C55E)
    val Cloud = Color(0xFFF5F7FB)      // light app background
    val CloudDeep = Color(0xFFEAEEF6)
    val Muted = Color(0xFF7A819A)
    val DarkBg = Color(0xFF0B0E1A)
    val DarkSurface = Color(0xFF151A2E)
}

/** Reusable brushes for headers, KPI cards and accents. */
object Gradients {
    val brand = Brush.linearGradient(listOf(AgencyPalette.Violet, AgencyPalette.Cyan))
    val brandVertical = Brush.verticalGradient(listOf(AgencyPalette.Violet, AgencyPalette.Cyan))
    val ink = Brush.linearGradient(listOf(AgencyPalette.Ink, AgencyPalette.InkSoft))
    val sunset = Brush.linearGradient(listOf(AgencyPalette.Coral, AgencyPalette.Amber))
    val mint = Brush.linearGradient(listOf(Color(0xFF11998E), AgencyPalette.Mint))
    val ocean = Brush.linearGradient(listOf(Color(0xFF2563EB), AgencyPalette.Cyan))
}

private val LightColors = lightColorScheme(
    primary = AgencyPalette.Ink,
    onPrimary = Color.White,
    primaryContainer = AgencyPalette.Violet,
    onPrimaryContainer = Color.White,
    secondary = AgencyPalette.Violet,
    onSecondary = Color.White,
    tertiary = AgencyPalette.Cyan,
    onTertiary = Color(0xFF04111B),
    background = AgencyPalette.Cloud,
    onBackground = AgencyPalette.Ink,
    surface = Color.White,
    onSurface = AgencyPalette.Ink,
    surfaceVariant = AgencyPalette.CloudDeep,
    onSurfaceVariant = AgencyPalette.Muted,
    outlineVariant = Color(0xFFE2E7F1),
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = AgencyPalette.Ink,
    primaryContainer = AgencyPalette.Violet,
    onPrimaryContainer = Color.White,
    secondary = AgencyPalette.Cyan,
    onSecondary = Color(0xFF04111B),
    tertiary = AgencyPalette.Cyan,
    background = AgencyPalette.DarkBg,
    onBackground = Color(0xFFE6EDF3),
    surface = AgencyPalette.DarkSurface,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1E2540),
    onSurfaceVariant = Color(0xFF9AA3C0),
    outlineVariant = Color(0xFF2A3350),
)

private val AgencyTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

@Composable
fun AgencyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AgencyTypography,
        content = content,
    )
}
