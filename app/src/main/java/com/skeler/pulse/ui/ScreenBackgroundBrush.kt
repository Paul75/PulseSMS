package com.skeler.pulse.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.skeler.pulse.design.theme.PureBlackBackground

internal fun ColorScheme.screenBackgroundBrush(tonalBrush: () -> Brush): Brush {
    if (hasOledDarkBackground()) {
        return SolidColor(background)
    }

    return tonalBrush()
}

private fun ColorScheme.hasOledDarkBackground(): Boolean =
    background == PureBlackBackground && surface == PureBlackBackground
