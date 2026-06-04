package com.skeler.pulse.design.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import coil.compose.AsyncImage
import com.skeler.pulse.design.theme.LocalReduceMotion
import com.skeler.pulse.design.theme.SerafinaFullShape

private data class SerafinaAvatarColors(
    val containerColor: Color,
    val contentColor: Color,
)

/**
 * Animated avatar composable with shape-morph between a 12-pointed star
 * and a circle, triggered by [isTyping].
 *
 * - When idle: clipped to a star polygon.
 * - When typing: smoothly morphs to a circle.
 * - If [hasUnread]: draws a `primaryContainer` 2dp stroke ring.
 *
 * Renders [imageUrl] via Coil's [AsyncImage] if non-null; otherwise
 * falls back to centered [initials] text with caller-supplied colors,
 * or a deterministic Material color-scheme palette when no colors are supplied.
 */
@Composable
fun SerafinaAvatar(
    imageUrl: String?,
    initials: String,
    modifier: Modifier = Modifier,
    isTyping: Boolean = false,
    hasUnread: Boolean = false,
    size: Dp = 52.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val reduceMotion = LocalReduceMotion.current

    val basePolygon = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.75f,
            rounding = CornerRounding(0.3f),
        )
    }
    val circlePolygon = remember { RoundedPolygon.circle() }
    val morph = remember(basePolygon, circlePolygon) { Morph(basePolygon, circlePolygon) }

    val targetProgress = if (isTyping) 1f else 0f
    val morphProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (reduceMotion) {
            spring(
                stiffness = Spring.StiffnessHigh,
                dampingRatio = Spring.DampingRatioNoBouncy,
            )
        } else {
            spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy,
            )
        },
        label = "avatar_morph",
    )

    val strokeColor = MaterialTheme.colorScheme.primaryContainer
    val fallbackColors = MaterialTheme.colorScheme.serafinaAvatarColors(initials)
    val resolvedContainerColor = containerColor ?: fallbackColors.containerColor
    val resolvedContentColor = contentColor ?: fallbackColors.contentColor

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (hasUnread) {
                    Modifier.border(
                        width = 2.dp,
                        color = strokeColor,
                        shape = SerafinaFullShape,
                    )
                } else {
                    Modifier
                }
            )
            .drawWithCache {
                // Build the morph path scaled to the component bounds
                val androidPath = morph.toPath(progress = morphProgress)
                val composePath = androidPath.asComposePath()

                // Scale from [-1,1] normalized coords to canvas size
                val matrix = Matrix()
                matrix.scale(this.size.width / 2f, this.size.height / 2f)
                matrix.translate(1f, 1f)
                composePath.transform(matrix)

                onDrawWithContent {
                    clipPath(composePath) {
                        this@onDrawWithContent.drawContent()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(resolvedContainerColor, SerafinaFullShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = resolvedContentColor,
                )
            }
        }
    }
}

private fun ColorScheme.serafinaAvatarColorPalette(): List<SerafinaAvatarColors> =
    listOf(
        SerafinaAvatarColors(primaryContainer, onPrimaryContainer),
        SerafinaAvatarColors(secondaryContainer, onSecondaryContainer),
        SerafinaAvatarColors(tertiaryContainer, onTertiaryContainer),
        SerafinaAvatarColors(primary, onPrimary),
        SerafinaAvatarColors(secondary, onSecondary),
        SerafinaAvatarColors(tertiary, onTertiary),
    )

private fun ColorScheme.serafinaAvatarColors(initials: String): SerafinaAvatarColors {
    val avatarPalette = serafinaAvatarColorPalette()
    return avatarPalette[initials.trim().ifBlank { "#" }.hashCode().mod(avatarPalette.size)]
}
