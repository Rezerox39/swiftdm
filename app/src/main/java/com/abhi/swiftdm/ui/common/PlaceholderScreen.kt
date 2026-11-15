package com.abhi.swiftdm.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.abhi.swiftdm.ui.components.AppIcon
import com.abhi.swiftdm.ui.components.InsetGroup
import com.abhi.swiftdm.ui.components.LargeTitleHeader
import com.abhi.swiftdm.ui.components.SymbolIcon
import com.abhi.swiftdm.ui.theme.SwiftShape
import com.abhi.swiftdm.ui.theme.SwiftTheme
import com.abhi.swiftdm.ui.theme.SwiftType

/** Placeholder destination for tabs whose full feature ships in a later phase. */
@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    icon: AppIcon,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        LargeTitleHeader(title = title)
        InsetGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SwiftShape.iconTile)
                        .background(palette.blue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(
                        icon = icon,
                        contentDescription = null,
                        size = 22.dp,
                        tint = palette.blue,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = SwiftType.subhead,
                    color = palette.secondaryLabel,
                )
            }
        }
    }
}
