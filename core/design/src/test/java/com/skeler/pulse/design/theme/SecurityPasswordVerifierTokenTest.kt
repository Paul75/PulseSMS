package com.skeler.pulse.design.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPasswordVerifierTokenTest {

    @Test
    fun `verifies matching password token`() {
        val token = securityPasswordVerifierTokenFromPassword("Pulse123")

        assertTrue(verifySecurityPassword("Pulse123", token))
    }

    @Test
    fun `rejects incorrect password for token`() {
        val token = securityPasswordVerifierTokenFromPassword("Pulse123")

        assertFalse(verifySecurityPassword("Pulse124", token))
    }

    @Test
    fun `rejects blank password and malformed token`() {
        val token = securityPasswordVerifierTokenFromPassword("Pulse123")

        assertFalse(verifySecurityPassword("", token))
        assertFalse(verifySecurityPassword("Pulse123", "not-a-token"))
    }
}
