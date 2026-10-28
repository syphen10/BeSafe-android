package com.besafe.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent first, then the Mobile Ads SDK. Google's UMP form appears only for users in regions
 * that require it (EEA/UK/Switzerland); the SDK isn't initialised until ads may be requested.
 * Started after the app is on screen so it never slows down launch.
 */
object Ads {

    private val sdkStarted = AtomicBoolean(false)

    private val _sdkReady = MutableStateFlow(false)
    /** True once consent allows ads and the SDK has initialised; banners wait for this. */
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True where the user must be able to change their ad-consent choice later. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun start(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    _privacyOptionsRequired.value = consent.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    if (consent.canRequestAds()) startSdk(activity.applicationContext)
                }
            },
            {
                // Offline or misconfigured: fall back to whatever consent was stored last time.
                if (consent.canRequestAds()) startSdk(activity.applicationContext)
            },
        )
        // Consent from a previous session is already valid; don't wait for the network.
        if (consent.canRequestAds()) startSdk(activity.applicationContext)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
    }

    private fun startSdk(context: Context) {
        if (!sdkStarted.compareAndSet(false, true)) return
        // Google recommends initialising off the main thread; the callback returns on main.
        Thread { MobileAds.initialize(context) { _sdkReady.value = true } }.start()
    }
}
