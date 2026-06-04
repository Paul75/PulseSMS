package com.skeler.pulse.design.theme

import androidx.compose.ui.graphics.Color

// ── Legacy Pulse palette (retained for backward compatibility) ──

val PulseBlue = Color(0xFF146C94)
val PulseBlueContainer = Color(0xFFC8ECFF)
val PulseInk = Color(0xFF0D1B2A)
val PulseSlate = Color(0xFF425466)
val PulseMist = Color(0xFFEAF4F8)
val PulseSand = Color(0xFFF5EEDF)
val PulseCopper = Color(0xFF9A5B2A)
val PulseCopperContainer = Color(0xFFFFDBBF)
val PulseMint = Color(0xFF0C7C66)

val PulseBlueDark = Color(0xFF86D2FF)
val PulseBlueContainerDark = Color(0xFF004B68)
val PulseInkDark = Color(0xFFE7F1F6)
val PulseSlateDark = Color(0xFF9CB2C2)
val PulseMistDark = Color(0xFF0E1722)
val PulseSandDark = Color(0xFF17130E)
val PulseCopperDark = Color(0xFFFFB780)
val PulseCopperContainerDark = Color(0xFF6F3F13)

val PureBlackBackground = Color(0xFF000000)
val PureBlackContainerLow = Color(0xFF101013)
val PureBlackContainer = Color(0xFF17171B)
val PureBlackContainerHigh = Color(0xFF1E1E23)
val PureBlackContainerHighest = Color(0xFF28282E)

// ═══════════════════════════════════════════════════════════
// Serafina Design System — Color Tokens
// ═══════════════════════════════════════════════════════════

/**
 * Four curated seed palettes for the Serafina design system.
 * Each variant carries a seed [Color] used to derive a full tonal palette
 * via MaterialKolor's `rememberDynamicColorScheme`.
 */
enum class SerafinaPalette(val seedColor: Color, val label: String) {
    LavenderVolt(Color(0xFF6B4EFF), "Lavender Volt"),
    OrchardGreen(Color(0xFF2E7D32), "Orchard Green"),
    OnyxNoir(Color(0xFF825500), "Onyx Noir"),
    Graphite(Color(0xFF595959), "Graphite"),
}

// ── Static brand fallback schemes (Tier 3) ──
// Generated from LavenderVolt seed (#6B4EFF) via Material Theme Builder.

val SerafinaLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF5B4FC4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF160065),
    secondary = Color(0xFF5D5C72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E0F9),
    onSecondaryContainer = Color(0xFF1A1A2C),
    tertiary = Color(0xFF795369),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EC),
    onTertiaryContainer = Color(0xFF2E1125),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF48454E),
    outline = Color(0xFF79757F),
    outlineVariant = Color(0xFFC9C4D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313034),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFC7BFFF),
    surfaceDim = Color(0xFFDDD8DE),
    surfaceBright = Color(0xFFFFFBFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2F7),
    surfaceContainer = Color(0xFFF1ECF1),
    surfaceContainerHigh = Color(0xFFEBE6EC),
    surfaceContainerHighest = Color(0xFFE6E1E6),
)

val SerafinaDarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF2D1D94),
    primaryContainer = Color(0xFF4335AB),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFC6C4DD),
    onSecondary = Color(0xFF2F2F42),
    secondaryContainer = Color(0xFF454559),
    onSecondaryContainer = Color(0xFFE2E0F9),
    tertiary = Color(0xFFE9B9D3),
    onTertiary = Color(0xFF46263A),
    tertiaryContainer = Color(0xFF5F3C51),
    onTertiaryContainer = Color(0xFFFFD8EC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFC9C4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF48454E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E6),
    inverseOnSurface = Color(0xFF313034),
    inversePrimary = Color(0xFF5B4FC4),
    surfaceDim = Color(0xFF1C1B1F),
    surfaceBright = Color(0xFF434246),
    surfaceContainerLowest = Color(0xFF17161A),
    surfaceContainerLow = Color(0xFF252427),
    surfaceContainer = Color(0xFF29282C),
    surfaceContainerHigh = Color(0xFF333236),
    surfaceContainerHighest = Color(0xFF3E3D41),
)

val GraphiteLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF595959),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF1C1C1C),
    secondary = Color(0xFF5F5F5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF676767),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9E9E9),
    onTertiaryContainer = Color(0xFF202020),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFF474747),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFC6C6C6),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF303030),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFC7C7C7),
    surfaceDim = Color(0xFFDCDCDC),
    surfaceBright = Color(0xFFFCFCFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F6F6),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE4E4E4),
)

val GraphiteDarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFC7C7C7),
    onPrimary = Color(0xFF303030),
    primaryContainer = Color(0xFF595959),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFC9C9C9),
    onSecondary = Color(0xFF313131),
    secondaryContainer = Color(0xFF484848),
    onSecondaryContainer = Color(0xFFE5E5E5),
    tertiary = Color(0xFFD0D0D0),
    onTertiary = Color(0xFF363636),
    tertiaryContainer = Color(0xFF505050),
    onTertiaryContainer = Color(0xFFE9E9E9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141414),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF474747),
    onSurfaceVariant = Color(0xFFC6C6C6),
    outline = Color(0xFF919191),
    outlineVariant = Color(0xFF474747),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF303030),
    inversePrimary = Color(0xFF595959),
    surfaceDim = Color(0xFF141414),
    surfaceBright = Color(0xFF3A3A3A),
    surfaceContainerLowest = Color(0xFF0F0F0F),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF222222),
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF373737),
)
