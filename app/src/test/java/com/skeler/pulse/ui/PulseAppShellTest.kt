package com.skeler.pulse.ui

import com.skeler.pulse.InboxAccessState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseAppShellTest {

    @Test
    fun should_keep_read_only_replyability_when_conversation_state_is_reset_during_transition() {
        val resetState = RealConversationState(loading = false)

        val isReplyable = conversationReplyabilityForActiveRoute(
            activeAddress = "ACME",
            conversationState = resetState,
        )

        assertFalse(isReplyable)
    }

    @Test
    fun should_keep_replyable_state_for_numeric_conversation_during_transition() {
        val resetState = RealConversationState(loading = false)

        val isReplyable = conversationReplyabilityForActiveRoute(
            activeAddress = "+15551234567",
            conversationState = resetState,
        )

        assertTrue(isReplyable)
    }

    @Test
    fun should_show_access_gate_when_sms_access_is_lost_outside_lock_screen() {
        val accessState = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true)

        assertTrue(shouldShowInboxAccessGate(accessState = accessState, isLockScreen = false))
    }

    @Test
    fun should_not_show_access_gate_over_lock_screen() {
        val accessState = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true)

        assertFalse(shouldShowInboxAccessGate(accessState = accessState, isLockScreen = true))
    }
}
