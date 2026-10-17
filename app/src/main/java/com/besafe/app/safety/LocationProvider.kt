package com.besafe.app.safety

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Gets the best location it can within a bounded time, using only the platform LocationManager
 * (works on devices without Google Play Services).
 *
 * Order: fresh GPS fix → fresh network fix → most recent cached fix from any provider.
 */
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)

    suspend fun currentLocation(): LocationFix? {
        if (!hasPermission()) return null
        val fresh = fresh(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_MS)
            ?: fresh(LocationManager.NETWORK_PROVIDER, NETWORK_TIMEOUT_MS)
        return (fresh ?: lastKnown())?.toFix()
    }

    fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission") // checked in currentLocation()
    private suspend fun fresh(provider: String, timeoutMs: Long): Location? {
        if (!manager.isProviderEnabled(provider)) return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(appContext),
                ) { location -> if (cont.isActive) cont.resume(location) }
            }
        }
    }

    @SuppressLint("MissingPermission") // checked in currentLocation()
    private fun lastKnown(): Location? =
        manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

    private fun Location.toFix() = LocationFix(latitude, longitude, accuracy, time)

    private companion object {
        const val GPS_TIMEOUT_MS = 12_000L
        const val NETWORK_TIMEOUT_MS = 6_000L
    }
}
