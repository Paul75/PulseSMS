package com.skeler.pulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.PulseLaunchRequest
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.security.auth.checkBiometricAvailability
import com.skeler.pulse.shouldHandleOpenNewChatRequest

private const val DESTINATION_INBOX = "inbox"
private const val DESTINATION_NEW_CHAT = "new_chat"
private const val DESTINATION_CONVERSATION = "conversation"
private const val DESTINATION_SETTINGS = "settings"
private const val DESTINATION_ARCHIVED = "archived"
private const val DESTINATION_SECURITY = "security"
private const val DESTINATION_BLOCKED_NUMBERS = "blocked_numbers"
private const val DESTINATION_LOCK = "lock"
private const val SCREEN_TRANSITION_DURATION_MILLIS = 180
private const val SCREEN_TRANSITION_EXIT_DURATION_MILLIS = 120

private fun screenDepth(destination: String): Int = when (destination) {
    DESTINATION_INBOX -> 0
    DESTINATION_NEW_CHAT,
    DESTINATION_SETTINGS,
    DESTINATION_ARCHIVED -> 1
    DESTINATION_CONVERSATION,
    DESTINATION_SECURITY,
    DESTINATION_BLOCKED_NUMBERS,
    DESTINATION_LOCK -> 2
    else -> 0
}

private fun screenTransition(
    initialState: String,
    targetState: String,
    reducedMotion: Boolean,
): ContentTransform {
    if (reducedMotion) {
        return ContentTransform(
            targetContentEnter = fadeIn(tween(durationMillis = 0)),
            initialContentExit = fadeOut(tween(durationMillis = 0)),
            sizeTransform = null,
        )
    }

    val forward = screenDepth(targetState) >= screenDepth(initialState)
    val enterOffset: (Int) -> Int = { width -> if (forward) width / 5 else -width / 5 }
    val exitOffset: (Int) -> Int = { width -> if (forward) -width / 8 else width / 8 }

    val enterTransition =
        slideInHorizontally(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetX = enterOffset,
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_EXIT_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )

    val exitTransition =
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_EXIT_DURATION_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
            targetOffsetX = exitOffset,
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = SCREEN_TRANSITION_EXIT_DURATION_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
        )

    return ContentTransform(
        targetContentEnter = enterTransition,
        initialContentExit = exitTransition,
        sizeTransform = null,
    )
}

@Composable
fun PulseAppShell(
    smsViewModel: RealSmsViewModel,
    launchRequest: PulseLaunchRequest? = null,
    openNewChatRequestKey: Int = 0,
    accessState: InboxAccessState = InboxAccessState(),
    onLaunchRequestConsumed: () -> Unit = {},
    onRequestNewChat: () -> Unit = {},
    onRequestSmsPermissions: () -> Unit = {},
    onOpenConversation: (String, Long?) -> Unit,
    onSendMessage: (String, String, Int?) -> Unit,
    themeViewModel: SerafinaThemeViewModel,
    onRequestDefaultSms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var backStack by rememberSaveable { mutableStateOf(listOf(DESTINATION_INBOX)) }
    var activeAddress by rememberSaveable { mutableStateOf("") }
    var activeConversationTitle by rememberSaveable { mutableStateOf("") }
    var activeSubscriptionId by rememberSaveable { mutableStateOf<Int?>(null) }
    var conversationDraftSeed by rememberSaveable { mutableStateOf("") }
    var pendingForwardDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var newChatQuery by rememberSaveable { mutableStateOf("") }
    var lastHandledNewChatRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var consumedLaunchRequest by rememberSaveable { mutableStateOf(false) }
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val shellThemeState by themeViewModel.state.collectAsState()
    val currentScreen = backStack.lastOrNull() ?: DESTINATION_INBOX
    val reducedMotion = rememberReducedMotionEnabled()
    val inboxListState = rememberLazyListState()
    val inboxFilterState = rememberLazyListState()
    val archivedListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()
    val blockedNumbersListState = rememberLazyListState()
    val newChatListState = rememberLazyListState()

    LaunchedEffect(launchRequest, accessState, consumedLaunchRequest) {
        if (!accessState.isReady || consumedLaunchRequest) return@LaunchedEffect
        val request = launchRequest ?: return@LaunchedEffect
        consumedLaunchRequest = true
        val requestedAddress = request.conversationAddress
        conversationDraftSeed = request.draftBody
        if (requestedAddress.isNotBlank()) {
            activeAddress = requestedAddress
            activeConversationTitle = request.conversationTitle.ifBlank { displayNameFor(context, requestedAddress) }
            activeSubscriptionId = null
            onOpenConversation(requestedAddress, null)
            backStack = listOf(DESTINATION_INBOX, DESTINATION_CONVERSATION)
        } else {
            activeAddress = ""
            activeConversationTitle = ""
            activeSubscriptionId = null
            backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT)
        }
        onLaunchRequestConsumed()
    }

    LaunchedEffect(openNewChatRequestKey, lastHandledNewChatRequestKey, accessState) {
        if (!shouldHandleOpenNewChatRequest(openNewChatRequestKey, lastHandledNewChatRequestKey, accessState)) {
            return@LaunchedEffect
        }
        backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT)
        lastHandledNewChatRequestKey = openNewChatRequestKey
    }

    // Trigger app lock when either configured unlock method requires authentication.
    LaunchedEffect(shellThemeState.fingerprintEnabled, shellThemeState.password, isAuthenticated) {
        val securityEnabled = shellThemeState.fingerprintEnabled || shellThemeState.password.isNotBlank()
        if (!securityEnabled) {
            isAuthenticated = false
            if (backStack.lastOrNull() == DESTINATION_LOCK) {
                backStack = listOf(DESTINATION_INBOX)
            }
            return@LaunchedEffect
        }
        if (!isAuthenticated) {
            backStack = listOf(DESTINATION_LOCK)
        }
    }

    fun navigateBack() {
        if (currentScreen == DESTINATION_CONVERSATION) {
            smsViewModel.closeConversation()
        }
        backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
    }

    BackHandler(enabled = currentScreen != DESTINATION_INBOX) {
        if (currentScreen != DESTINATION_LOCK) {
            navigateBack()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                screenTransition(
                    initialState = initialState,
                    targetState = targetState,
                    reducedMotion = reducedMotion,
                )
            },
            label = "screen_transition",
        ) { screen ->
            when (screen) {
                DESTINATION_INBOX -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    if (!accessState.isReady) {
                        InboxOnboardingScreen(
                            accessState = accessState,
                            hasPendingLaunchRequest = launchRequest != null,
                            onRequestSmsPermissions = onRequestSmsPermissions,
                            onRequestDefaultSms = onRequestDefaultSms,
                        )
                    } else {
                        RealInboxScreen(
                            state = inboxState,
                            listState = inboxListState,
                            filterState = inboxFilterState,
                            onOpenConversation = { address, threadId ->
                                activeAddress = address
                                activeConversationTitle = displayNameFor(context, address)
                                activeSubscriptionId = null
                                conversationDraftSeed = ""
                                onOpenConversation(address, threadId)
                                backStack = listOf(DESTINATION_INBOX, DESTINATION_CONVERSATION)
                            },
                            onOpenArchivedChats = {
                                backStack = listOf(DESTINATION_INBOX, DESTINATION_ARCHIVED)
                            },
                            onOpenSettings = {
                                backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS)
                            },
                            onOpenNewChat = onRequestNewChat,
                            onRefreshInbox = smsViewModel::refreshInbox,
                            onTogglePinned = smsViewModel::toggleThreadPinned,
                            onToggleArchived = smsViewModel::toggleThreadArchived,
                            onSetThreadUnread = smsViewModel::setThreadUnread,
                            onBlockThread = smsViewModel::blockThread,
                            onDeleteThread = smsViewModel::deleteThread,
                        )
                    }
                }

                DESTINATION_NEW_CHAT -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    NewChatScreen(
                        threads = inboxState.threads,
                        listState = newChatListState,
                        query = newChatQuery,
                        onQueryChange = { newChatQuery = it },
                        onBack = {
                            backStack = listOf(DESTINATION_INBOX)
                        },
                        onStartConversation = { recipient, subscriptionId ->
                            activeAddress = recipient.address
                            activeConversationTitle = displayNameFor(context, recipient.address)
                            activeSubscriptionId = subscriptionId
                            conversationDraftSeed = pendingForwardDraft.orEmpty()
                            pendingForwardDraft = null
                            onOpenConversation(recipient.address, null)
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT, DESTINATION_CONVERSATION)
                        },
                    )
                }

                DESTINATION_CONVERSATION -> {
                    val conversationState by smsViewModel.conversationState.collectAsState()
                    val sendState by smsViewModel.sendState.collectAsState()
                    RealConversationScreen(
                        title = activeConversationTitle.ifBlank { displayNameFor(context, activeAddress) },
                        address = activeAddress,
                        initialDraft = conversationDraftSeed,
                        initialSubscriptionId = activeSubscriptionId,
                        messages = if (conversationState.address == activeAddress) {
                            conversationState.messages
                        } else {
                            emptyList()
                        },
                        loading = conversationState.address == activeAddress && conversationState.loading,
                        importantMessageIds = if (conversationState.address == activeAddress) {
                            conversationState.importantMessageIds
                        } else {
                            emptySet()
                        },
                        sendState = sendState,
                        onBack = {
                            navigateBack()
                        },
                        onSubscriptionIdChange = { activeSubscriptionId = it },
                        onSend = { body ->
                            onSendMessage(activeAddress, body, activeSubscriptionId)
                        },
                        onRetrySend = smsViewModel::retrySend,
                        onClearSendState = smsViewModel::clearSendState,
                        onDraftConsumed = { conversationDraftSeed = "" },
                        onDeleteMessage = smsViewModel::deleteMessage,
                        onBlockConversation = {
                            smsViewModel.blockThread(activeAddress)
                            smsViewModel.closeConversation()
                            backStack = listOf(DESTINATION_INBOX)
                        },
                        onForwardMessage = { body ->
                            pendingForwardDraft = body
                            newChatQuery = ""
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_NEW_CHAT)
                        },
                    )
                }

                DESTINATION_SETTINGS -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        listState = settingsListState,
                        archivedCount = inboxState.archivedThreads.size,
                        blockedCount = inboxState.blockedAddresses.size,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onRequestDefaultSms = onRequestDefaultSms,
                        onOpenArchivedChats = {
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS, DESTINATION_ARCHIVED)
                        },
                        onOpenSecurity = {
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS, DESTINATION_SECURITY)
                        },
                        onOpenBlockedNumbers = {
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS, DESTINATION_BLOCKED_NUMBERS)
                        },
                        isDefaultSmsApp = inboxState.isDefaultSmsApp,
                    )
                }

                DESTINATION_ARCHIVED -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    ArchivedChatsScreen(
                        threads = inboxState.archivedThreads,
                        pinnedThreadIds = inboxState.pinnedThreadIds,
                        archivedThreadIds = inboxState.archivedThreadIds,
                        loading = inboxState.loading,
                        errorMessage = inboxState.errorMessage,
                        listState = archivedListState,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onOpenConversation = { address, threadId ->
                            activeAddress = address
                            activeConversationTitle = displayNameFor(context, address)
                            activeSubscriptionId = null
                            conversationDraftSeed = ""
                            onOpenConversation(address, threadId)
                            backStack = listOf(DESTINATION_INBOX, DESTINATION_SETTINGS, DESTINATION_ARCHIVED, DESTINATION_CONVERSATION)
                        },
                        onRefreshInbox = smsViewModel::refreshInbox,
                        onTogglePinned = smsViewModel::toggleThreadPinned,
                        onToggleArchived = smsViewModel::toggleThreadArchived,
                        onSetThreadUnread = smsViewModel::setThreadUnread,
                        onBlockThread = smsViewModel::blockThread,
                        onDeleteThread = smsViewModel::deleteThread,
                    )
                }

                DESTINATION_SECURITY -> {
                    SecuritySettingsScreen(
                        themeViewModel = themeViewModel,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                    )
                }

                DESTINATION_BLOCKED_NUMBERS -> {
                    val inboxState by smsViewModel.inboxState.collectAsState()
                    BlockedNumbersScreen(
                        blockedAddresses = inboxState.blockedAddresses,
                        listState = blockedNumbersListState,
                        onBack = {
                            backStack = backStack.dropLast(1).ifEmpty { listOf(DESTINATION_INBOX) }
                        },
                        onUnblock = smsViewModel::unblockThread,
                    )
                }

                DESTINATION_LOCK -> {
                    LockScreen(
                        biometricEnabled = shellThemeState.fingerprintEnabled,
                        biometricAvailability = checkBiometricAvailability(context),
                        passwordVerifierToken = shellThemeState.password,
                        onAuthenticated = {
                            isAuthenticated = true
                            backStack = listOf(DESTINATION_INBOX)
                        },
                        onCancel = {
                            // Stay on lock screen — user must authenticate
                        },
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// REAL SMS INBOX
// ═══════════════════════════════════════════════════════════

