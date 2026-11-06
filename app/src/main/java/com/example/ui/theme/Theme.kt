package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BlueprintDarkColorScheme = darkColorScheme(
    primary = BlueprintDarkPrimary,
    onPrimary = BlueprintDarkOnPrimary,
    primaryContainer = BlueprintDarkSurfaceVariant,
    onPrimaryContainer = BlueprintDarkPrimary,
    secondary = BlueprintDarkSecondary,
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF3E2C00),
    onSecondaryContainer = BlueprintDarkSecondary,
    tertiary = BlueprintDarkTertiary,
    background = BlueprintDarkBg,
    onBackground = BlueprintDarkText,
    surface = BlueprintDarkSurface,
    onSurface = BlueprintDarkText,
    surfaceVariant = BlueprintDarkSurfaceVariant,
    onSurfaceVariant = BlueprintDarkTextSecondary
)

val BlueprintLightColorScheme = lightColorScheme(
    primary = BlueprintLightPrimary,
    onPrimary = BlueprintLightOnPrimary,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = BlueprintLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = BlueprintLightTertiary,
    background = BlueprintLightBg,
    onBackground = BlueprintLightText,
    surface = BlueprintLightSurface,
    onSurface = BlueprintLightText,
    surfaceVariant = BlueprintLightSurfaceVariant,
    onSurfaceVariant = BlueprintLightTextSecondary
)

@Composable
fun BlueprintTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BlueprintDarkColorScheme else BlueprintLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
