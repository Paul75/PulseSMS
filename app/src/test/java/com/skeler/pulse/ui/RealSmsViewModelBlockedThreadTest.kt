package com.skeler.pulse.ui

import com.skeler.pulse.sms.SmsThread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealSmsViewModelBlockedThreadTest {

    @Test
    fun `detects blocked thread by normalized phone address`() {
        val thread = smsThread(address = "+1 (555) 123-4567")

        assertTrue(thread.isBlockedBy(setOf("phone:+15551234567")))
        assertTrue(thread.isBlockedBy(setOf("phone:5551234567")))
    }

    @Test
    fun `detects blocked thread by normalized business sender`() {
        val thread = smsThread(address = "ACME")

        assertTrue(thread.isBlockedBy(setOf("sender:acme")))
        assertTrue(smsThread(address = "ＡＣＭＥ").isBlockedBy(setOf("sender:acme")))
    }

    @Test
    fun `keeps unblocked threads when filtering blocked addresses`() {
        val blocked = smsThread(threadId = 1L, address = "ACME")
        val visible = smsThread(threadId = 2L, address = "665")

        assertEquals(listOf(visible), listOf(blocked, visible).withoutBlockedAddresses(setOf("sender:acme")))
        assertFalse(visible.isBlockedBy(setOf("sender:acme")))
    }
}

private fun smsThread(
    threadId: Long = 1L,
    address: String,
): SmsThread = SmsThread(
    threadId = threadId,
    address = address,
    snippet = "hello",
    date = 1L,
    messageCount = 1,
    unreadCount = 0,
)
