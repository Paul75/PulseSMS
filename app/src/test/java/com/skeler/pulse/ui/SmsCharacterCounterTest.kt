package com.skeler.pulse.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsCharacterCounterTest {
    @Test
    fun should_hide_counter_when_message_has_one_hundred_characters() {
        assertNull(smsCharacterCounterOrNull("a".repeat(100)))
    }

    @Test
    fun should_count_remaining_gsm_characters_in_single_segment_when_above_threshold() {
        assertEquals(
            SmsCharacterCounter(remainingCharacters = 59, segmentCount = 1),
            smsCharacterCounterOrNull("a".repeat(101)),
        )
    }

    @Test
    fun should_count_multipart_gsm_segments_when_message_exceeds_single_segment_limit() {
        assertEquals(
            SmsCharacterCounter(remainingCharacters = 145, segmentCount = 2),
            smsCharacterCounterOrNull("a".repeat(161)),
        )
    }

    @Test
    fun should_count_ucs2_segments_when_message_contains_non_gsm_character() {
        assertEquals(
            SmsCharacterCounter(remainingCharacters = 33, segmentCount = 2),
            smsCharacterCounterOrNull("漢".repeat(100) + "a"),
        )
    }
}
