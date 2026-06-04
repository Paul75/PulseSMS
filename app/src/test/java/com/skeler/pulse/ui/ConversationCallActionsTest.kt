package com.skeler.pulse.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationCallActionsTest {

    @Test
    fun should_return_dial_number_when_address_contains_phone_digits() {
        val dialNumber = " +1 (555) 123-4567 ".toDialablePhoneNumberOrNull()

        assertEquals("+15551234567", dialNumber)
    }

    @Test
    fun should_return_null_when_address_is_business_sender_id() {
        val dialNumber = "PULSE".toDialablePhoneNumberOrNull()

        assertNull(dialNumber)
    }

    @Test
    fun should_show_call_action_when_address_is_dialable() {
        assertEquals(true, shouldShowConversationCallAction("+1 (555) 123-4567"))
    }

    @Test
    fun should_hide_call_action_when_address_is_not_dialable() {
        assertEquals(false, shouldShowConversationCallAction("PULSE"))
    }
}
