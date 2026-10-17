package com.besafe.app.safety

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** A resolved device position. Plain data so message building stays unit-testable. */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timeMillis: Long,
)

/** Builds the SMS texts. Kept short: anything over 160 chars is split into multiple SMS. */
object AlertMessages {

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun alert(
        userName: String,
        fix: LocationFix?,
        reason: AlertReason,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val who = displayName(userName)
        val what = when (reason) {
            AlertReason.SOS -> "$who sent an SOS and may need help."
            AlertReason.MISSED_CHECK_IN -> "$who didn't check in safely on time and may need help."
        }
        val where = fix?.let {
            val time = timeFormat.format(Instant.ofEpochMilli(it.timeMillis).atZone(zone))
            "Location (±${it.accuracyMeters.roundToInt()}m, $time): ${mapsLink(it)}"
        } ?: "Location unavailable."
        return "Be Safe alert: $what $where"
    }

    fun safe(userName: String): String = "Be Safe: ${displayName(userName)} is safe now. No need to worry."

    fun mapsLink(fix: LocationFix): String =
        String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", fix.latitude, fix.longitude)

    private fun displayName(userName: String) = userName.trim().ifEmpty { "Your contact" }
}
