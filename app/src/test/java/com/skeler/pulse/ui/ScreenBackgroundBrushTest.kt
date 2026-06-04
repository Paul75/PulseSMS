package com.skeler.pulse.ui

import androidx.compose.ui.graphics.SolidColor
import com.skeler.pulse.design.theme.PureBlackBackground
import com.skeler.pulse.design.theme.SerafinaDarkColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenBackgroundBrushTest {
    @Test
    fun should_use_solid_background_when_color_scheme_is_oled_dark() {
        val oledColorScheme = SerafinaDarkColorScheme.copy(
            background = PureBlackBackground,
            surface = PureBlackBackground,
        )

        val screenBackgroundBrush = oledColorScheme.screenBackgroundBrush {
            error("OLED dark mode should not create a gradient brush")
        }

        assertTrue(screenBackgroundBrush is SolidColor)
        assertEquals(PureBlackBackground, (screenBackgroundBrush as SolidColor).value)
    }

    @Test
    fun should_use_gradient_background_when_color_scheme_is_regular_dark() {
        val gradientBrush = SerafinaDarkColorScheme.screenBackgroundBrush {
            SolidColor(SerafinaDarkColorScheme.surfaceContainerLow)
        }

        assertTrue(gradientBrush is SolidColor)
        assertEquals(SerafinaDarkColorScheme.surfaceContainerLow, (gradientBrush as SolidColor).value)
    }
}
