package com.besafe.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val HOLD_MS = 2_000
private const val IDLE_CYCLE_MS = 2_800

/** How far the idle rings travel, as a multiple of the button's size. */
private const val RING_REACH = 1.36f
private const val RING_MAX_ALPHA = 0.16f
private val RING_OFFSETS = floatArrayOf(0f, 0.5f)

/**
 * Press-and-hold SOS: a ring fills over [HOLD_MS]; releasing early cancels, which prevents
 * pocket triggers. While idle, soft rings ripple outward — calm motion, no glow.
 */
@Composable
fun SosButton(
    enabled: Boolean,
    onTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnTriggered by rememberUpdatedState(onTriggered)

    val red = MaterialTheme.colorScheme.error
    val fill = if (enabled) red else MaterialTheme.colorScheme.surfaceVariant
    val content = if (enabled) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
    val halo = if (enabled) red else MaterialTheme.colorScheme.outline

    // Idle motion only while SOS is usable: soft rings drift outward and fade (half a cycle apart)
    // and the button breathes slightly. Values are read in draw/layer lambdas, so animating
    // never recomposes.
    val idle = rememberInfiniteTransition(label = "sosIdle")
    val wave = if (enabled) {
        idle.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(IDLE_CYCLE_MS, easing = LinearEasing)),
            label = "wave",
        )
    } else {
        null
    }
    val breathe = if (enabled) {
        idle.animateFloat(
            initialValue = 1f,
            targetValue = 1.025f,
            animationSpec = infiniteRepeatable(
                tween(IDLE_CYCLE_MS / 2, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "breathe",
        )
    } else {
        null
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size * RING_REACH)
            .drawBehind {
                val phase = wave?.value ?: return@drawBehind
                val buttonRadius = this.size.minDimension / 2 / RING_REACH
                for (offset in RING_OFFSETS) {
                    val p = (phase + offset) % 1f
                    drawCircle(
                        color = halo,
                        radius = buttonRadius * (1f + (RING_REACH - 1f) * p),
                        alpha = RING_MAX_ALPHA * (1f - p),
                    )
                }
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val scale = breathe?.value ?: 1f
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(fill)
                .semantics { contentDescription = "SOS. Press and hold for 2 seconds to alert your contacts." }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(onPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val hold = scope.launch {
                            progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnTriggered()
                        }
                        tryAwaitRelease()
                        hold.cancel()
                        scope.launch { progress.animateTo(0f, tween(250)) }
                    })
                }
                .drawWithContent {
                    drawContent()
                    val stroke = 6.dp.toPx()
                    val inset = stroke / 2 + 8.dp.toPx()
                    drawArc(
                        color = content,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    "SOS",
                    color = content,
                    fontSize = (size.value * 0.24f).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Text(
                    "Hold 2 sec",
                    color = content.copy(alpha = 0.85f),
                    style = if (size >= 160.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
