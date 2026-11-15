package com.abhi.armz.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.abhi.armz.ui.theme.SwiftMotion

/**
 * Apple-style press feedback: scales to 0.97 and fades to 72% alpha while
 * pressed, with no Material ripple.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = pressable(
    enabled = enabled,
    onClickLabel = onClickLabel,
    onLongClickLabel = null,
    onLongClick = null,
    onClick = onClick,
)

/** Same press feedback, plus a long-press action for download rows. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
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
    val feedback = this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    if (onLongClick != null) {
        feedback.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    } else {
        feedback.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
    }
}
