package com.besafe.app.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class AlertMessagesTest {

    private val fix = LocationFix(
        latitude = 24.860966,
        longitude = 67.010791,
        accuracyMeters = 12.4f,
        timeMillis = 1_700_000_000_000L, // 22:13 UTC
    )

    @Test
    fun `sos message includes name, accuracy, time and maps link`() {
        val msg = AlertMessages.alert("Sara", fix, AlertReason.SOS, zone = ZoneOffset.UTC)
        assertEquals(
            "Be Safe alert: Sara sent an SOS and may need help. " +
                "Location (±12m, 22:13): https://maps.google.com/?q=24.860966,67.010791",
            msg,
        )
    }

    @Test
    fun `missed check-in wording differs from sos`() {
        val msg = AlertMessages.alert("Sara", fix, AlertReason.MISSED_CHECK_IN, zone = ZoneOffset.UTC)
        assertTrue(msg.contains("didn't check in safely on time"))
    }

    @Test
    fun `check-in note and battery level are included`() {
        val msg = AlertMessages.alert(
            "Sara",
            fix,
            AlertReason.MISSED_CHECK_IN,
            note = " Taxi ABC-123 to Johar Town ",
            batteryPercent = 12,
            zone = ZoneOffset.UTC,
        )
        assertEquals(
            "Be Safe alert: Sara didn't check in safely on time and may need help. Note: Taxi ABC-123 to Johar Town. " +
                "Location (±12m, 22:13): https://maps.google.com/?q=24.860966,67.010791 Battery 12%.",
            msg,
        )
    }

    @Test
    fun `long notes are cut so one alert can't balloon into many SMS`() {
        val msg = AlertMessages.alert("Sara", null, AlertReason.MISSED_CHECK_IN, note = "x".repeat(500))
        assertTrue(msg.contains("Note: ${"x".repeat(AlertMessages.MAX_NOTE)}."))
        assertTrue(!msg.contains("x".repeat(AlertMessages.MAX_NOTE + 1)))
    }

    @Test
    fun `missing location and blank name still produce a usable message`() {
        val msg = AlertMessages.alert("   ", null, AlertReason.SOS)
        assertEquals("Be Safe alert: Your contact sent an SOS and may need help. Location unavailable.", msg)
    }

    @Test
    fun `safe message uses trimmed name`() {
        assertEquals("Be Safe: Sara is safe now. No need to worry.", AlertMessages.safe(" Sara "))
    }
}
