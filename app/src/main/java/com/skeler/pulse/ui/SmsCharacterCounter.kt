package com.skeler.pulse.ui

internal data class SmsCharacterCounter(
    val remainingCharacters: Int,
    val segmentCount: Int,
)

private const val SMS_COUNTER_VISIBLE_THRESHOLD = 100
private const val GSM_SINGLE_SEGMENT_LIMIT = 160
private const val GSM_MULTI_SEGMENT_LIMIT = 153
private const val UCS2_SINGLE_SEGMENT_LIMIT = 70
private const val UCS2_MULTI_SEGMENT_LIMIT = 67

internal fun smsCharacterCounterOrNull(message: String): SmsCharacterCounter? {
    val characterCount = message.length
    if (characterCount <= SMS_COUNTER_VISIBLE_THRESHOLD) return null

    val multipartLimit = if (message.isGsm7()) GSM_MULTI_SEGMENT_LIMIT else UCS2_MULTI_SEGMENT_LIMIT
    val singleSegmentLimit = if (message.isGsm7()) GSM_SINGLE_SEGMENT_LIMIT else UCS2_SINGLE_SEGMENT_LIMIT
    val segmentLimit = if (characterCount <= singleSegmentLimit) singleSegmentLimit else multipartLimit
    val segmentCount = ((characterCount + segmentLimit - 1) / segmentLimit).coerceAtLeast(1)
    val remainingCharacters = segmentCount * segmentLimit - characterCount

    return SmsCharacterCounter(
        remainingCharacters = remainingCharacters,
        segmentCount = segmentCount,
    )
}

private fun String.isGsm7(): Boolean = all { character -> character in GSM_7_BASIC_CHARACTERS }

private val GSM_7_BASIC_CHARACTERS: Set<Char> = (
    "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞ" +
        " !\"#¤%&'()*+,-./0123456789:;<=>?" +
        "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà"
    ).toSet()
