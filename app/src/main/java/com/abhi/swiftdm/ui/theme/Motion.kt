package com.abhi.swiftdm.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

/**
 * One spring curve for the whole app: dampingRatio 0.8, stiffness 300.
 * Material ripple is never used; press feedback is scale + opacity.
 */
object SwiftMotion {
    const val DampingRatio = 0.8f
    const val Stiffness = 300f
    const val PressScale = 0.97f
    const val PressedAlpha = 0.72f

    val standard: SpringSpec<Float> = spring(
        dampingRatio = DampingRatio,
        stiffness = Stiffness,
    )

    val standardDp: SpringSpec<Dp> = spring(
        dampingRatio = DampingRatio,
        stiffness = Stiffness,
    )
}
