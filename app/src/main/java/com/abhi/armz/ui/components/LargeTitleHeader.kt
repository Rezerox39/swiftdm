package com.abhi.armz.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhi.armz.ui.theme.SwiftTheme
import com.abhi.armz.ui.theme.SwiftType

/**
 * iOS large title. Lives inside the scrolling content so it slides away
 * naturally, and pairs with [LargeTitleCollapseBar] for the pinned inline
 * title.
 */
@Composable
fun LargeTitleHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Text(
            text = title,
            style = SwiftType.largeTitle,
            color = palette.label,
            maxLines = 1,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = SwiftType.footnote,
                color = palette.secondaryLabel,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * Pinned translucent bar that reveals the inline title once the large title
 * scrolls out of view.
 */
@Composable
fun LargeTitleCollapseBar(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 170)) +
            slideInVertically(animationSpec = tween(durationMillis = 170)) { height -> -height / 3 },
        exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
            slideOutVertically(animationSpec = tween(durationMillis = 120)) { height -> -height / 3 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.glassTint)
                .statusBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = SwiftType.headline,
                    color = palette.label,
                    maxLines = 1,
                )
            }
            Hairline()
        }
    }
}
