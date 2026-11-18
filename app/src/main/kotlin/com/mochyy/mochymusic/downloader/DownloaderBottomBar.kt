package com.mochyy.mochymusic.downloader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.ui.component.ChromeShadow
import com.mochyy.mochymusic.ui.component.FrostedChromeGlass
import com.mochyy.mochymusic.ui.component.GlassState
import com.mochyy.mochymusic.ui.component.NavBarHeight
import com.mochyy.mochymusic.ui.component.NavBarShape
import com.mochyy.mochymusic.ui.component.NavRowPadding
import com.mochyy.mochymusic.ui.component.drawChromeOutline
import com.mochyy.mochymusic.ui.component.navHighlight
import com.mochyy.mochymusic.ui.component.navPillDrag
import com.mochyy.mochymusic.ui.component.press3D
import com.mochyy.mochymusic.ui.component.rememberChromeAccentWash
import com.mochyy.mochymusic.ui.component.rememberChromeModuleGlass
import com.mochyy.mochymusic.ui.component.rememberNavPill

data class DownloaderNavItem(val label: String, val icon: ImageVector)

/**
 * The music player's floating rounded pill, kept as-is down to the frosted glass, stretchy lit pill
 * and drag-to-pick gesture. Only the destinations are downloader ones.
 */
@Composable
fun DownloaderBottomBar(
    items: List<DownloaderNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    glass: GlassState,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slim: Boolean = false,
) {
    val pill = rememberNavPill(selectedIndex)
    val moduleGlass = rememberChromeModuleGlass()
    val density = LocalDensity.current
    val accentWash = rememberChromeAccentWash()
    val glassPill = remember(pill, items.size, density) {
        { bar: Size -> pill.forGlass(bar, with(density) { NavRowPadding.toPx() }, items.size, density) }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
                .height(if (slim) 56.dp else NavBarHeight)
                .press3D(maxTilt = 4f, pressedScale = 0.985f)
                .then(
                    if (moduleGlass?.shadow == false) Modifier else Modifier.dropShadow(NavBarShape, ChromeShadow),
                )
                .clip(NavBarShape)
                .drawWithContent {
                    drawContent()
                    if (moduleGlass?.outline != false) {
                        drawChromeOutline(Offset.Zero, size, cornerRadius = size.height / 2f)
                    }
                },
        ) {
            FrostedChromeGlass(
                state = glass,
                pureBlack = pureBlack,
                accentWash = accentWash,
                modifier = Modifier.fillMaxSize(),
                pill = glassPill,
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NavRowPadding)
                    .navHighlight(pill, items.size) { moduleGlass?.drawsPill != true }
                    .navPillDrag(pill, items.size, selectedIndex, onSelect),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    DownloaderNavTab(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DownloaderNavTab(
    item: DownloaderNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val lit by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180),
        label = "nav lit",
    )
    val pop by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 520f),
        label = "nav pop",
    )
    val interaction = remember { MutableInteractionSource() }
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                    alpha = 0.7f + 0.3f * lit
                },
        )
        Text(
            text = item.label,
            color = tint,
            fontSize = 10.sp,
            modifier = Modifier.graphicsLayer { alpha = 0.75f + 0.25f * lit },
        )
    }
}
