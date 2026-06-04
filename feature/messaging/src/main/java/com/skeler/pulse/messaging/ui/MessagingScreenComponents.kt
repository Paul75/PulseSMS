package com.skeler.pulse.messaging.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skeler.pulse.contracts.messaging.AnchorReason
import com.skeler.pulse.contracts.messaging.ComposerTransition
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.DeliveryIndicator
import com.skeler.pulse.contracts.messaging.MessageDirection
import com.skeler.pulse.contracts.messaging.MessageRenderItem
import com.skeler.pulse.contracts.messaging.MessagingState
import com.skeler.pulse.contracts.messaging.RowSyncState
import com.skeler.pulse.contracts.messaging.SendBlockReason
import com.skeler.pulse.contracts.messaging.SendEligibility
import com.skeler.pulse.contracts.messaging.TimelineOrdering
import com.skeler.pulse.design.component.BubbleShape
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.rememberMomentumFlingBehavior
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.messaging.model.MessagingIntent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
internal fun MessageComposer(
    draft: String,
    enabled: Boolean,
    sendEnabled: Boolean,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    transition: ComposerTransition?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                minLines = 1,
                maxLines = 6,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                ),
                enabled = enabled,
            )
            FilledTonalIconButton(
                onClick = onSend,
                enabled = sendEnabled,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = if (transition == ComposerTransition.SendStarted) "…" else "↑",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun EmptyMessagesState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("💬", fontSize = 40.sp)
            Text("No messages yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Send the first message below",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal data class ConversationBanner(
    val title: String,
    val detail: String,
    val isError: Boolean,
)

internal fun SendEligibility.toBanner(): ConversationBanner? = when (this) {
    SendEligibility.Allowed -> null
    is SendEligibility.Blocked -> ConversationBanner(reason.toBlockedTitle(), reason.toBlockedDetail(), true)
}

internal fun ConversationSyncState.toBanner(): ConversationBanner? = when (this) {
    ConversationSyncState.Idle, ConversationSyncState.Syncing, ConversationSyncState.UpToDate -> null
    is ConversationSyncState.Backoff -> ConversationBanner("Retry scheduled", "We'll keep trying to deliver your messages.", false)
    is ConversationSyncState.Conflict -> ConversationBanner("Sync conflict", "A delivery conflict was detected.", true)
    is ConversationSyncState.Failed -> ConversationBanner("Sync failed", error.message, true)
}

internal fun SendBlockReason.toBlockedTitle(): String = when (this) {
    SendBlockReason.SenderVerificationPending -> "Verification pending"
    SendBlockReason.RecipientVerificationPending -> "Recipient pending"
    SendBlockReason.TenDlcRegistrationPending -> "Registration pending"
    SendBlockReason.MissingIdentityVerification -> "Identity check required"
    SendBlockReason.MissingEncryptionMaterial -> "Encryption unavailable"
    SendBlockReason.RateLimited -> "Sending paused"
    SendBlockReason.Offline -> "Offline"
}

internal fun SendBlockReason.toBlockedDetail(): String = when (this) {
    SendBlockReason.SenderVerificationPending -> "Business verification in progress."
    SendBlockReason.RecipientVerificationPending -> "Recipient needs verification."
    SendBlockReason.TenDlcRegistrationPending -> "10DLC registration in progress."
    SendBlockReason.MissingIdentityVerification -> "Identity verification required."
    SendBlockReason.MissingEncryptionMaterial -> "Encryption keys unavailable."
    SendBlockReason.RateLimited -> "Rate limit hit. Auto-retrying."
    SendBlockReason.Offline -> "No network connection."
}

@Composable
internal fun DeliveryIndicator.toStatusColor() = when (this) {
    DeliveryIndicator.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
    DeliveryIndicator.Queued -> MaterialTheme.colorScheme.tertiary
    DeliveryIndicator.Sent, DeliveryIndicator.Delivered, DeliveryIndicator.Read -> MaterialTheme.colorScheme.primary
    is DeliveryIndicator.Failed -> MaterialTheme.colorScheme.error
}

internal fun DeliveryIndicator.toStatusLabel(): String = when (this) {
    DeliveryIndicator.Pending -> "Sending…"
    DeliveryIndicator.Queued -> "Retrying"
    DeliveryIndicator.Sent -> "Sent"
    DeliveryIndicator.Delivered -> "Delivered"
    DeliveryIndicator.Read -> "Read"
    is DeliveryIndicator.Failed -> "Not sent"
}

internal fun RowSyncState.failureDetail(): String? = when (this) {
    RowSyncState.Idle, RowSyncState.Syncing -> null
    is RowSyncState.Failed -> error.message
}

internal fun Instant.toBubbleTime(): String = BUBBLE_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))

internal val BUBBLE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
