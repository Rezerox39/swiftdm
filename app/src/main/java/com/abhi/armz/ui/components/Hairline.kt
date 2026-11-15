package com.abhi.armz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.abhi.armz.ui.theme.SwiftShape
import com.abhi.armz.ui.theme.SwiftTheme

/** 0.5dp hairline separator, the iOS divider. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val palette = SwiftTheme.palette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SwiftShape.hairline)
            .background(color ?: palette.separator),
    )
}
