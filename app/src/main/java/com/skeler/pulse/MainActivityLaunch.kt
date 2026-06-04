package com.skeler.pulse
import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.design.theme.SerafinaAppTheme
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.ui.PulseAppShell
import com.skeler.pulse.ui.RealSmsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class PulseLaunchRequest(
    val conversationAddress: String = "",
    val conversationTitle: String = "",
    val draftBody: String = "",
)

data class InboxAccessState(
    val permissionDenied: Boolean = false,
    val isDefaultSmsApp: Boolean = true,
) {
    val isReady: Boolean get() = !permissionDenied && isDefaultSmsApp
}

internal const val NEW_CHAT_PERMISSION_REQUEST_NONE = 0
internal const val NEW_CHAT_PERMISSION_REQUEST_PENDING_OPEN = 1
internal const val DEFAULT_SMS_RECONCILE_ATTEMPTS = 5
internal const val DEFAULT_SMS_RECONCILE_DELAY_MILLIS = 250L

internal fun requiredCorePermissions(sdkInt: Int): List<String> = buildList {
    add(Manifest.permission.READ_SMS)
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.RECEIVE_SMS)
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

internal fun requiredNewChatPermissions(): List<String> = listOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_PHONE_STATE,
)

internal fun missingPermissions(
    context: Context,
    permissions: List<String>,
): List<String> = permissions.filter {
    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
}

internal fun shouldOpenNewChatAfterPermissionResult(
    requestedNewChatOpen: Boolean,
    hasContactPermission: Boolean,
): Boolean = requestedNewChatOpen && hasContactPermission

internal fun shouldHandleLaunchRequest(
    launchRequest: PulseLaunchRequest?,
    accessState: InboxAccessState,
): Boolean = launchRequest != null && accessState.isReady

internal fun shouldHandleOpenNewChatRequest(
    requestKey: Int,
    lastHandledRequestKey: Int,
    accessState: InboxAccessState,
): Boolean = requestKey > lastHandledRequestKey && accessState.isReady

internal fun isSystemNightMode(uiMode: Int): Boolean =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

internal fun resolveDarkTheme(themeMode: SerafinaThemeMode, systemDarkTheme: Boolean): Boolean =
    when (themeMode) {
        SerafinaThemeMode.System -> systemDarkTheme
        SerafinaThemeMode.Light -> false
        SerafinaThemeMode.Dark -> true
    }

internal fun buildPulseLaunchRequestOrNull(
    conversationAddress: String?,
    draftBody: String?,
): PulseLaunchRequest? {
    val normalizedConversationAddress = conversationAddress.orEmpty().trim()
    val normalizedDraftBody = draftBody.orEmpty().trim()
    return if (normalizedConversationAddress.isBlank() && normalizedDraftBody.isBlank()) {
        null
    } else {
        PulseLaunchRequest(
            conversationAddress = normalizedConversationAddress,
            draftBody = normalizedDraftBody,
        )
    }
}

internal fun Context.toPulseLaunchRequestOrNull(
    conversationAddress: String?,
    draftBody: String?,
): PulseLaunchRequest? {
    val request = buildPulseLaunchRequestOrNull(
        conversationAddress = conversationAddress,
        draftBody = draftBody,
    ) ?: return null
    return request.copy(
        conversationTitle = request.conversationAddress
            .takeIf(String::isNotBlank)
            ?.let { displayNameFor(this, it) }
            .orEmpty(),
    )
}

internal fun Intent?.toPulseLaunchRequestOrNull(context: Context): PulseLaunchRequest? {
    if (this == null) return null
    return context.toPulseLaunchRequestOrNull(
        conversationAddress = getStringExtra(MainActivity.EXTRA_CONVERSATION_ADDRESS),
        draftBody = getStringExtra(MainActivity.EXTRA_COMPOSE_BODY),
    )
}

internal fun resolveInboxAccessState(
    context: Context,
    sdkInt: Int,
    packageName: String,
): InboxAccessState = InboxAccessState(
    permissionDenied = missingPermissions(context, requiredCorePermissions(sdkInt)).isNotEmpty(),
    isDefaultSmsApp = isDefaultSmsApp(
        context = context,
        sdkInt = sdkInt,
        packageName = packageName,
    ),
)

internal fun isDefaultSmsApp(
    packageName: String,
    telephonyDefaultPackage: String?,
    sdkInt: Int,
    smsRoleHeld: Boolean,
): Boolean {
    if (telephonyDefaultPackage == packageName) {
        return true
    }
    if (sdkInt < Build.VERSION_CODES.Q) {
        return false
    }
    return smsRoleHeld
}

internal fun isDefaultSmsApp(
    context: Context,
    sdkInt: Int,
    packageName: String,
): Boolean {
    val telephonyDefaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
    val smsRoleHeld = if (sdkInt >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isSmsRoleHeld(context)
    } else {
        false
    }
    return isDefaultSmsApp(
        packageName = packageName,
        telephonyDefaultPackage = telephonyDefaultPackage,
        sdkInt = sdkInt,
        smsRoleHeld = smsRoleHeld,
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
internal fun isSmsRoleHeld(context: Context): Boolean {
    val roleManager = context.getSystemService(RoleManager::class.java)
    return roleManager != null &&
        roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
        roleManager.isRoleHeld(RoleManager.ROLE_SMS)
}

@Suppress("DEPRECATION")
internal fun legacyDefaultSmsRequestIntent(packageName: String): Intent =
    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
    }

internal fun defaultAppsSettingsIntent(): Intent =
    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

internal fun appDetailsSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
