package com.besafe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF1E3A8A)
private val SosRed = Color(0xFFD32F2F)
private val SafeGreen = Color(0xFF2E7D32)

private val Light = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = SafeGreen,
    onSecondary = Color.White,
    error = SosRed,
    onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9DB4FF),
    onPrimary = Color(0xFF0A1A4F),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF0B2E0F),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0000),
)

@Composable
fun BeSafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
