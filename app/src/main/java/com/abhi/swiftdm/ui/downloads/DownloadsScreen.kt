package com.abhi.swiftdm.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abhi.swiftdm.ui.components.AppIcon
import com.abhi.swiftdm.ui.components.InsetGroup
import com.abhi.swiftdm.ui.components.LargeTitleCollapseBar
import com.abhi.swiftdm.ui.components.LargeTitleHeader
import com.abhi.swiftdm.ui.components.SegmentedControl
import com.abhi.swiftdm.ui.components.SymbolIcon
import com.abhi.swiftdm.ui.components.pressable
import com.abhi.swiftdm.ui.theme.SwiftShape
import com.abhi.swiftdm.ui.theme.SwiftTheme
import com.abhi.swiftdm.ui.theme.SwiftType
import com.abhi.swiftdm.ui.theme.rememberSwiftHaptics

enum class DownloadFilter(val label: String) {
    All("All"),
    Active("Active"),
    Done("Done"),
}

/**
 * P0 placeholder home. The list itself arrives with the engine in P1 and the
 * real rows in P3; what is real here is the Apple-style chrome: large title,
 * segmented control, inset card, floating tab bar and collapsing title.
 */
@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    onOpenEngineLab: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    var selectedFilter by rememberSaveable { mutableStateOf(DownloadFilter.All) }
    val haptics = rememberSwiftHaptics()
    val palette = SwiftTheme.palette
    val openEngineLab = onOpenEngineLab
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 16
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 132.dp),
        ) {
            item(key = "large-title") {
                LargeTitleHeader(
                    title = "Downloads",
                    subtitle = "Up to 16 segments, resumable, no merge step.",
                )
            }
            item(key = "segmented-control") {
                SegmentedControl(
                    options = DownloadFilter.entries.map { it.label },
                    selectedIndex = selectedFilter.ordinal,
                    onSelect = { index ->
                        haptics.selection()
                        selectedFilter = DownloadFilter.entries[index]
                    },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item(key = "gap") {
                Spacer(modifier = Modifier.height(20.dp))
            }
            item(key = "empty-state") {
                EmptyDownloadsCard(filter = selectedFilter)
            }
            if (openEngineLab != null) {
                item(key = "engine-lab") {
                    InsetGroup(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressable(
                                    onClickLabel = "Open Engine Lab",
                                    onClick = openEngineLab,
                                )
                                .padding(16.dp),
                            verticalAlignment = ComposeAlignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(SwiftShape.iconTile)
                                    .background(palette.blue.copy(alpha = 0.12f)),
                                contentAlignment = ComposeAlignment.Center,
                            ) {
                                SymbolIcon(
                                    icon = AppIcon.Speed,
                                    contentDescription = null,
                                    size = 20.dp,
                                    tint = palette.blue,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Engine Lab",
                                    style = SwiftType.headline,
                                    color = palette.label,
                                )
                                Text(
                                    text = "P1 · live per-segment test bench",
                                    style = SwiftType.footnote,
                                    color = palette.secondaryLabel,
                                )
                            }
                            SymbolIcon(
                                icon = AppIcon.ChevronRight,
                                contentDescription = null,
                                size = 18.dp,
                                tint = palette.tertiaryLabel,
                            )
                        }
                    }
                }
            }
            item(key = "phase-note") {
                Text(
                    text = "P1 build · segmented engine and debug lab",
                    style = SwiftType.caption,
                    color = SwiftTheme.palette.tertiaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                )
            }
        }

        LargeTitleCollapseBar(
            visible = collapsed,
            title = "Downloads",
            modifier = Modifier.align(ComposeAlignment.TopCenter),
        )
    }
}

@Composable
private fun EmptyDownloadsCard(filter: DownloadFilter) {
    val palette = SwiftTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(SwiftShape.cardLarge)
            .background(palette.card)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = ComposeAlignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(SwiftShape.iconTile)
                .background(palette.blue.copy(alpha = 0.12f)),
            contentAlignment = ComposeAlignment.Center,
        ) {
            SymbolIcon(
                icon = AppIcon.Download,
                contentDescription = null,
                size = 30.dp,
                tint = palette.blue,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (filter) {
                DownloadFilter.All -> "No downloads yet"
                DownloadFilter.Active -> "Nothing downloading"
                DownloadFilter.Done -> "Nothing here yet"
            },
            style = SwiftType.headline,
            color = palette.label,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when (filter) {
                DownloadFilter.All -> "Links you add will appear here."
                DownloadFilter.Active -> "Active transfers appear here."
                DownloadFilter.Done -> "Finished downloads appear here."
            },
            style = SwiftType.footnote,
            color = palette.secondaryLabel,
            textAlign = TextAlign.Center,
        )
    }
}
