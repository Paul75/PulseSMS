package com.skeler.pulse.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpClipboardAutoCopyTest {
    @Test
    fun should_return_code_when_auto_copy_is_enabled_and_body_has_otp_context() {
        val code = OtpClipboardAutoCopy.codeToCopy(
            body = "Pulse verification code: 493827. Do not share it.",
            isEnabled = true,
        )

        assertEquals("493827", code)
    }

    @Test
    fun should_not_return_code_when_auto_copy_is_disabled() {
        val code = OtpClipboardAutoCopy.codeToCopy(
            body = "Pulse verification code: 493827. Do not share it.",
            isEnabled = false,
        )

        assertNull(code)
    }

    @Test
    fun should_not_return_amount_when_body_has_no_otp_context() {
        val code = OtpClipboardAutoCopy.codeToCopy(
            body = "Your card was charged $1299 at Store.",
            isEnabled = true,
        )

        assertNull(code)
    }
}
