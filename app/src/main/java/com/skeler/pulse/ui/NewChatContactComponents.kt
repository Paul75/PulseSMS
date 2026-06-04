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
internal fun NewChatLoadingState(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.new_chat_loading_contacts),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ContactPickerTokens.rowHorizontalPadding,
                vertical = ContactPickerTokens.loadingVerticalPadding,
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ── Expressive recipient input surface — layered To/With rows ──

@Composable
internal fun RecipientInputCard(
    query: String,
    onQueryChange: (String) -> Unit,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    onSimOptionClick: ((NewChatSimOption) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val selectedSim = remember(simOptions, selectedSimKey) {
        simOptions.firstOrNull { option -> option.key == selectedSimKey } ?: simOptions.firstOrNull()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(ContactPickerTokens.searchCardCorner),
        color = colors.surfaceContainerLow,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ContactPickerTokens.inputCardHorizontalPadding,
                    vertical = ContactPickerTokens.inputCardVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(ContactPickerTokens.inputRowSpacing),
        ) {
            RecipientSearchRow(
                query = query,
                onQueryChange = onQueryChange,
            )
            RecipientSimRow(
                simOptions = simOptions,
                selectedSim = selectedSim,
                onSimOptionClick = onSimOptionClick,
            )
        }
    }
}

@Composable
internal fun RecipientSearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier.defaultMinSize(minHeight = ContactPickerTokens.inputRowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.new_chat_recipient_label),
            modifier = Modifier.defaultMinSize(minWidth = ContactPickerTokens.inputLabelWidth),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = colors.onSurface,
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = ContactPickerTokens.inputRowMinHeight),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = colors.onSurface,
                fontWeight = FontWeight.Normal,
            ),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search,
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isBlank()) {
                        Text(
                            text = stringResource(R.string.new_chat_recipient_placeholder),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
internal fun RecipientSimRow(
    simOptions: List<NewChatSimOption>,
    selectedSim: NewChatSimOption?,
    onSimOptionClick: ((NewChatSimOption) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val nextSim = remember(simOptions, selectedSim) {
        simOptions.nextSimOption(selectedSim?.key)
    }

    Row(
        modifier = modifier.defaultMinSize(minHeight = ContactPickerTokens.inputRowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.new_chat_sim_label),
            modifier = Modifier.defaultMinSize(minWidth = ContactPickerTokens.inputLabelWidth),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            color = colors.onSurface,
        )
        if (selectedSim != null && nextSim != null) {
            NewChatSimSelectorPill(
                subscriptionLabel = selectedSim.subscriptionIndicator(),
                carrierLabel = selectedSim.carrierLabel,
                canToggle = simOptions.size > 1,
                enabled = onSimOptionClick != null,
                onClick = { onSimOptionClick?.invoke(nextSim) },
            )
        }
    }
}

@Composable
internal fun NewChatSimSelectorPill(
    subscriptionLabel: String,
    carrierLabel: String,
    canToggle: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (canToggle) colors.onSurface else colors.onSurfaceVariant
    val simIndicatorContainerColor = colors.tertiaryContainer
    val simIndicatorContentColor = colors.onTertiaryContainer

    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize(),
        enabled = enabled,
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = colors.outlineVariant.copy(alpha = ContactPickerTokens.simPillSelectedBorderAlpha),
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = ContactPickerTokens.simPillHorizontalPadding,
                vertical = ContactPickerTokens.simPillVerticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(ContactPickerTokens.simPillContentSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(ContactPickerTokens.simChipSize)
                    .clip(RoundedCornerShape(ContactPickerTokens.simChipIconCorner))
                    .background(simIndicatorContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = subscriptionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = simIndicatorContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
            Text(
                text = carrierLabel,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
