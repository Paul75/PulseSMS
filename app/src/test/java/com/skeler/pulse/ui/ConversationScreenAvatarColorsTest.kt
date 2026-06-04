package com.skeler.pulse.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.skeler.pulse.design.theme.SerafinaDarkColorScheme
import com.skeler.pulse.design.theme.SerafinaLightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val WCAG_MINIMUM_NORMAL_TEXT_CONTRAST_RATIO = 4.5f

class ConversationScreenAvatarColorsTest {
    @Test
    fun should_use_material_color_scheme_token_pairs_for_conversation_avatars() {
        val colors = SerafinaLightColorScheme

        assertEquals(
            listOf(
                ConversationAvatarColors(colors.primaryContainer, colors.onPrimaryContainer),
                ConversationAvatarColors(colors.secondaryContainer, colors.onSecondaryContainer),
                ConversationAvatarColors(colors.tertiaryContainer, colors.onTertiaryContainer),
                ConversationAvatarColors(colors.primary, colors.onPrimary),
                ConversationAvatarColors(colors.secondary, colors.onSecondary),
                ConversationAvatarColors(colors.tertiary, colors.onTertiary),
            ),
            colors.conversationAvatarColorPalette(),
        )
    }

    @Test
    fun should_keep_conversation_avatar_palette_above_wcag_contrast_threshold() {
        val lowContrastRatios = listOf(
            SerafinaLightColorScheme,
            SerafinaDarkColorScheme,
        )
            .flatMap { colorScheme -> colorScheme.conversationAvatarColorPalette() }
            .map { avatarColors -> avatarColors.contentColor.contrastRatioAgainst(avatarColors.containerColor) }
            .filter { contrastRatio -> contrastRatio < WCAG_MINIMUM_NORMAL_TEXT_CONTRAST_RATIO }

        assertEquals(emptyList<Float>(), lowContrastRatios)
    }

    @Test
    fun should_select_stable_conversation_avatar_color_pair_from_title() {
        val colors = SerafinaLightColorScheme
        val selectedColors = colors.conversationAvatarColors("Pulse Support")

        assertTrue(selectedColors in colors.conversationAvatarColorPalette())
        assertEquals(selectedColors, colors.conversationAvatarColors("Pulse Support"))
    }
}

private fun Color.contrastRatioAgainst(background: Color): Float {
    val foregroundLuminance = luminance() + 0.05f
    val backgroundLuminance = background.luminance() + 0.05f
    return maxOf(foregroundLuminance, backgroundLuminance) / minOf(foregroundLuminance, backgroundLuminance)
}
