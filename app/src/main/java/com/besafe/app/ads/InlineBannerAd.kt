package com.besafe.app.ads

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.besafe.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val FIRST_LOAD_DELAY_MS = 5_000L
private const val RETRY_DELAY_MS = 60_000L

/** Clear gap between the last real button on the screen and the ad, against accidental taps. */
private val GAP_ABOVE_AD = 32.dp

/**
 * Banner placed at the END of a scrolling list (Messages, Contacts), never on the Safety screen
 * and never pinned next to the navigation bar, where accidental taps happen. It takes no space
 * until an ad has actually loaded, and is clearly labelled.
 *
 * @param horizontalPadding the list's side padding, so the ad fits the content width exactly.
 */
@Composable
fun InlineBannerAd(horizontalPadding: Dp, modifier: Modifier = Modifier) {
    val sdkReady by Ads.sdkReady.collectAsState()
    if (!sdkReady) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp - (horizontalPadding.value * 2).toInt()
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
    var loaded by remember { mutableStateOf(false) }

    val adView = remember(adSize) {
        val handler = Handler(Looper.getMainLooper())
        AdView(context).apply {
            adUnitId = BuildConfig.BANNER_UNIT_ID
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Only the first load needs a manual retry; once shown, the SDK refreshes itself.
                    if (!loaded) handler.postDelayed({ loadAd(AdRequest.Builder().build()) }, RETRY_DELAY_MS)
                }
            }
            // Ad responses are decoded on the main thread; keep that away from screen changes.
            handler.postDelayed({ loadAd(AdRequest.Builder().build()) }, FIRST_LOAD_DELAY_MS)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    Column(modifier.fillMaxWidth()) {
        if (loaded) {
            Spacer(Modifier.height(GAP_ABOVE_AD))
            Text(
                "Advertisement",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
        }
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (loaded) adSize.height.dp else 0.dp),
        )
    }
}
