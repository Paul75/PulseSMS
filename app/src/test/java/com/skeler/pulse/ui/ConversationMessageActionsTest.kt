package com.skeler.pulse.ui

import android.provider.Telephony
import com.skeler.pulse.sms.SystemSms
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMessageActionsTest {

    @Test
    fun should_hide_block_action_when_message_is_outbound() {
        assertFalse(shouldShowMessageBlockAction(isOutbound = true))
    }

    @Test
    fun should_show_block_action_when_message_is_not_outbound() {
        assertTrue(shouldShowMessageBlockAction(isOutbound = false))
    }

    @Test
    fun should_show_failed_delivery_status_when_message_type_failed() {
        val failedMessage = smsMessage(type = Telephony.Sms.MESSAGE_TYPE_FAILED)

        assertTrue(failedMessage.hasFailedDelivery())
    }

    @Test
    fun should_not_show_failed_delivery_status_when_message_type_sent() {
        val sentMessage = smsMessage(type = Telephony.Sms.MESSAGE_TYPE_SENT)

        assertFalse(sentMessage.hasFailedDelivery())
    }

    @Test
    fun should_restore_failed_send_body_when_current_draft_is_blank() {
        val nextDraft = draftAfterSendState(
            currentDraft = "",
            sendState = SendState.Failed("Retry me"),
        )

        assertEquals("Retry me", nextDraft)
    }

    @Test
    fun should_keep_existing_draft_when_failed_send_body_is_already_visible() {
        val nextDraft = draftAfterSendState(
            currentDraft = "Retry me",
            sendState = SendState.Failed("Retry me"),
        )

        assertEquals("Retry me", nextDraft)
    }

    @Test
    fun should_clear_draft_when_send_succeeds() {
        val nextDraft = draftAfterSendState(
            currentDraft = "Sent body",
            sendState = SendState.Sent("Sent body"),
        )

        assertEquals("", nextDraft)
    }

    @Test
    fun should_confirm_before_discarding_non_blank_draft() {
        assertTrue(shouldConfirmDiscardDraft("Unsent message"))
    }

    @Test
    fun should_not_confirm_before_leaving_blank_draft() {
        assertFalse(shouldConfirmDiscardDraft("   "))
    }

    @Test
    fun should_return_copyable_code_when_message_contains_business_verification_code() {
        val code = copyableMessageCode("Your business account code is 739201.")

        assertEquals("739201", code)
    }

    @Test
    fun should_not_return_copyable_code_when_message_has_no_verification_code() {
        val code = copyableMessageCode("Your card was charged $1299 at Store.")

        assertNull(code)
    }

    private fun smsMessage(type: Int): SystemSms = SystemSms(
        id = 1L,
        address = "555",
        body = "Body",
        date = 0L,
        type = type,
        read = true,
        threadId = 10L,
    )
}
