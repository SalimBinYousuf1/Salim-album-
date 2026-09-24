package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.SalimTheme

private val SalimLightColorScheme = lightColorScheme(
    primary = SalimBlue,
    onPrimary = Color.White,
    primaryContainer = SalimOffWhite,
    onPrimaryContainer = SalimCharcoal,
    secondary = SalimCharcoal,
    onSecondary = Color.White,
    secondaryContainer = SalimOffWhite,
    onSecondaryContainer = SalimCharcoal,
    background = SalimWhite,
    onBackground = SalimCharcoal,
    surface = SalimWhite,
    onSurface = SalimCharcoal,
    surfaceVariant = SalimOffWhite,
    onSurfaceVariant = SalimSecondaryTextLight,
    outline = SalimBorderLight,
    error = SalimRed,
    onError = Color.White
)

private val SalimDarkColorScheme = darkColorScheme(
    primary = SalimBlue,
    onPrimary = Color.White,
    primaryContainer = SalimDarkElevated,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = SalimOledBlack,
    secondaryContainer = SalimDarkElevated,
    onSecondaryContainer = Color.White,
    background = SalimOledBlack,
    onBackground = SalimTextDark,
    surface = SalimDarkSurface,
    onSurface = SalimTextDark,
    surfaceVariant = SalimDarkElevated,
    onSurfaceVariant = SalimSecondaryTextDark,
    outline = SalimBorderDark,
    error = SalimRed,
    onError = Color.White
)

private val SalimAsglColorScheme = lightColorScheme(
    primary = SalimBlue,
    onPrimary = Color.White,
    primaryContainer = SalimAsglSurface,
    onPrimaryContainer = SalimCharcoal,
    secondary = SalimCharcoal,
    onSecondary = Color.White,
    secondaryContainer = SalimAsglSurface,
    onSecondaryContainer = SalimCharcoal,
    background = SalimAsglBg,
    onBackground = SalimCharcoal,
    surface = SalimAsglCard,
    onSurface = SalimCharcoal,
    surfaceVariant = SalimAsglSurface,
    onSurfaceVariant = SalimSecondaryTextLight,
    outline = SalimBorderLight,
    error = SalimRed,
    onError = Color.White
)

@Composable
fun SalimTheme(
    selectedTheme: SalimTheme = SalimTheme.LIGHT,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (selectedTheme) {
        SalimTheme.LIGHT -> SalimLightColorScheme
        SalimTheme.DARK -> SalimDarkColorScheme
        SalimTheme.SYSTEM -> if (isSystemDark) SalimDarkColorScheme else SalimLightColorScheme
        SalimTheme.ASGL -> SalimAsglColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
