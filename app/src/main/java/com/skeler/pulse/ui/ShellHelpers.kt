package com.skeler.pulse.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import com.skeler.pulse.R
import com.skeler.pulse.security.auth.BiometricAvailability
import com.skeler.pulse.sms.SystemSms
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun Instant.toInboxTimestamp(): String = when {
    atZone(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() ->
        INBOX_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))
    else -> INBOX_DATE_FORMATTER.format(atZone(ZoneId.systemDefault()))
}

internal fun BiometricAvailability.lockScreenMessage(resources: Resources): String = when (this) {
    BiometricAvailability.Available -> resources.getString(R.string.biometric_tap_to_auth)
    BiometricAvailability.NoHardware -> resources.getString(R.string.biometric_no_hardware)
    BiometricAvailability.HardwareUnavailable -> resources.getString(R.string.biometric_hw_unavailable)
    BiometricAvailability.NoneEnrolled -> resources.getString(R.string.biometric_none_enrolled)
    BiometricAvailability.SecurityUpdateRequired -> resources.getString(R.string.biometric_security_update)
}

internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

internal sealed interface ConversationTimelineItem {
    val key: String
    val contentType: String

    data class DayDivider(
        override val key: String,
        val label: String,
    ) : ConversationTimelineItem {
        override val contentType: String = "conversation_day_divider"
    }

    data class UnreadDivider(
        val label: String,
        override val key: String,
    ) : ConversationTimelineItem {
        override val contentType: String = "conversation_unread_divider"
    }

    data class Message(
        val message: SystemSms,
    ) : ConversationTimelineItem {
        override val key: String = "conversation_message_${message.id}"
        override val contentType: String = "conversation_message"
    }
}

internal fun List<SystemSms>.toConversationTimeline(
    unreadMessagesFormatter: (Int) -> String,
    todayLabel: String,
    yesterdayLabel: String,
): List<ConversationTimelineItem> {
    if (isEmpty()) return emptyList()

    val items = ArrayList<ConversationTimelineItem>(size + 4)
    var lastDate: LocalDate? = null
    val unreadMessages = count { it.isInbound && !it.read }
    val firstUnreadMessageId = firstOrNull { it.isInbound && !it.read }?.id

    for (message in this) {
        val localDate = message.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        if (localDate != lastDate) {
            items += ConversationTimelineItem.DayDivider(
                key = "conversation_day_${localDate}",
                label = localDate.toConversationDayLabel(todayLabel, yesterdayLabel),
            )
            lastDate = localDate
        }
        if (message.id == firstUnreadMessageId) {
            items += ConversationTimelineItem.UnreadDivider(
                key = "conversation_unread_${message.id}",
                label = unreadMessagesFormatter(unreadMessages),
            )
        }
        items += ConversationTimelineItem.Message(message)
    }

    return items
}

internal fun LocalDate.toConversationDayLabel(
    todayLabel: String,
    yesterdayLabel: String,
    today: LocalDate = LocalDate.now(),
): String = when (this) {
    today -> todayLabel
    today.minusDays(1) -> yesterdayLabel
    else -> CONVERSATION_DAY_FORMATTER.format(this)
}

internal fun Instant.toConversationTime(): String =
    BUBBLE_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))

internal fun String.toAvatarInitials(): String =
    trim()
        .split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { take(2).uppercase().ifBlank { "#" } }

internal fun String.isDirectAddressCandidate(): Boolean {
    if (isBlank()) return false
    return any(Char::isDigit) || contains('@') || any { it == '+' }
}

internal fun String.toConversationCategoryLabel(
    businessLabel: String,
    personalLabel: String,
): String =
    if (any(Char::isLetter)) businessLabel else personalLabel

internal fun String.toConversationMetaLabel(
    categoryLabel: String,
    messagesLabel: String,
    unreadLabel: String?,
    keptLabel: String?,
): String {
    val parts = buildList {
        add(categoryLabel)
        add(messagesLabel)
        if (unreadLabel != null) add(unreadLabel)
        if (keptLabel != null) add(keptLabel)
    }
    return parts.joinToString(" · ")
}

internal val INBOX_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val INBOX_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
internal val CONVERSATION_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
internal val BUBBLE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
