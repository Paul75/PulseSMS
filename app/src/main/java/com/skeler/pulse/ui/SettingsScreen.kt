package com.skeler.pulse.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.theme.SerafinaPalette
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.rememberMomentumFlingBehavior
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import com.skeler.pulse.security.auth.BiometricAuthResult
import com.skeler.pulse.security.auth.BiometricAvailability
import com.skeler.pulse.security.auth.checkBiometricAvailability
import com.skeler.pulse.security.auth.showBiometricPrompt

internal data class SettingsChoiceOption(
    val id: String,
    val label: String,
    val accentColor: Color? = null,
)

// ═══════════════════════════════════════════════════════════
// SETTINGS SCREEN — Real features wired from codebase
// ═══════════════════════════════════════════════════════════

@Composable
internal fun SettingsScreen(
    themeViewModel: SerafinaThemeViewModel,
    listState: LazyListState,
    archivedCount: Int,
    blockedCount: Int,
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onOpenArchivedChats: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenBlockedNumbers: () -> Unit,
    isDefaultSmsApp: Boolean,
) {
    val themeState by themeViewModel.state.collectAsState()
    val reducedMotion = rememberReducedMotionEnabled()
    val settingsFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val appearanceOptionState = rememberLazyListState()
    val appearanceOptionFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val colorSchemeOptions = remember {
        buildList {
            add(SettingsChoiceOption(id = "dynamic", label = "Dynamic"))
            addAll(
                SerafinaPalette.entries.map { palette ->
                    SettingsChoiceOption(
                        id = palette.name,
                        label = palette.label,
                        accentColor = palette.seedColor,
                    )
                },
            )
        }
    }
    val themeOptions = remember {
        SerafinaThemeMode.entries.map { mode ->
            SettingsChoiceOption(id = mode.name, label = mode.label)
        }
    }
    val selectedColorSchemeId = if (themeState.dynamicColorEnabled) {
        "dynamic"
    } else {
        themeState.selectedPalette.name
    }
    val colorSchemeLabel = if (themeState.dynamicColorEnabled) {
        "Dynamic"
    } else {
        themeState.selectedPalette.label
    }

    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            flingBehavior = settingsFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = listState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "general_header") { SettingsSectionHeader("General") }
            item(key = "general_card") {
                SettingsGroupCard {
                    SettingsRow(
                        icon = if (isDefaultSmsApp) Icons.Rounded.CheckCircle else Icons.Outlined.Sms,
                        title = if (isDefaultSmsApp) "Default SMS app" else "Set as default",
                        subtitle = if (isDefaultSmsApp) "Pulse is your default SMS app" else "Tap to set Pulse as default",
                        onClick = onRequestDefaultSms,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Archive,
                        title = "Archived chats",
                        subtitle = if (archivedCount == 0) "No archived chats" else "$archivedCount archived chats",
                        onClick = onOpenArchivedChats,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "Security & Biometric",
                        subtitle = "Fingerprint, password",
                        onClick = onOpenSecurity,
                    )
                    SettingsGroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.Block,
                        title = "Blocked numbers",
                        subtitle = if (blockedCount == 0) "No blocked senders" else "$blockedCount blocked senders",
                        onClick = onOpenBlockedNumbers,
                    )
                }
            }
            item(key = "appearance_header") { SettingsSectionHeader("Appearance") }
            item(key = "appearance_card") {
                SettingsAppearanceCard(
                    colorSchemeLabel = colorSchemeLabel,
                    selectedColorSchemeId = selectedColorSchemeId,
                    colorSchemeOptions = colorSchemeOptions,
                    appearanceOptionState = appearanceOptionState,
                    appearanceOptionFlingBehavior = appearanceOptionFlingBehavior,
                    reducedMotion = reducedMotion,
                    themeMode = themeState.themeMode,
                    themeOptions = themeOptions,
                    blackThemeEnabled = themeState.blackThemeEnabled,
                    onSelectColorScheme = { optionId ->
                        if (optionId == "dynamic") {
                            if (!themeState.dynamicColorEnabled) {
                                themeViewModel.toggleDynamicColor()
                            }
                        } else {
                            if (themeState.dynamicColorEnabled) {
                                themeViewModel.toggleDynamicColor()
                            }
                            SerafinaPalette.entries.firstOrNull { it.name == optionId }?.let(themeViewModel::selectPalette)
                        }
                    },
                    onSelectThemeMode = { optionId ->
                        SerafinaThemeMode.entries.firstOrNull { it.name == optionId }?.let(themeViewModel::selectThemeMode)
                    },
                    onToggleBlackTheme = { themeViewModel.setBlackThemeEnabled(!themeState.blackThemeEnabled) },
                )
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Settings sub-components ──

@Composable
internal fun SettingsTopBar(
    onBack: () -> Unit,
    title: String = "Settings",
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(72.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun SettingsSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp))
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column { content() } }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsAppearanceCard(
    colorSchemeLabel: String,
    selectedColorSchemeId: String,
    colorSchemeOptions: List<SettingsChoiceOption>,
    appearanceOptionState: LazyListState,
    appearanceOptionFlingBehavior: androidx.compose.foundation.gestures.FlingBehavior,
    reducedMotion: Boolean,
    themeMode: SerafinaThemeMode,
    themeOptions: List<SettingsChoiceOption>,
    blackThemeEnabled: Boolean,
    onSelectColorScheme: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onToggleBlackTheme: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsExpressiveRow(
                    icon = Icons.Outlined.Palette,
                    title = "Color scheme",
                    subtitle = colorSchemeLabel,
                    reducedMotion = reducedMotion,
                ) {
                    SettingsChoiceRail(
                        options = colorSchemeOptions,
                        selectedId = selectedColorSchemeId,
                        listState = appearanceOptionState,
                        flingBehavior = appearanceOptionFlingBehavior,
                        reducedMotion = reducedMotion,
                        onSelect = onSelectColorScheme,
                    )
                }
                SettingsExpressiveRow(
                    icon = Icons.Outlined.Contrast,
                    title = "Theme",
                    subtitle = themeMode.label,
                    reducedMotion = reducedMotion,
                ) {
                    SettingsChoiceRail(
                        options = themeOptions,
                        selectedId = themeMode.name,
                        reducedMotion = reducedMotion,
                        onSelect = onSelectThemeMode,
                    )
                }
                SettingsExpressiveToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Black theme only",
                    subtitle = "Use pure black surfaces whenever the app is in dark mode.",
                    checked = blackThemeEnabled,
                    onToggle = onToggleBlackTheme,
                )
            }
        }
    }
}

@Composable
private fun SettingsExpressiveRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    reducedMotion: Boolean,
    controls: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .animateContentSize(
                animationSpec = if (reducedMotion) {
                    tween(durationMillis = 0)
                } else {
                    spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = Spring.DampingRatioNoBouncy,
                    )
                },
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsExpressiveIcon(icon = icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        controls()
    }
}

@Composable
private fun SettingsExpressiveToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val trackColor = if (checked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsExpressiveIcon(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = trackColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = trackColor,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
private fun SettingsExpressiveIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsChoiceRail(
    options: List<SettingsChoiceOption>,
    selectedId: String,
    reducedMotion: Boolean,
    onSelect: (String) -> Unit,
    listState: LazyListState? = null,
    flingBehavior: androidx.compose.foundation.gestures.FlingBehavior? = null,
) {
    val resolvedListState = listState ?: rememberLazyListState()
    val resolvedFlingBehavior = flingBehavior ?: rememberSnapFlingBehavior(
        lazyListState = resolvedListState,
        snapPosition = SnapPosition.Start,
    )

    LazyRow(
        state = resolvedListState,
        flingBehavior = resolvedFlingBehavior,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.elasticOverscroll(
            enabled = !reducedMotion,
            state = resolvedListState,
            orientation = Orientation.Horizontal,
        ),
    ) {
        items(
            items = options,
            key = { option -> option.id },
            contentType = { "settings_choice" },
        ) { option ->
            SettingsChoicePill(
                option = option,
                selected = option.id == selectedId,
                reducedMotion = reducedMotion,
                onClick = { onSelect(option.id) },
            )
        }
    }
}

@Composable
private fun SettingsChoicePill(
    option: SettingsChoiceOption,
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.34f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy,
            )
        },
        label = "settings_choice_scale",
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        option.accentColor?.let { accentColor ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

// ═══════════════════════════════════════════════════════════
// SECURITY SETTINGS
// ═══════════════════════════════════════════════════════════

@Composable
internal fun SecuritySettingsScreen(
    themeViewModel: SerafinaThemeViewModel,
    onBack: () -> Unit,
) {
    val themeState by themeViewModel.state.collectAsState()
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled()
    val securityListState = rememberLazyListState()
    val securityFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    var biometricToggleError by rememberSaveable { mutableStateOf<String?>(null) }
    fun requestFingerprintToggle() {
        if (themeState.fingerprintEnabled) {
            biometricToggleError = null
            themeViewModel.setFingerprintEnabled(false)
            return
        }
        val availability = checkBiometricAvailability(context)
        if (availability != BiometricAvailability.Available) {
            biometricToggleError = availability.lockScreenMessage()
            return
        }
        val activity = context.findFragmentActivity()
        if (activity == null) {
            biometricToggleError = "Biometric prompt could not be started."
            return
        }
        biometricToggleError = null
        showBiometricPrompt(
            activity = activity,
            title = "Enable biometric login",
            subtitle = "Authenticate to protect Pulse with biometrics",
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    biometricToggleError = null
                    themeViewModel.setFingerprintEnabled(true)
                }
                is BiometricAuthResult.Cancelled -> Unit
                is BiometricAuthResult.Failed -> {
                    biometricToggleError = "Authentication failed. Try again."
                }
                is BiometricAuthResult.Error -> {
                    biometricToggleError = result.message
                }
            }
        }
    }

    Scaffold(
        topBar = { SettingsTopBar(onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            state = securityListState,
            flingBehavior = securityFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = securityListState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "security_header") {
                SettingsSectionHeader("Security & Biometric")
            }
            item(key = "security_card") {
                SettingsGroupCard {
                    SecurityFingerprintRow(
                        enabled = themeState.fingerprintEnabled,
                        error = biometricToggleError,
                        onToggle = ::requestFingerprintToggle,
                    )
                    SettingsGroupDivider()
                    SecurityPasswordSection(
                        passwordSet = themeState.password.isNotEmpty(),
                        onSetPassword = { themeViewModel.setPassword(it) },
                        onClearPassword = { themeViewModel.clearPassword() },
                    )
                }
            }
            item(key = "security_bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SecurityFingerprintRow(
    enabled: Boolean,
    error: String?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Fingerprint", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = error ?: if (enabled) "Biometric login active" else "Tap to enable biometric login",
                style = MaterialTheme.typography.bodySmall,
                color = if (error == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SecurityPasswordSection(
    passwordSet: Boolean,
    onSetPassword: (String) -> Unit,
    onClearPassword: () -> Unit,
) {
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Password", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (passwordSet) "Alphanumeric password set" else "Set an alphanumeric password",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isEditing || !passwordSet) {
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { newValue -> passwordInput = newValue.filter { it.isLetterOrDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (passwordInput.isNotBlank()) { onSetPassword(passwordInput); passwordInput = ""; isEditing = false }
                }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { if (passwordInput.isNotBlank()) { onSetPassword(passwordInput); passwordInput = ""; isEditing = false } },
                    enabled = passwordInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Save") }
                if (passwordSet && isEditing) {
                    FilledTonalButton(
                        onClick = { passwordInput = ""; isEditing = false },
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Cancel") }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { isEditing = true }, shape = RoundedCornerShape(12.dp)) { Text("Change") }
                FilledTonalButton(onClick = { onClearPassword(); passwordInput = "" }, shape = RoundedCornerShape(12.dp)) { Text("Remove") }
            }
        }
    }
}
