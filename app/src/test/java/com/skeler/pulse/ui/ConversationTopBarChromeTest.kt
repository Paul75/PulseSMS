package com.skeler.pulse.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTopBarChromeTest {

    @Test
    fun should_keep_status_bar_chrome_transparent_when_messages_are_unread() {
        assertEquals(Color.Transparent, conversationTopBarChromeContainerColor(hasUnreadMessages = true))
    }

    @Test
    fun should_keep_status_bar_chrome_transparent_when_messages_are_read() {
        assertEquals(Color.Transparent, conversationTopBarChromeContainerColor(hasUnreadMessages = false))
    }
}
