package com.skeler.pulse.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.skeler.pulse.sms.SmsThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun NewChatScreen(
    threads: List<SmsThread>,
    listState: LazyListState,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onStartConversation: (NewChatRecipient, Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fallbackSimOption = remember {
        NewChatSimOption(
            key = "sim_default",
            subscriptionId = null,
            slotLabel = "SIM 1",
            carrierLabel = "Default line",
        )
    }
    val directoryRecipients by produceState<List<NewChatRecipient>?>(
        initialValue = null,
        key1 = context,
    ) {
        value = withContext(Dispatchers.IO) {
            loadNewChatRecipients(context)
        }
    }
    val recipients = remember(directoryRecipients, threads) {
        directoryRecipients?.mergeThreadRecipients(threads)
    }
    val simOptions by produceState(
        initialValue = emptyList<NewChatSimOption>(),
        key1 = context,
    ) {
        value = withContext(Dispatchers.IO) {
            loadSimOptions(context)
        }
    }
    var selectedSimKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(simOptions) {
        if (simOptions.isNotEmpty() && simOptions.none { it.key == selectedSimKey }) {
            selectedSimKey = simOptions.first().key
        }
    }

    val availableSimOptions = remember(simOptions, fallbackSimOption) {
        if (simOptions.isEmpty()) listOf(fallbackSimOption) else simOptions
    }
    val selectedSim = remember(availableSimOptions, selectedSimKey) {
        availableSimOptions.firstOrNull { it.key == selectedSimKey } ?: availableSimOptions.firstOrNull()
    }
    val normalizedQuery = remember(query) { query.trim() }
    val filteredRecipients = remember(normalizedQuery, recipients) {
        val availableRecipients = recipients.orEmpty()
        if (normalizedQuery.isBlank()) {
            availableRecipients
        } else {
            availableRecipients.filter { recipient ->
                recipient.displayName.contains(normalizedQuery, ignoreCase = true) ||
                    recipient.address.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    val directEntryAddress = normalizedQuery.takeIf { it.isDirectAddressCandidate() }
    val shouldShowDirectEntry = remember(directEntryAddress, filteredRecipients) {
        directEntryAddress != null && filteredRecipients.none { recipient ->
            recipient.address.equals(directEntryAddress, ignoreCase = true) ||
                recipient.displayName.equals(directEntryAddress, ignoreCase = true)
        }
    }
    val groupedRecipients = remember(filteredRecipients) {
        filteredRecipients.toContactGroups()
    }
    val recipientIndex = remember(filteredRecipients) {
        filteredRecipients.associateBy { it.key }
    }

    NewChatContactSelectionScreen(
        contactGroups = groupedRecipients,
        loading = recipients == null,
        searchQuery = query,
        simOptions = availableSimOptions,
        selectedSimKey = selectedSim?.key,
        onContactClick = { contact ->
            val recipient = recipientIndex[contact.key] ?: NewChatRecipient(
                key = contact.key,
                displayName = contact.name,
                address = contact.phoneNumber,
                sortLabel = contact.name,
            )
            onStartConversation(recipient, selectedSim?.subscriptionId)
        },
        onBackClick = onBack,
        onSearchQueryChange = onQueryChange,
        modifier = modifier,
        listState = listState,
        manualEntry = directEntryAddress?.takeIf { shouldShowDirectEntry }?.let { address ->
            ContactListItem(
                key = "manual_$address",
                name = address,
                phoneNumber = address,
            )
        },
        onManualEntryClick = { contact ->
            onStartConversation(
                NewChatRecipient(
                    key = contact.key,
                    displayName = contact.phoneNumber,
                    address = contact.phoneNumber,
                    sortLabel = contact.phoneNumber,
                ),
                selectedSim?.subscriptionId,
            )
        },
        onSimOptionClick = { option ->
            selectedSimKey = option.key
        },
    )
}
