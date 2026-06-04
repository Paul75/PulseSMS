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


internal const val DESTINATION_INBOX = "inbox"
internal const val DESTINATION_NEW_CHAT = "new_chat"
internal const val DESTINATION_CONVERSATION = "conversation"
internal const val DESTINATION_SETTINGS = "settings"
internal const val DESTINATION_ARCHIVED = "archived"
internal const val DESTINATION_SECURITY = "security"
internal const val DESTINATION_BLOCKED_NUMBERS = "blocked_numbers"
internal const val DESTINATION_LOCK = "lock"
internal const val SCREEN_TRANSITION_DURATION_MILLIS = 180
internal const val SCREEN_TRANSITION_EXIT_DURATION_MILLIS = 120

internal fun conversationReplyabilityForActiveRoute(
    activeAddress: String,
    conversationState: RealConversationState,
): Boolean {
    if (activeAddress.isBlank()) return conversationState.isReplyable
    if (conversationState.address == activeAddress) return conversationState.isReplyable

    return activeAddress.isReplyableConversationAddress()
}

internal fun shouldShowInboxAccessGate(
    accessState: InboxAccessState,
    isLockScreen: Boolean,
): Boolean = !accessState.isReady && !isLockScreen

internal fun screenDepth(destination: String): Int = when (destination) {
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

internal fun screenTransition(
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
