package com.abhi.swiftdm.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.abhi.swiftdm.ui.theme.SwiftMotion

/**
 * Apple-style press feedback: scales to 0.97 and fades to 72% alpha while
 * pressed, with no Material ripple.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) SwiftMotion.PressScale else 1f,
        animationSpec = SwiftMotion.standard,
        label = "pressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed && enabled) SwiftMotion.PressedAlpha else 1f,
        animationSpec = SwiftMotion.standard,
        label = "pressAlpha",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}
