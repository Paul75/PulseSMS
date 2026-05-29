package com.skeler.pulse.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemSmsReaderTest {

    @Test
    fun should_calculate_callback_request_code_without_int_overflow() {
        val token = "overflow-token"
        val partIndex = 42
        val expectedRequestCode = Math.floorMod(
            token.hashCode().toLong() * CALLBACK_REQUEST_CODE_MULTIPLIER + partIndex,
            CALLBACK_REQUEST_CODE_MODULUS,
        ).toInt()

        val requestCode = callbackRequestCode(token, partIndex)

        assertEquals(expectedRequestCode, requestCode)
        assertTrue(requestCode >= 0)
    }
}
