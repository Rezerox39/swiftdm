package com.abhi.armz.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhi.armz.ui.theme.SwiftMotion
import com.abhi.armz.ui.theme.SwiftShape
import com.abhi.armz.ui.theme.SwiftTheme
import com.abhi.armz.ui.theme.SwiftType

/**
 * iOS segmented control: rounded track, sliding white thumb, spring animation
 * and no ripple.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(SwiftShape.segment)
            .background(palette.segmentTrack),
    ) {
        val segmentWidth = maxWidth / options.size.toFloat()
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = SwiftMotion.standardDp,
            label = "segmentThumbOffset",
        )

        if (selectedIndex in options.indices) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset + 2.dp, y = 2.dp)
                    .width(segmentWidth - 4.dp)
                    .height(30.dp)
                    .shadow(elevation = 1.5.dp, shape = SwiftShape.segmentThumb)
                    .clip(SwiftShape.segmentThumb)
                    .background(palette.segmentThumb)
                    .border(
                        width = 0.5.dp,
                        color = palette.separator.copy(alpha = 0.45f),
                        shape = SwiftShape.segmentThumb,
                    ),
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) palette.label else palette.secondaryLabel,
                    animationSpec = tween(durationMillis = 160),
                    label = "segmentTextColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { selected = isSelected }
                        .pressable(onClickLabel = label) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = SwiftType.subhead,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
