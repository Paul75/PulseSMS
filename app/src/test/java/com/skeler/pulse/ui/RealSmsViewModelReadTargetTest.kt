package com.skeler.pulse.ui

import com.skeler.pulse.sms.SmsThread
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealSmsViewModelReadTargetTest {

    @Test
    fun `matches read target by thread id`() {
        val thread = SmsThread(
            threadId = 42L,
            address = "665",
            snippet = "hello",
            date = 1L,
            messageCount = 2,
            unreadCount = 1,
        )

        assertTrue(thread.matchesReadTarget(ReadConversationTarget(address = "other", threadId = 42L)))
    }

    @Test
    fun `matches read target by address when thread id is unavailable`() {
        val thread = SmsThread(
            threadId = 42L,
            address = "665",
            snippet = "hello",
            date = 1L,
            messageCount = 2,
            unreadCount = 1,
        )

        assertTrue(thread.matchesReadTarget(ReadConversationTarget(address = "665", threadId = null)))
    }

    @Test
    fun `does not match different thread and address`() {
        val thread = SmsThread(
            threadId = 42L,
            address = "665",
            snippet = "hello",
            date = 1L,
            messageCount = 2,
            unreadCount = 1,
        )

        assertFalse(thread.matchesReadTarget(ReadConversationTarget(address = "777", threadId = null)))
    }

    @Test
    fun `should_return_true_when_address_has_no_letters`() {
        assertTrue("+1 (555) 010-9988".isReplyableConversationAddress())
    }

    @Test
    fun `should_return_false_when_address_is_short_code`() {
        assertFalse("555".isReplyableConversationAddress())
    }

    @Test
    fun `should_return_false_when_address_has_letters`() {
        assertFalse("BANK-OTP".isReplyableConversationAddress())
    }
}
