package com.abhi.swiftdm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.abhi.swiftdm.ui.theme.SwiftShape
import com.abhi.swiftdm.ui.theme.SwiftTheme

/** Grouped inset card used by list and detail screens. */
@Composable
fun InsetGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = SwiftTheme.palette
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(SwiftShape.cardLarge)
            .background(palette.card),
        content = content,
    )
}
