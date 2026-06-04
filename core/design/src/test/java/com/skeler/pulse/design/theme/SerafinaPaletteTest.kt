package com.skeler.pulse.design.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SerafinaPaletteTest {
    @Test
    fun should_include_graphite_palette_seeded_by_595959() {
        assertEquals(Color(0xFF595959), SerafinaPalette.Graphite.seedColor)
        assertEquals("Graphite", SerafinaPalette.Graphite.label)
    }

    @Test
    fun should_keep_graphite_color_schemes_neutral_gray() {
        val graphiteAccentColors = listOf(
            GraphiteLightColorScheme.primary,
            GraphiteLightColorScheme.primaryContainer,
            GraphiteLightColorScheme.secondary,
            GraphiteLightColorScheme.secondaryContainer,
            GraphiteLightColorScheme.tertiary,
            GraphiteLightColorScheme.tertiaryContainer,
            GraphiteDarkColorScheme.primary,
            GraphiteDarkColorScheme.primaryContainer,
            GraphiteDarkColorScheme.secondary,
            GraphiteDarkColorScheme.secondaryContainer,
            GraphiteDarkColorScheme.tertiary,
            GraphiteDarkColorScheme.tertiaryContainer,
        )

        assertTrue(graphiteAccentColors.all(Color::isNeutralGray))
    }
}

private fun Color.isNeutralGray(): Boolean =
    red == green && green == blue
