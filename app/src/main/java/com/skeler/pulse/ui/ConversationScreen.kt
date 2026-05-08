@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.skeler.pulse.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.component.StatusPill
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.isNearListEnd
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.design.util.rememberSmoothFlingBehavior
import com.skeler.pulse.design.util.scrollToItemSmoothly
import com.skeler.pulse.sms.SystemSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
internal fun RealConversationScreen(
    title: String,
    address: String,
    initialDraft: String,
    initialSubscriptionId: Int?,
    messages: List<SystemSms>,
    loading: Boolean,
    importantMessageIds: Set<Long>,
    sendState: SendState,
    onBack: () -> Unit,
    onSubscriptionIdChange: (Int?) -> Unit,
    onSend: (String) -> Unit,
    onRetrySend: () -> Unit,
    onClearSendState: () -> Unit,
    onDraftConsumed: () -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onBlockConversation: () -> Unit,
    onForwardMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberSmoothFlingBehavior(enabled = !reducedMotion)
    var draft by rememberSaveable(address) { mutableStateOf("") }
    var previousMessageCount by remember(address) { mutableIntStateOf(0) }
    val fallbackSimOption = remember {
        NewChatSimOption(
            key = "sim_default",
            subscriptionId = null,
            slotLabel = "SIM 1",
            carrierLabel = "Default line",
        )
    }
    val simOptions by produceState(
        initialValue = emptyList<NewChatSimOption>(),
        key1 = context,
    ) {
        value = withContext(Dispatchers.IO) {
            loadSimOptions(context)
        }
    }
    val availableSimOptions = remember(simOptions, fallbackSimOption) {
        if (simOptions.isEmpty()) listOf(fallbackSimOption) else simOptions
    }
    var selectedSimKey by rememberSaveable(address) { mutableStateOf<String?>(null) }

    LaunchedEffect(address, initialSubscriptionId, availableSimOptions) {
        val matchingOption = availableSimOptions.firstOrNull { it.subscriptionId == initialSubscriptionId }
        selectedSimKey = when {
            matchingOption != null -> matchingOption.key
            availableSimOptions.any { it.key == selectedSimKey } -> selectedSimKey
            else -> availableSimOptions.firstOrNull()?.key
        }
    }

    val selectedSim = remember(availableSimOptions, selectedSimKey) {
        availableSimOptions.firstOrNull { it.key == selectedSimKey } ?: availableSimOptions.firstOrNull()
    }

    LaunchedEffect(selectedSim?.subscriptionId) {
        onSubscriptionIdChange(selectedSim?.subscriptionId)
    }

    LaunchedEffect(address, initialDraft) {
        if (initialDraft.isNotBlank()) {
            draft = initialDraft
            onDraftConsumed()
        }
    }

    LaunchedEffect(address) {
        onClearSendState()
    }

    LaunchedEffect(sendState) {
        when (sendState) {
            is SendState.Sent -> {
                draft = ""
                delay(1200)
                onClearSendState()
            }
            is SendState.Failed -> {
                if (draft.isBlank()) {
                    draft = sendState.body
                }
            }
            else -> Unit
        }
    }

    val timelineItems = remember(messages) { messages.toConversationTimeline() }
    val unreadCount = remember(messages) { messages.count { it.isInbound && !it.read } }
    val importantCount = remember(messages, importantMessageIds) {
        messages.count { it.id in importantMessageIds }
    }
    var contextMenuMessageId by rememberSaveable(address) { mutableStateOf<Long?>(null) }
    val contextMenuMessage = remember(messages, contextMenuMessageId) {
        messages.firstOrNull { it.id == contextMenuMessageId }
    }
    val clipboardManager = remember(context) {
        context.getSystemService(ClipboardManager::class.java)
    }
    val isNearEnd by remember(listState) {
        derivedStateOf { listState.isNearListEnd() }
    }
    var hasPositionedInitialMessages by remember(address) { mutableStateOf(false) }

    LaunchedEffect(address) {
        previousMessageCount = 0
        hasPositionedInitialMessages = false
    }

    LaunchedEffect(address, loading, timelineItems.size) {
        if (!loading && !hasPositionedInitialMessages && timelineItems.isNotEmpty()) {
            listState.scrollToItem(timelineItems.lastIndex)
            previousMessageCount = messages.size
            hasPositionedInitialMessages = true
        }
    }

    LaunchedEffect(messages.size, timelineItems.size) {
        if (timelineItems.isEmpty()) {
            previousMessageCount = 0
            return@LaunchedEffect
        }

        val listGrew = messages.size > previousMessageCount
        if (hasPositionedInitialMessages && listGrew && isNearEnd) {
            listState.scrollToItemSmoothly(timelineItems.lastIndex)
        }
        previousMessageCount = messages.size
    }

    val isReplyable = remember(address) {
        // Addresses containing letters are typically business/shortcode senders
        // that don't accept replies (OTP, bank, service notifications)
        address.none { it.isLetter() }
    }

    // ── Keyboard auto-scroll: when IME opens, scroll to latest message ──
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible, isNearEnd, timelineItems.size) {
        if (isKeyboardVisible && isNearEnd && timelineItems.isNotEmpty()) {
            listState.scrollToItemSmoothly(timelineItems.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back",
                        )
                    }
                },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SerafinaAvatar(
                            imageUrl = null,
                            initials = title.toAvatarInitials(),
                            hasUnread = messages.lastOrNull()?.let { it.isInbound && !it.read } == true,
                            size = 40.dp,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = address.toConversationMetaLabel(
                                    totalMessages = messages.size,
                                    unreadCount = unreadCount,
                                    importantCount = importantCount,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (isReplyable) {
                Column {
                    ConversationSendStatusRow(
                        sendState = when (sendState) {
                            is SendState.Sent -> SendState.Idle
                            else -> sendState
                        },
                        onRetrySend = onRetrySend,
                    )
                    ConversationComposer(
                        draft = draft,
                        sendState = sendState,
                        simOptions = availableSimOptions,
                        selectedSimKey = selectedSim?.key,
                        onSimOptionClick = { option ->
                            selectedSimKey = option.key
                        },
                        onDraftChange = {
                            draft = it
                            if (sendState is SendState.Failed || sendState is SendState.Sent) {
                                onClearSendState()
                            }
                        },
                        onSend = {
                            val message = draft.trim()
                            if (message.isEmpty()) return@ConversationComposer
                            onSend(message)
                        },
                    )
                }
            } else {
                ReadOnlyConversationNotice()
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = listFlingBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .elasticOverscroll(
                        enabled = !reducedMotion,
                        state = listState,
                    ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "conversation_header") {
                    ConversationOverviewCard(
                        title = title,
                        address = address,
                        messageCount = messages.size,
                        unreadCount = unreadCount,
                        importantCount = importantCount,
                        latestTimestamp = messages.lastOrNull()?.timestamp,
                        modifier = rememberEntranceModifier("conversation_header_$address", reducedMotion),
                    )
                }

                when {
                    loading -> {
                        item(key = "conversation_loading") {
                            ConversationLoadingSkeleton(
                                modifier = rememberEntranceModifier("conversation_loading_$address", reducedMotion),
                            )
                        }
                    }
                    timelineItems.isEmpty() -> {
                        item(key = "conversation_empty") {
                            EmptyConversationState(
                                title = title,
                                modifier = rememberEntranceModifier("conversation_empty_$address", reducedMotion),
                            )
                        }
                    }
                    else -> {
                        items(
                            items = timelineItems,
                            key = ConversationTimelineItem::key,
                            contentType = ConversationTimelineItem::contentType,
                        ) { item ->
                            when (item) {
                                is ConversationTimelineItem.DayDivider -> {
                                    Box(
                                        modifier = motionAnimateItemModifier(reducedMotion)
                                            .then(rememberEntranceModifier(item.key, reducedMotion))
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ) {
                                            Text(
                                                text = item.label,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                is ConversationTimelineItem.UnreadDivider -> {
                                    Box(
                                        modifier = motionAnimateItemModifier(reducedMotion)
                                            .then(rememberEntranceModifier(item.key, reducedMotion))
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        StatusPill(
                                            label = item.label,
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    }
                                }

                                is ConversationTimelineItem.Message -> {
                                    ConversationMessageBubble(
                                        message = item.message,
                                        isImportant = item.message.id in importantMessageIds,
                                        isContextMenuOpen = contextMenuMessageId == item.message.id,
                                        onLongPress = {
                                            contextMenuMessageId =
                                                if (contextMenuMessageId == item.message.id) null else item.message.id
                                        },
                                        onDismissMenu = { contextMenuMessageId = null },
                                        onCopy = {
                                            clipboardManager?.setPrimaryClip(
                                                ClipData.newPlainText("message", item.message.body)
                                            )
                                            contextMenuMessageId = null
                                        },
                                        onDelete = {
                                            onDeleteMessage(item.message.id)
                                            contextMenuMessageId = null
                                        },
                                        onBlock = {
                                            onBlockConversation()
                                            contextMenuMessageId = null
                                        },
                                        onForward = {
                                            onForwardMessage(item.message.body)
                                            contextMenuMessageId = null
                                        },
                                        modifier = motionAnimateItemModifier(reducedMotion)
                                            .then(rememberEntranceModifier(item.key, reducedMotion)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun ConversationOverviewCard(
    title: String,
    address: String,
    messageCount: Int,
    unreadCount: Int,
    importantCount: Int,
    latestTimestamp: Instant?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SerafinaAvatar(
                imageUrl = null,
                initials = title.toAvatarInitials(),
                size = 52.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = address.toConversationMetaLabel(
                        totalMessages = messageCount.coerceAtLeast(0),
                        unreadCount = unreadCount,
                        importantCount = importantCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            latestTimestamp?.let { timestamp ->
                Text(
                    text = timestamp.toInboxTimestamp(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConversationMessageBubble(
    message: SystemSms,
    isImportant: Boolean,
    isContextMenuOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onBlock: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isOutbound = message.isOutbound
    val isUnread = message.isInbound && !message.read
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bubbleShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = if (isOutbound) 24.dp else 10.dp,
        bottomEnd = if (isOutbound) 10.dp else 24.dp,
    )
    val bubbleScale by animateFloatAsState(
        targetValue = if (isPressed || isContextMenuOpen) 0.985f else 1f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "message_bubble_press_scale",
    )
    val bubbleElevation by animateDpAsState(
        targetValue = if (isContextMenuOpen) 6.dp else if (isPressed) 2.dp else if (isOutbound) 0.dp else 1.dp,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "message_bubble_press_elevation",
    )
    val bubbleOutlineColor by animateColorAsState(
        targetValue = when {
            isContextMenuOpen -> MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            isUnread -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f)
            else -> Color.Transparent
        },
        label = "message_bubble_press_outline",
    )
    val shouldShowBubbleOutline = isContextMenuOpen || isPressed || (isUnread && !isOutbound)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                    onLongClick = onLongPress,
                ),
        ) {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                horizontalAlignment = if (isOutbound) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    modifier = Modifier.then(
                        if (shouldShowBubbleOutline) {
                            Modifier.border(
                                width = 1.dp,
                                color = bubbleOutlineColor,
                                shape = bubbleShape,
                            )
                        } else {
                            Modifier
                        }
                    ),
                    shape = bubbleShape,
                    color = if (isOutbound) MaterialTheme.colorScheme.primaryContainer
                    else if (isUnread) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = bubbleElevation,
                ) {
                    Text(
                        text = message.body.ifBlank { " " },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                        ),
                        color = if (isOutbound) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isImportant) {
                        StatusPill(
                            label = "Kept",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = message.timestamp.toConversationTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnread) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SerafinaContextMenu(
                expanded = isContextMenuOpen,
                onDismissRequest = onDismissMenu,
                shadowElevation = 0.dp,
            ) {
                SerafinaContextMenuItem(
                    text = "Copy",
                    icon = Icons.Rounded.ContentCopy,
                    onClick = onCopy,
                )
                SerafinaContextMenuItem(
                    text = "Forward",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onForward,
                )
                SerafinaContextMenuItem(
                    text = "Block",
                    icon = Icons.Rounded.Block,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = onBlock,
                )
                SerafinaContextMenuDivider()
                SerafinaContextMenuItem(
                    text = "Delete",
                    icon = Icons.Rounded.Delete,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ConversationComposer(
    draft: String,
    sendState: SendState,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    onSimOptionClick: (NewChatSimOption) -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isSending = sendState is SendState.Sending
    val canSend by remember(draft, isSending) {
        derivedStateOf { draft.isNotBlank() && !isSending }
    }
    val showSimToggle = simOptions.size > 1
    val fieldInteractionSource = remember { MutableInteractionSource() }
    val isFocused by fieldInteractionSource.collectIsFocusedAsState()
    val sendInteractionSource = remember { MutableInteractionSource() }
    val isSendPressed by sendInteractionSource.collectIsPressedAsState()
    val quickDuration = if (reducedMotion) 0 else 200
    val exitDuration = if (reducedMotion) 0 else 140

    // ── Pill container animations ──
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "composer_container",
    )
    val containerLift by animateFloatAsState(
        targetValue = if (isFocused) -2f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "composer_lift",
    )

    // ── Send button animations (the hero moment) ──
    val sendScale by animateFloatAsState(
        targetValue = when {
            isSendPressed && canSend -> 0.92f
            canSend -> 1f
            else -> 0.88f
        },
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "send_scale",
    )
    val sendContainerColor by animateColorAsState(
        targetValue = if (canSend) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "send_color",
    )
    val sendContentColor by animateColorAsState(
        targetValue = if (canSend) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = quickDuration),
        label = "send_content",
    )
    val sendIconRotation by animateFloatAsState(
        targetValue = if (canSend) 0f else -30f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "send_rotation",
    )

    val pillShape = RoundedCornerShape(28.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .graphicsLayer { translationY = containerLift },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showSimToggle) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(simOptions, key = { it.key }) { option ->
                    val selected = option.key == selectedSimKey
                    FilterChip(
                        selected = selected,
                        onClick = { onSimOptionClick(option) },
                        enabled = !isSending,
                        modifier = Modifier.height(32.dp),
                        label = {
                            Text(
                                text = option.slotLabel,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = null,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                enabled = !isSending,
                interactionSource = fieldInteractionSource,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            onSend()
                        }
                    },
                ),
                maxLines = 6,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(pillShape)
                            .background(containerColor)
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                text = if (isSending) "Sending…" else "Message",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .clip(CircleShape)
                    .background(sendContainerColor)
                    .clickable(
                        interactionSource = sendInteractionSource,
                        indication = null,
                        enabled = canSend,
                        onClick = onSend,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send message",
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = sendIconRotation },
                    tint = sendContentColor,
                )
            }
        }
    }
}

@Composable
private fun ConversationSendStatusRow(
    sendState: SendState,
    onRetrySend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    AnimatedVisibility(
        visible = sendState !is SendState.Idle,
        enter = fadeIn(animationSpec = tween(if (reducedMotion) 0 else 180)),
        exit = fadeOut(animationSpec = tween(if (reducedMotion) 0 else 120)),
        modifier = modifier,
    ) {
        val containerColor = when (sendState) {
            is SendState.Sending -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            is SendState.Sent -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
            is SendState.Failed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.76f)
            SendState.Idle -> MaterialTheme.colorScheme.surfaceContainerLow
        }
        val contentColor = when (sendState) {
            is SendState.Sending -> MaterialTheme.colorScheme.onTertiaryContainer
            is SendState.Sent -> MaterialTheme.colorScheme.onSecondaryContainer
            is SendState.Failed -> MaterialTheme.colorScheme.onErrorContainer
            SendState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val icon = when (sendState) {
            is SendState.Sending -> Icons.Rounded.HourglassTop
            is SendState.Sent -> Icons.Rounded.CheckCircle
            is SendState.Failed -> Icons.Rounded.ErrorOutline
            SendState.Idle -> Icons.Rounded.CheckCircle
        }
        val title = when (sendState) {
            is SendState.Sending -> "Sending message"
            is SendState.Sent -> "Message sent"
            is SendState.Failed -> "Send failed"
            SendState.Idle -> ""
        }
        val subtitle = when (sendState) {
            is SendState.Sending -> "Holding your draft until Android confirms the handoff."
            is SendState.Sent -> "Your draft cleared after the send completed."
            is SendState.Failed -> "Your draft is still here. Retry when you're ready."
            SendState.Idle -> ""
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.84f),
                    )
                }
                if (sendState is SendState.Failed) {
                    FilledTonalButton(onClick = onRetrySend) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyConversationNotice(
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "read_only_notice_border",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            )
            .semantics {
                contentDescription = "Read only business sender. Replies are disabled."
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Read only",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Business sender — replies unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConversationLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StatusPill(
                label = "Opening thread",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            SerafinaProgressIndicator(modifier = Modifier.fillMaxWidth())
            repeat(3) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End,
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 280.dp),
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = if (index % 2 == 0) 10.dp else 24.dp,
                            bottomEnd = if (index % 2 == 0) 24.dp else 10.dp,
                        ),
                        color = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                        },
                        tonalElevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(if (index == 1) 0.78f else 0.9f)
                                    .height(14.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                tonalElevation = 0.dp,
                            ) {}
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(if (index == 2) 0.52f else 0.66f)
                                    .height(14.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                                tonalElevation = 0.dp,
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState(
    title: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Start a cleaner thread with $title using the composer below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
