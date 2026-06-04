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



@Composable
internal fun NewChatContactDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ContactPickerTokens.groupHorizontalPadding),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = ContactPickerTokens.contactDividerAlpha),
    )
}

@Composable
internal fun NewChatContactSurface(
    contact: ContactListItem,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        NewChatContactRow(
            contact = contact,
        )
    }
}

@Composable
internal fun NewChatSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Text(
        text = label,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(
                start = ContactPickerTokens.sectionHeaderStartPadding,
                top = ContactPickerTokens.sectionHeaderTopPadding,
                bottom = ContactPickerTokens.sectionHeaderBottomPadding,
                end = ContactPickerTokens.rowInnerHorizontalPadding,
            ),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
        ),
        color = colors.onSurface,
    )
}

@Composable
internal fun NewChatContactRow(
    contact: ContactListItem,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val displayNumber = remember(contact.phoneNumber) { contact.phoneNumber.formatForContactRow() }
    val showNumber = remember(contact.name, displayNumber) {
        !contact.name.isSameDisplayValueAs(displayNumber)
    }
    val avatarColors = remember(contact.name, colors) {
        contact.name.contactAvatarColors(colors)
    }

    NewChatRecipientRow(
        avatar = {
            ContactAvatar(
                contactName = contact.name,
                colors = avatarColors,
            )
        },
        modifier = modifier,
        verticalTextSpacing = ContactPickerTokens.rowTextSpacing,
    ) {
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showNumber) {
            Text(
                text = displayNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Manual entry row ──

@Composable
internal fun NewChatManualEntryRow(
    contact: ContactListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val displayNumber = remember(contact.phoneNumber) { contact.phoneNumber.formatForContactRow() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ContactPickerTokens.rowHorizontalPadding,
                vertical = ContactPickerTokens.rowVerticalPadding,
            ),
        shape = RoundedCornerShape(ContactPickerTokens.contactRowCorner),
        color = colors.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        NewChatRecipientRow(
            avatar = {
                ManualEntryAvatar()
            },
            verticalTextSpacing = ContactPickerTokens.manualRowTextSpacing,
        ) {
            Text(
                text = displayNumber,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.new_chat_manual_entry_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
            )
        }
    }
}
