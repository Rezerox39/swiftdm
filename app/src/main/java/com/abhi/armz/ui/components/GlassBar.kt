package com.abhi.armz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhi.armz.ui.theme.SwiftShape
import com.abhi.armz.ui.theme.SwiftTheme

/**
 * Translucent HIG-style bar: thin tint, 0.5dp hairline and an optional soft
 * shadow. True backdrop sampling (RenderEffect blur on API 31+) needs an
 * offscreen snapshot of the scrolling content and lands with the P6 polish;
 * until then this is the HIG "thin material" fallback and looks correct on
 * every API level.
 */
@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    shape: Shape = SwiftShape.capsule,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = SwiftTheme.palette
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(palette.glassTint)
            .border(
                width = SwiftShape.hairline,
                color = palette.separator.copy(alpha = 0.6f),
                shape = shape,
            ),
        content = content,
    )
}
