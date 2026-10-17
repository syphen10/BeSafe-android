package com.besafe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand navy — also the splash/intro background, identical in light and dark. */
val BrandNavy = Color(0xFF1E3A8A)
val BrandPink = Color(0xFFFFC9D6)

private val Light = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FA),
    onPrimaryContainer = Color(0xFF0E1F55),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9EFDA),
    onSecondaryContainer = Color(0xFF0B3A10),
    tertiary = Color(0xFFB26A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCEBD0),
    onTertiaryContainer = Color(0xFF4A2C00),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFBE3E3),
    onErrorContainer = Color(0xFF5C0B0B),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF151A2D),
    surface = Color.White,
    onSurface = Color(0xFF151A2D),
    surfaceVariant = Color(0xFFEEF0F6),
    onSurfaceVariant = Color(0xFF5A6275),
    outline = Color(0xFFC5CAD8),
    outlineVariant = Color(0xFFE4E7EF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA9BCFF),
    onPrimary = Color(0xFF0A1A4F),
    primaryContainer = Color(0xFF22337A),
    onPrimaryContainer = Color(0xFFDCE3FA),
    secondary = Color(0xFF8BD48F),
    onSecondary = Color(0xFF0B2E0F),
    secondaryContainer = Color(0xFF1D4A21),
    onSecondaryContainer = Color(0xFFD9EFDA),
    tertiary = Color(0xFFF2B766),
    onTertiary = Color(0xFF3F2500),
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = Color(0xFFFCEBD0),
    error = Color(0xFFE5534B),
    onError = Color.White,
    errorContainer = Color(0xFF5C1414),
    onErrorContainer = Color(0xFFFBE3E3),
    background = Color(0xFF0E1322),
    onBackground = Color(0xFFE6E9F2),
    surface = Color(0xFF161C2E),
    onSurface = Color(0xFFE6E9F2),
    surfaceVariant = Color(0xFF212A40),
    onSurfaceVariant = Color(0xFFA9B1C6),
    outline = Color(0xFF3A4462),
    outlineVariant = Color(0xFF2A3350),
)

private val BaseType = Typography()
private val AppTypography = Typography(
    headlineMedium = BaseType.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = BaseType.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseType.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseType.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseType.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun BeSafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
