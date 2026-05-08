@file:OptIn(ExperimentalMaterial3Api::class)

package com.skeler.pulse.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.sms.SmsThread

internal enum class InboxFilter(val label: String) {
    All("All"), Personal("Personal"), Business("Business"), OTP("OTP"),
}

@Composable
internal fun RealInboxScreen(
    state: RealInboxState,
    listState: LazyListState,
    filterState: LazyListState,
    onOpenConversation: (String, Long?) -> Unit,
    onOpenArchivedChats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewChat: () -> Unit,
    onRefreshInbox: () -> Unit,
    onTogglePinned: (Long) -> Unit,
    onToggleArchived: (Long) -> Unit,
    onSetThreadUnread: (Long?, String, Boolean) -> Unit,
    onBlockThread: (String) -> Unit,
    onDeleteThread: (Long?, String) -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var contextMenuThreadId by rememberSaveable { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    val filterFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)

    val filteredByChip = remember(state.threads, selectedFilter) {
        when (InboxFilter.entries[selectedFilter]) {
            InboxFilter.All -> state.threads
            InboxFilter.OTP -> state.threads.filter { t ->
                t.snippet.contains("code", true) || t.snippet.contains("OTP", true) ||
                    t.snippet.contains("verification", true) || t.snippet.contains("verify", true)
            }
            InboxFilter.Business -> state.threads.filter { t -> t.address.any { it.isLetter() } }
            InboxFilter.Personal -> state.threads.filter { t -> t.address.all { it.isDigit() || it == '+' || it == ' ' } }
        }
    }

    val normalizedQuery = remember(searchQuery) { searchQuery.trim() }
    val searchableDisplayNames = remember(filteredByChip, context) {
        filteredByChip.associateWith { thread ->
            displayNameFor(context, thread.address)
        }
    }
    val filteredThreads = remember(filteredByChip, normalizedQuery, searchableDisplayNames) {
        if (normalizedQuery.isBlank()) {
            filteredByChip
        } else {
            filteredByChip.filter { thread ->
                val displayName = searchableDisplayNames[thread].orEmpty()
                displayName.contains(normalizedQuery, ignoreCase = true) ||
                    thread.address.contains(normalizedQuery, ignoreCase = true) ||
                    thread.snippet.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(normalizedQuery, selectedFilter) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(filteredThreads, contextMenuThreadId) {
        val activeThreadId = contextMenuThreadId ?: return@LaunchedEffect
        if (filteredThreads.none { it.threadId == activeThreadId }) {
            contextMenuThreadId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(onClick = onOpenArchivedChats) {
                        Icon(Icons.Rounded.Archive, contentDescription = "Archived chats", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "New chat"
                },
                onClick = onOpenNewChat,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.AddComment,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        text = "New chat",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
            state = listState,
            flingBehavior = listFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = listState,
                ),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp, end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "inbox_search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(bottom = 6.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search conversations",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                )
            }
            item(key = "filter_chips") {
                LazyRow(
                    state = filterState,
                    flingBehavior = filterFlingBehavior,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .elasticOverscroll(
                            enabled = !reducedMotion,
                            state = filterState,
                            orientation = Orientation.Horizontal,
                        ),
                ) {
                    items(
                        count = InboxFilter.entries.size,
                        key = { index -> "inbox_filter_${InboxFilter.entries[index].name}" },
                        contentType = { "inbox_filter_chip" },
                    ) { index ->
                        val filter = InboxFilter.entries[index]
                        val animatedModifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier(filter.name, reducedMotion))
                        FilterChip(
                            modifier = animatedModifier,
                            selected = selectedFilter == index,
                            onClick = { selectedFilter = index },
                            label = { Text(filter.label) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            when {
                state.loading && state.showLoadingCard -> {
                    item(key = "inbox_loading") {
                        InboxLoadingStateCard(onRefreshInbox = onRefreshInbox)
                    }
                }
                state.errorMessage != null -> {
                    item(key = "inbox_error") {
                        InboxErrorStateCard(
                            message = state.errorMessage,
                            onRetry = onRefreshInbox,
                        )
                    }
                }
                state.threads.isEmpty() && !state.loading -> {
                    item(key = "inbox_empty") {
                        InboxEmptyStateCard(onOpenNewChat = onOpenNewChat)
                    }
                }
                filteredThreads.isEmpty() -> {
                    item(key = "inbox_filtered_empty") {
                        InboxFilteredEmptyStateCard(
                            activeFilter = if (normalizedQuery.isNotBlank()) {
                                "${InboxFilter.entries[selectedFilter].label} · \"$normalizedQuery\""
                            } else {
                                InboxFilter.entries[selectedFilter].label
                            },
                            onShowAll = {
                                selectedFilter = InboxFilter.All.ordinal
                                searchQuery = ""
                            },
                        )
                    }
                }
                else -> {
                    items(
                        items = filteredThreads,
                        key = { "${it.threadId}:${it.address}" },
                        contentType = { "inbox_thread" },
                    ) { thread ->
                        val isMenuOpenForThread = contextMenuThreadId == thread.threadId
                        val itemModifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier(thread.address, reducedMotion))
                            .then(if (isMenuOpenForThread) Modifier.zIndex(2f) else Modifier)
                        SmsThreadCard(
                            thread = thread,
                            isPinned = thread.threadId in state.pinnedThreadIds,
                            isArchived = thread.threadId in state.archivedThreadIds,
                            isContextMenuOpen = contextMenuThreadId == thread.threadId,
                            onClick = { onOpenConversation(thread.address, thread.threadId) },
                            onLongPress = { contextMenuThreadId = thread.threadId },
                            onDismissMenu = { contextMenuThreadId = null },
                            onTogglePinned = { onTogglePinned(thread.threadId) },
                            onToggleArchived = { onToggleArchived(thread.threadId) },
                            onToggleUnread = {
                                onSetThreadUnread(
                                    thread.threadId,
                                    thread.address,
                                    thread.unreadCount == 0,
                                )
                            },
                            onBlock = { onBlockThread(thread.address) },
                            onDelete = { onDeleteThread(thread.threadId, thread.address) },
                            modifier = itemModifier,
                        )
                    }
                }
            }

        }
    }
}
}

@Composable
internal fun InboxOnboardingScreen(
    accessState: InboxAccessState,
    hasPendingLaunchRequest: Boolean,
    onRequestSmsPermissions: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAlreadyDefault = !accessState.permissionDenied && accessState.isDefaultSmsApp

    val title: String
    val body: String
    val ctaLabel: String
    val ctaIcon: ImageVector
    val onCtaClick: () -> Unit
    val statusLabel: String

    if (accessState.permissionDenied) {
        title = "Unlock your inbox"
        body = "Pulse needs SMS access before it can read threads, open drafts, and route you into the right conversation."
        ctaLabel = "Grant permissions"
        ctaIcon = Icons.Rounded.Key
        onCtaClick = onRequestSmsPermissions
        statusLabel = "SMS permission required"
    } else if (isAlreadyDefault) {
        title = "Pulse is your default app"
        body = "Android is ready to route SMS messages to Pulse. Everything is set up correctly."
        ctaLabel = "Manage default app"
        ctaIcon = Icons.Rounded.CheckCircle
        onCtaClick = onRequestDefaultSms
        statusLabel = "Default SMS app active"
    } else {
        title = "Make Pulse your default"
        body = "Set Pulse as your default SMS app so Android can hand off compose requests, send reliably, and keep your inbox in one place."
        ctaLabel = "Set as default"
        ctaIcon = Icons.Rounded.MarkunreadMailbox
        onCtaClick = onRequestDefaultSms
        statusLabel = "Default SMS app required"
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        ),
                    ),
                ),
        ) {
            val cardWidth = if (maxWidth > 720.dp) 520.dp else maxWidth - 32.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = cardWidth),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        StatusPill(
                            label = statusLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = ctaIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (hasPendingLaunchRequest) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.68f),
                                tonalElevation = 0.dp,
                            ) {
                                Text(
                                    text = "A requested conversation is waiting. Pulse will open it as soon as setup is complete.",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        Button(
                            onClick = onCtaClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = ctaIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(ctaLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InboxLoadingStateCard(
    onRefreshInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Loading your threads",
        body = "Pulse is syncing with Android so your latest messages appear in one place.",
        statusLabel = "Inbox refresh",
        icon = Icons.Rounded.HourglassTop,
        actionLabel = "Refresh",
        onAction = onRefreshInbox,
        modifier = modifier,
    ) {
        SerafinaProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun InboxEmptyStateCard(
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Your inbox is ready",
        body = "There are no SMS threads here yet. Start a new conversation and Pulse will keep the lane warm for you.",
        statusLabel = "Zero threads",
        icon = Icons.Rounded.AddComment,
        actionLabel = "New chat",
        onAction = onOpenNewChat,
        modifier = modifier,
    )
}

@Composable
private fun InboxFilteredEmptyStateCard(
    activeFilter: String,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Nothing in $activeFilter",
        body = "This filter is clear right now. Switch back to the full inbox to see every thread again.",
        statusLabel = "$activeFilter filter",
        icon = Icons.Rounded.Search,
        actionLabel = "Show all",
        onAction = onShowAll,
        modifier = modifier,
    )
}

@Composable
internal fun InboxErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = "Inbox unavailable",
        body = message,
        statusLabel = "Read problem",
        icon = Icons.Rounded.ErrorOutline,
        actionLabel = "Try again",
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
internal fun InboxStateCard(
    title: String,
    body: String,
    statusLabel: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        tonalElevation = 0.dp,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = accentColor,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatusPill(
                            label = statusLabel,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                supportingContent?.invoke()
                FilledTonalButton(onClick = onAction) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
internal fun SmsThreadCard(
    thread: SmsThread,
    isPinned: Boolean,
    isArchived: Boolean,
    isContextMenuOpen: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleArchived: () -> Unit,
    onToggleUnread: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val displayName = remember(thread.address) { displayNameFor(context, thread.address) }
    val initials = displayName.toAvatarInitials()
    val hasUnread = thread.unreadCount > 0
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen -> MaterialTheme.colorScheme.surfaceContainerHigh
            hasUnread -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "thread_card_container",
    )
    val outlineColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen -> MaterialTheme.colorScheme.primary.copy(alpha = 0.44f)
            hasUnread -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        },
        label = "thread_card_outline",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed || isContextMenuOpen) 0.985f else 1f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "thread_card_press_scale",
    )
    val semanticsLabel = remember(displayName, thread.unreadCount) {
        buildString {
            append("Open thread ")
            append(displayName)
            if (thread.unreadCount > 0) {
                append(", ")
                append(thread.unreadCount)
                append(" unread")
            }
        }
    }
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .semantics {
                role = Role.Button
                contentDescription = semanticsLabel
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(outlineColor),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                SerafinaAvatar(imageUrl = null, initials = initials, hasUnread = hasUnread, size = 48.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = displayName,
                            style = if (hasUnread) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isPinned) {
                            StatusPill(
                                label = "Pinned",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Text(
                        text = thread.snippet,
                        style = if (hasUnread) {
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (hasUnread) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = thread.timestamp.toInboxTimestamp(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (hasUnread) {
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(thread.unreadCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        SerafinaContextMenu(
            expanded = isContextMenuOpen,
            onDismissRequest = onDismissMenu,
        ) {
            SerafinaContextMenuItem(
                text = if (isPinned) "Unpin" else "Pin",
                icon = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                onClick = {
                    onDismissMenu()
                    onTogglePinned()
                },
            )
            SerafinaContextMenuItem(
                text = if (isArchived) "Unarchive" else "Archive",
                icon = Icons.Rounded.Archive,
                onClick = {
                    onDismissMenu()
                    onToggleArchived()
                },
            )
            SerafinaContextMenuItem(
                text = if (hasUnread) "Mark as read" else "Mark as unread",
                icon = Icons.Rounded.MarkunreadMailbox,
                onClick = {
                    onDismissMenu()
                    onToggleUnread()
                },
            )
            SerafinaContextMenuItem(
                text = "Block",
                icon = Icons.Rounded.Block,
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismissMenu()
                    onBlock()
                },
            )
            SerafinaContextMenuDivider()
            SerafinaContextMenuItem(
                text = "Delete",
                icon = Icons.Rounded.Delete,
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismissMenu()
                    onDelete()
                },
            )
        }
    }
}

