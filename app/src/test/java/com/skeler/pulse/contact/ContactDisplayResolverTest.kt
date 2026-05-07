package com.skeler.pulse.contact

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDisplayResolverTest {

    @Test
    fun `normalizes formatted phone numbers to the same key`() {
        assertEquals("+15551234567", "+1 (555) 123-4567".normalizeAddressForDisplay())
        assertEquals("5551234567", "555 123 4567".normalizeAddressForDisplay())
    }

    @Test
    fun `normalizes business senders case insensitively`() {
        assertEquals("amazon", "AMAZON".normalizeAddressForDisplay())
        assertEquals("uber", "Uber".normalizeAddressForDisplay())
    }

    @Test
    fun `canonicalizes blocked phone senders and international prefix variants`() {
        assertEquals("phone:+15551234567", "+1 (555) 123-4567".toBlockedSenderKeyOrNull())
        assertEquals("phone:+15551234567", "001 555 123 4567".toBlockedSenderKeyOrNull())
    }

    @Test
    fun `matches blocked us phone senders across local and e164 formats`() {
        assertEquals(true, "+1 (555) 123-4567".matchesBlockedSenderKey("(555) 123-4567"))
    }

    @Test
    fun `canonicalizes blocked business senders across case width and whitespace`() {
        assertEquals("sender:acme", "ACME".toBlockedSenderKeyOrNull())
        assertEquals("sender:acme", "acme".toBlockedSenderKeyOrNull())
        assertEquals("sender:acme", "ＡＣＭＥ".toBlockedSenderKeyOrNull())
        assertEquals("sender:acme", " Ac Me ".toBlockedSenderKeyOrNull())
    }

    @Test
    fun `strips bidi control characters from blocked sender keys`() {
        assertEquals("sender:acme", "A\u202ECME".toBlockedSenderKeyOrNull())
    }

    @Test
    fun `deduplicates equivalent blocked sender keys`() {
        assertEquals(
            setOf("sender:acme", "phone:5551234567"),
            listOf("ACME", "acme", "ＡＣＭＥ", "(555) 123-4567").toCanonicalBlockedSenderKeys(),
        )
    }

    @Test
    fun `returns blank for blank input`() {
        assertEquals("", "   ".normalizeAddressForDisplay())
    }
}
