package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SophisticatedColorScheme = darkColorScheme(
    primary = SophisticatedOpBg,
    secondary = SophisticatedUtilityBg,
    tertiary = SophisticatedNumberBg,
    background = SophisticatedBg,
    surface = SophisticatedNumberBg,
    onPrimary = SophisticatedOpText,
    onSecondary = SophisticatedUtilityText,
    onTertiary = SophisticatedNumberText,
    onBackground = SophisticatedNumberText,
    onSurface = SophisticatedNumberText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SophisticatedColorScheme,
        typography = Typography,
        content = content
    )
}
