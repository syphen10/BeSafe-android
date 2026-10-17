package com.besafe.app.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun `strips formatting characters`() {
        assertEquals("+923001234567", PhoneNumbers.normalize(" +92 (300) 123-4567 "))
    }

    @Test
    fun `converts international 00 prefix to plus`() {
        assertEquals("+447700900123", PhoneNumbers.normalize("0044 7700 900123"))
    }

    @Test
    fun `drops stray plus signs after the first character`() {
        assertEquals("03001234567", PhoneNumbers.normalize("0300+1234567"))
    }

    @Test
    fun `validates length and characters`() {
        assertTrue(PhoneNumbers.isValid("+923001234567"))
        assertTrue(PhoneNumbers.isValid("03001234567"))
        assertFalse(PhoneNumbers.isValid("12345"))
        assertFalse(PhoneNumbers.isValid(""))
        assertFalse(PhoneNumbers.isValid("+1234567890123456"))
    }
}
