package com.besafe.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.besafe.app.R
import com.besafe.app.ui.theme.BrandNavy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Picks up exactly where the system splash leaves off (same navy, same logo at the same 240dp
 * size and position), gives the heart one soft beat, reveals the name, then fades into the app.
 * Kept to ~1.3s and tap-to-skip — this is a safety app, SOS must never be far away.
 */
@Composable
fun IntroOverlay(onFinished: () -> Unit) {
    val currentOnFinished = rememberUpdatedState(onFinished)
    val logoScale = remember { Animatable(1f) }
    val logoLift = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textRise = remember { Animatable(12f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                logoScale.animateTo(1.07f, tween(180, easing = FastOutSlowInEasing))
                logoScale.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
            }
            launch {
                delay(200)
                logoLift.animateTo(-36f, tween(420, easing = FastOutSlowInEasing))
            }
            launch {
                delay(300)
                launch { textAlpha.animateTo(1f, tween(380)) }
                textRise.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
            }
        }
        delay(350)
        overlayAlpha.animateTo(0f, tween(300))
        currentOnFinished.value()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(BrandNavy)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                currentOnFinished.value()
            },
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    translationY = logoLift.value * density
                },
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = 96.dp)
                .graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textRise.value * density
                },
        ) {
            Text("Be Safe", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                "Someone always has your back",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp,
            )
        }
    }
}
