package com.skeler.pulse.ui

private const val MinimumDialableDigitCount = 3

internal fun String.toDialablePhoneNumberOrNull(): String? {
    if (any(Char::isLetter)) return null

    val normalizedAddress = trim()
    val dialableNumber = buildString(normalizedAddress.length) {
        normalizedAddress.forEach { character ->
            when {
                character.isDigit() -> append(character)
                character == '+' && length == 0 -> append(character)
            }
        }
    }
    val digitCount = dialableNumber.count(Char::isDigit)
    return dialableNumber.takeIf { digitCount >= MinimumDialableDigitCount }
}

internal fun shouldShowConversationCallAction(address: String): Boolean =
    address.toDialablePhoneNumberOrNull() != null
