package com.abhi.swiftdm.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhi.swiftdm.ui.navigation.AppTab
import com.abhi.swiftdm.ui.theme.SwiftMotion
import com.abhi.swiftdm.ui.theme.SwiftTheme
import com.abhi.swiftdm.ui.theme.SwiftType

/**
 * Floating translucent tab bar: three tabs, capsule shape, hairline, soft
 * shadow, spring scale on selection.
 */
@Composable
fun FloatingTabBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassBar(
        modifier = modifier
            .padding(horizontal = 28.dp)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
        elevation = 18.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    val tint by animateColorAsState(
        targetValue = if (selected) palette.blue else palette.gray,
        animationSpec = tween(durationMillis = 180),
        label = "tabTint",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = SwiftMotion.standard,
        label = "tabScale",
    )
    Column(
        modifier = modifier
            .height(56.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .pressable(onClickLabel = tab.label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SymbolIcon(
            icon = tab.icon,
            contentDescription = null,
            tint = tint,
            size = 24.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.label,
            style = SwiftType.caption2,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
