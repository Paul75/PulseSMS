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
internal fun NewChatRecipientRow(
    avatar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    verticalTextSpacing: Dp,
    textContent: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .padding(
                horizontal = ContactPickerTokens.rowInnerHorizontalPadding,
                vertical = ContactPickerTokens.rowInnerVerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(ContactPickerTokens.rowContentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        avatar()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(verticalTextSpacing),
            content = textContent,
        )
    }
}

@Composable
internal fun ContactAvatar(
    contactName: String,
    colors: ContactAvatarColors,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ContactPickerTokens.avatarSize)
            .clip(CircleShape)
            .background(colors.containerColor),
        contentAlignment = Alignment.Center,
    ) {
        val monogram = contactName.contactAvatarInitials()
        if (monogram != null) {
            Text(
                text = monogram,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.contentColor,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(ContactPickerTokens.defaultIconSize),
                tint = colors.contentColor,
            )
        }
    }
}

@Composable
internal fun ManualEntryAvatar(
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(ContactPickerTokens.avatarSize)
            .clip(CircleShape)
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            modifier = Modifier.size(ContactPickerTokens.supportingIconSize),
            tint = colors.onPrimary,
        )
    }
}

// ── Empty state — expressive with icon ──

@Composable
internal fun NewChatEmptyState(
    query: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ContactPickerTokens.rowHorizontalPadding,
                vertical = ContactPickerTokens.emptyStateVerticalPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ContactPickerTokens.emptyStateSpacing),
    ) {
        Icon(
            imageVector = Icons.Rounded.PersonSearch,
            contentDescription = null,
            modifier = Modifier.size(ContactPickerTokens.emptyStateIconSize),
            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.new_chat_empty_no_contacts)
            } else {
                stringResource(R.string.new_chat_empty_no_matches, query)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(R.string.new_chat_empty_manual_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

// ── Helpers ──
