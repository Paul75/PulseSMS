package com.skeler.pulse.ui
import android.content.res.Configuration
import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.contact.normalizeAddressForDisplay
import com.skeler.pulse.design.theme.SerafinaAppTheme
import com.skeler.pulse.design.theme.SerafinaPalette
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeState


internal fun List<NewChatRecipient>.toContactGroups(): List<ContactGroup> =
    groupBy { recipient ->
        recipient.sortLabel.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
    }.toSortedMap().map { (label, recipients) ->
        ContactGroup(
            label = label,
            contacts = recipients.map { recipient ->
                ContactListItem(
                    key = recipient.key,
                    name = recipient.displayName,
                    phoneNumber = recipient.address,
                )
            },
        )
    }

internal fun NewChatSimOption.slotIndicator(): String {
    val slotDigits = slotLabel.filter(Char::isDigit)
    return slotDigits.ifBlank { subscriptionId?.toString().orEmpty() }.ifBlank { "1" }
}

internal fun NewChatSimOption.composerSimBadgeLabel(): String =
    "SIM ${slotIndicator()}"

internal fun List<NewChatSimOption>.nextSimOption(selectedSimKey: String?): NewChatSimOption? {
    if (isEmpty()) return null
    val selectedIndex = indexOfFirst { option -> option.key == selectedSimKey }
    if (selectedIndex == -1) return first()
    return get((selectedIndex + 1) % size)
}

internal fun NewChatSimOption.subscriptionIndicator(): String =
    subscriptionId?.toString().orEmpty().ifBlank { slotIndicator() }

internal fun ContactListItem.lazyContactKey(groupLabel: String): String =
    "new_chat_contact_${groupLabel}_$key"

internal fun ContactListItem.lazyDividerKey(groupLabel: String): String =
    "new_chat_divider_${groupLabel}_$key"

internal fun contactRowShape(index: Int, lastIndex: Int): RoundedCornerShape {
    val topCorner = if (index == 0) ContactPickerTokens.contactRowCorner else 0.dp
    val bottomCorner = if (index == lastIndex) ContactPickerTokens.contactRowCorner else 0.dp
    return RoundedCornerShape(
        topStart = topCorner,
        topEnd = topCorner,
        bottomStart = bottomCorner,
        bottomEnd = bottomCorner,
    )
}

internal fun mockContactGroups(): List<ContactGroup> {
    val recipients = listOf(
        NewChatRecipient(
            key = "aarif",
            displayName = "Aarif Khadija",
            address = "+212603257455",
            sortLabel = "Aarif Khadija",
        ),
        NewChatRecipient(
            key = "achraf",
            displayName = "Achraf Boksing",
            address = "+212672206047",
            sortLabel = "Achraf Boksing",
        ),
        NewChatRecipient(
            key = "adnan",
            displayName = "Adnan",
            address = "+212673784670",
            sortLabel = "Adnan",
        ),
        NewChatRecipient(
            key = "ayae",
            displayName = "Ayae",
            address = "+212694215301",
            sortLabel = "Ayae",
        ),
        NewChatRecipient(
            key = "baba",
            displayName = "Baba",
            address = "+212612983744",
            sortLabel = "Baba",
        ),
        NewChatRecipient(
            key = "cheymae",
            displayName = "Cheymae",
            address = "+212661420188",
            sortLabel = "Cheymae",
        ),
    )

    return recipients.toContactGroups()
}

internal fun ColorScheme.contactAvatarColorPalette(): List<ContactAvatarColors> =
    listOf(
        ContactAvatarColors(primaryContainer, onPrimaryContainer),
        ContactAvatarColors(secondaryContainer, onSecondaryContainer),
        ContactAvatarColors(tertiaryContainer, onTertiaryContainer),
        ContactAvatarColors(primary, onPrimary),
        ContactAvatarColors(secondary, onSecondary),
        ContactAvatarColors(tertiary, onTertiary),
    )

internal fun String.contactAvatarColors(colors: ColorScheme): ContactAvatarColors {
    val avatarPalette = colors.contactAvatarColorPalette()
    return avatarPalette[trim().ifBlank { "#" }.hashCode().mod(avatarPalette.size)]
}

internal fun String.contactAvatarInitials(): String? =
    toAvatarInitials()
        .takeIf { initials -> initials.any(Char::isLetter) }

internal fun String.isSameDisplayValueAs(other: String): Boolean {
    return normalizeDisplayValue() == other.normalizeDisplayValue()
}

internal fun String.normalizeDisplayValue(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.any { it.isLetter() } && !trimmed.contains('@')) {
        return trimmed.lowercase()
    }
    return trimmed.filter { it.isDigit() || it == '+' || it == '@' || it == '.' }.lowercase()
}

internal fun String.formatForContactRow(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.any(Char::isLetter) || trimmed.contains('@')) return trimmed

    val normalized = normalizeAddressForDisplay()
    if (normalized.isBlank()) return trimmed

    return PhoneNumberUtils.formatNumber(normalized, java.util.Locale.getDefault().country)
        ?.takeIf(String::isNotBlank)
        ?: normalized
}
