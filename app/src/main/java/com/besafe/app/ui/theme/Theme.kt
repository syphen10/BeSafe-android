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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.besafe.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand: lime is the dominant colour. Screens sit on a lime background with white cards floating on
// it; deep olive carries text and the main buttons (with light-lime labels), bright lime marks
// selection on the white cards, and pink is the accent.
// SOS stays red in every theme: it's the colour people instinctively read as "emergency".
val BrandLime = Color(0xFFA3E635)
val BrandOlive = Color(0xFF1A2E05)
val BrandPink = Color(0xFFDB2777)

private val Light = lightColorScheme(
    primary = BrandOlive,
    onPrimary = Color(0xFFD9F99D),
    primaryContainer = Color(0xFFECFCCB),
    onPrimaryContainer = BrandOlive,
    secondary = Color(0xFF84CC16),
    onSecondary = BrandOlive,
    secondaryContainer = Color(0xFFD9F99D),
    onSecondaryContainer = BrandOlive,
    tertiary = BrandPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF831843),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = BrandLime,
    onBackground = BrandOlive,
    surface = Color.White,
    onSurface = BrandOlive,
    surfaceVariant = Color(0xFFF2FBE3),
    onSurfaceVariant = Color(0xFF4B5A35),
    outline = Color(0xFFB9D88A),
    outlineVariant = Color(0xFFE3F2C8),
)

// Dark keeps the identity: deep olive ground, lime as the bold accent.
private val Dark = darkColorScheme(
    primary = BrandLime,
    onPrimary = BrandOlive,
    primaryContainer = Color(0xFF365314),
    onPrimaryContainer = Color(0xFFECFCCB),
    secondary = Color(0xFF84CC16),
    onSecondary = BrandOlive,
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
    background = Color(0xFF14200A),
    onBackground = Color(0xFFECFCCB),
    surface = Color(0xFF1E2B12),
    onSurface = Color(0xFFECFCCB),
    surfaceVariant = Color(0xFF2A3A19),
    onSurfaceVariant = Color(0xFFBFD49A),
    outline = Color(0xFF4A5E2E),
    outlineVariant = Color(0xFF2E3527),
)

// Nunito (SIL Open Font License, see NUNITO_OFL.txt) as one variable font, so every weight costs
// a single file.
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Nunito = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800), nunito(900))

private val BaseType = Typography()

private fun TextStyle.nunito(weight: FontWeight, size: Int? = null, tracking: Float = 0f) = copy(
    fontFamily = Nunito,
    fontWeight = weight,
    fontSize = size?.sp ?: fontSize,
    letterSpacing = tracking.sp,
)

// Heavy, tight headings and plain, generous body text.
private val AppTypography = Typography(
    displaySmall = BaseType.displaySmall.nunito(FontWeight.Black, 38, -0.8f),
    headlineLarge = BaseType.headlineLarge.nunito(FontWeight.Black, 32, -0.6f),
    headlineMedium = BaseType.headlineMedium.nunito(FontWeight.ExtraBold, 28, -0.5f),
    headlineSmall = BaseType.headlineSmall.nunito(FontWeight.ExtraBold, 23, -0.3f),
    titleLarge = BaseType.titleLarge.nunito(FontWeight.ExtraBold, 21),
    titleMedium = BaseType.titleMedium.nunito(FontWeight.Bold, 17),
    titleSmall = BaseType.titleSmall.nunito(FontWeight.Bold),
    bodyLarge = BaseType.bodyLarge.nunito(FontWeight.Medium),
    bodyMedium = BaseType.bodyMedium.nunito(FontWeight.Medium),
    bodySmall = BaseType.bodySmall.nunito(FontWeight.Medium),
    labelLarge = BaseType.labelLarge.nunito(FontWeight.ExtraBold, 15),
    labelMedium = BaseType.labelMedium.nunito(FontWeight.Bold),
    labelSmall = BaseType.labelSmall.nunito(FontWeight.Bold),
)

// Softly squared rather than pill-shaped: chunkier, less "default".
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
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
