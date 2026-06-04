@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

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

@Immutable
internal data class ContactListItem(
    val key: String,
    val name: String,
    val phoneNumber: String,
)

@Immutable
internal data class ContactGroup(
    val label: String,
    val contacts: List<ContactListItem>,
)

// ── Expressive Material 3 contact picker tokens ──

internal object ContactPickerTokens {
    val avatarSize = 48.dp
    val contentHorizontalPadding = 16.dp
    val searchCardCorner = 24.dp
    val inputCardHorizontalPadding = 20.dp
    val inputCardVerticalPadding = 14.dp
    val inputLabelWidth = 48.dp
    val inputRowMinHeight = 48.dp
    val inputRowSpacing = 4.dp
    val simPillHorizontalPadding = 12.dp
    val simPillVerticalPadding = 7.dp
    val simPillContentSpacing = 10.dp
    val simChipSize = 28.dp
    val simChipIconCorner = 6.dp
    val simPillSelectedBorderAlpha = 0.55f
    val contactRowCorner = 16.dp
    val contactDividerAlpha = 0.4f
    val rowHorizontalPadding = 0.dp
    val rowVerticalPadding = 4.dp
    val rowInnerHorizontalPadding = 24.dp
    val rowInnerVerticalPadding = 14.dp
    val rowContentSpacing = 14.dp
    val rowTextSpacing = 1.dp
    val manualRowTextSpacing = 2.dp
    val supportingIconSize = 22.dp
    val defaultIconSize = 20.dp
    val emptyStateIconSize = 48.dp
    val emptyStateVerticalPadding = 40.dp
    val emptyStateSpacing = 12.dp
    val loadingVerticalPadding = 24.dp
    val listBottomPadding = 32.dp
    val searchTopPadding = 10.dp
    val sectionHeaderStartPadding = 52.dp
    val sectionHeaderTopPadding = 16.dp
    val sectionHeaderBottomPadding = 10.dp
    val groupHorizontalPadding = 0.dp
}

@Immutable
internal data class ContactAvatarColors(
    val containerColor: Color,
    val contentColor: Color,
)

internal enum class NewChatListContentType {
    ManualEntry,
    Loading,
    Empty,
    Header,
    Contact,
    Divider,
}

@Composable
internal fun NewChatContactSelectionScreen(
    contactGroups: List<ContactGroup>,
    loading: Boolean,
    searchQuery: String,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    onContactClick: (ContactListItem) -> Unit,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    manualEntry: ContactListItem? = null,
    onManualEntryClick: ((ContactListItem) -> Unit)? = null,
    onSimOptionClick: ((NewChatSimOption) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.new_chat_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = ContactPickerTokens.contentHorizontalPadding),
        ) {
            RecipientInputCard(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                simOptions = simOptions,
                selectedSimKey = selectedSimKey,
                onSimOptionClick = onSimOptionClick,
                modifier = Modifier.padding(top = ContactPickerTokens.searchTopPadding),
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = ContactPickerTokens.listBottomPadding),
            ) {
                // ── Manual entry ──
                if (manualEntry != null && onManualEntryClick != null) {
                    item(
                        key = "new_chat_manual_entry",
                        contentType = NewChatListContentType.ManualEntry,
                    ) {
                        NewChatManualEntryRow(
                            contact = manualEntry,
                            onClick = { onManualEntryClick(manualEntry) },
                        )
                    }
                }

                // ── Contact list ──
                if (loading) {
                    item(
                        key = "new_chat_loading",
                        contentType = NewChatListContentType.Loading,
                    ) {
                        NewChatLoadingState()
                    }
                } else if (contactGroups.isEmpty()) {
                    item(
                        key = "new_chat_empty",
                        contentType = NewChatListContentType.Empty,
                    ) {
                        NewChatEmptyState(query = searchQuery)
                    }
                } else {
                    newChatContactItems(
                        contactGroups = contactGroups,
                        onContactClick = onContactClick,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.newChatContactItems(
    contactGroups: List<ContactGroup>,
    onContactClick: (ContactListItem) -> Unit,
) {
    contactGroups.forEach { group ->
        stickyHeader(
            key = "new_chat_header_${group.label}",
            contentType = NewChatListContentType.Header,
        ) {
            NewChatSectionHeader(label = group.label)
        }

        group.contacts.forEachIndexed { index, contact ->
            item(
                key = contact.lazyContactKey(group.label),
                contentType = NewChatListContentType.Contact,
            ) {
                NewChatContactSurface(
                    contact = contact,
                    shape = contactRowShape(index = index, lastIndex = group.contacts.lastIndex),
                    onClick = { onContactClick(contact) },
                    modifier = Modifier.padding(horizontal = ContactPickerTokens.groupHorizontalPadding),
                )
            }

            if (index != group.contacts.lastIndex) {
                item(
                    key = contact.lazyDividerKey(group.label),
                    contentType = NewChatListContentType.Divider,
                ) {
                    NewChatContactDivider()
                }
            }
        }
    }
}
