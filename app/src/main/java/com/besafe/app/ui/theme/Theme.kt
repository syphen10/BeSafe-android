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

// Brand: lime, white and a tint of pink. In the light theme "primary" is a deep lime, because
// Material draws text buttons, text fields and links in primary and bright lime text is hard to read
// on white. Bright lime is "secondary": a fill colour (the "I'm safe" button, selected chips,
// countdown ring) that always carries dark text.
// SOS stays red in every theme: it's the colour people instinctively read as "emergency".
val BrandLime = Color(0xFF84CC16)
val BrandLimeDeep = Color(0xFF4D7C0F)
val BrandPink = Color(0xFFEC4899)

private val Light = lightColorScheme(
    primary = BrandLimeDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECFCCB),
    onPrimaryContainer = Color(0xFF2B4A08),
    secondary = BrandLime,
    onSecondary = Color(0xFF1A2E05),
    secondaryContainer = Color(0xFFD9F99D),
    onSecondaryContainer = Color(0xFF1A2E05),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF831843),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFFFFBFC),
    onBackground = Color(0xFF1B2014),
    surface = Color.White,
    onSurface = Color(0xFF1B2014),
    surfaceVariant = Color(0xFFF6EFF2),
    onSurfaceVariant = Color(0xFF5F6657),
    outline = Color(0xFFD8CFD3),
    outlineVariant = Color(0xFFEFE6EA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA3E635),
    onPrimary = Color(0xFF1A2E05),
    primaryContainer = Color(0xFF2F4A0C),
    onPrimaryContainer = Color(0xFFECFCCB),
    secondary = Color(0xFFBEF264),
    onSecondary = Color(0xFF1A2E05),
    secondaryContainer = Color(0xFF365314),
    onSecondaryContainer = Color(0xFFECFCCB),
    tertiary = Color(0xFFF9A8D4),
    onTertiary = Color(0xFF500724),
    tertiaryContainer = Color(0xFF6B1A44),
    onTertiaryContainer = Color(0xFFFCE7F3),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5C1414),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF11150D),
    onBackground = Color(0xFFE8EDE0),
    surface = Color(0xFF181E12),
    onSurface = Color(0xFFE8EDE0),
    surfaceVariant = Color(0xFF262C1F),
    onSurfaceVariant = Color(0xFFB5BCA8),
    outline = Color(0xFF424A38),
    outlineVariant = Color(0xFF2E3527),
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
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
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
