package com.skeler.pulse.design.component

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A stable Material 3 progress indicator used during retry / recovery states.
 *
 * @param modifier Layout modifier.
 * @param progress Optional determinate progress lambda (null = indeterminate).
 */
@Composable
fun SerafinaProgressIndicator(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier,
        )
    } else {
        LinearProgressIndicator(modifier = modifier)
    }
}
