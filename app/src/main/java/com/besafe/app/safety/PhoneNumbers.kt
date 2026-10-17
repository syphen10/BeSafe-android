package com.besafe.app.safety

/** Minimal phone-number cleanup: SMS only needs digits with an optional leading '+'. */
object PhoneNumbers {

    private val valid = Regex("""^\+?\d{6,15}$""")

    /** Strips spaces, dashes, dots and brackets; converts a leading "00" to "+". */
    fun normalize(raw: String): String {
        val cleaned = raw.trim().filter { it.isDigit() || it == '+' }
        val plusFixed = if (cleaned.startsWith("00")) "+" + cleaned.drop(2) else cleaned
        // A '+' is only meaningful at the start.
        return plusFixed.take(1) + plusFixed.drop(1).replace("+", "")
    }

    fun isValid(normalized: String): Boolean = valid.matches(normalized)
}
