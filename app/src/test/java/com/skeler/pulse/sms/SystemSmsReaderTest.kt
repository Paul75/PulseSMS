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

    @Test
    fun should_query_messages_by_thread_id_when_provider_thread_is_available() {
        val criteria = messageReadCriteria(threadId = 42L, address = "+15550100")

        assertEquals("thread_id = ?", criteria.selection)
        assertEquals(listOf("42"), criteria.selectionArgs.toList())
        assertEquals(false, criteria.shouldFilterByAddress)
    }

    @Test
    fun should_query_messages_by_exact_address_before_full_scan_when_thread_id_is_unavailable() {
        val criteria = messageReadCriteria(threadId = null, address = "+15550100")

        assertEquals("address = ?", criteria.selection)
        assertEquals(listOf("+15550100"), criteria.selectionArgs.toList())
        assertEquals(true, criteria.shouldFilterByAddress)
    }
}
