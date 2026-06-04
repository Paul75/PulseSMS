package com.skeler.pulse.ui

import com.skeler.pulse.InboxAccessState
import org.junit.Assert.assertEquals
import org.junit.Test

class RealSmsViewModelTest {

    @Test
    fun `inbox access state reflects denied permissions`() {
        val state = RealInboxState().copy(
            permissionDenied = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true).permissionDenied,
            isDefaultSmsApp = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true).isDefaultSmsApp,
            loading = false,
        )

        assertEquals(true, state.permissionDenied)
        assertEquals(true, state.isDefaultSmsApp)
        assertEquals(false, state.loading)
    }

    @Test
    fun `send should not start while another sms send is active`() {
        assertEquals(false, shouldStartSmsSend(SendState.Sending("hello")))
    }

    @Test
    fun `send can start when no sms send is active`() {
        assertEquals(true, shouldStartSmsSend(SendState.Idle))
    }
}
