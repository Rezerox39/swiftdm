package com.mochyy.mochymusic.amzui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.amzui.AmzUiViewModel
import com.mochyy.mochymusic.amzui.components.AmzChip
import com.mochyy.mochymusic.amzui.components.AmzSearchField
import com.mochyy.mochymusic.amzui.components.FileTile
import com.mochyy.mochymusic.amzui.components.RoundIconButton
import com.mochyy.mochymusic.amzui.components.ScreenTopBar
import com.mochyy.mochymusic.amzui.components.StatCard
import com.mochyy.mochymusic.amzui.components.amzPress
import com.mochyy.mochymusic.amzui.data.AmzDownloadItem
import com.mochyy.mochymusic.amzui.data.AmzFileKind
import com.mochyy.mochymusic.amzui.data.gradient
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzShapes
import com.mochyy.mochymusic.amzui.theme.AmzType

@Composable
fun LibraryScreen(
    viewModel: AmzUiViewModel,
    profileName: String,
    profilePhoto: String?,
    onAvatarClick: () -> Unit,
    onAdd: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onTab: (Int) -> Unit,
) {
    val detected by viewModel.detected.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var grid by remember { mutableStateOf(true) }

    val files = remember {
        listOf(
            AmzDownloadItem(
                id = "lib-iso",
                name = "ubuntu-24.04-desktop.iso",
                kind = AmzFileKind.DISK,
                sizeLabel = "5.9 GB",
                pct = 100f,
                active = false,
                done = true,
            ),
            AmzDownloadItem(
                id = "lib-zip",
                name = "Wallpapers_Pack.zip",
                kind = AmzFileKind.ARCHIVE,
                sizeLabel = "1.2 GB",
                pct = 100f,
                active = false,
                done = true,
            ),
            AmzDownloadItem(
                id = "lib-mp4",
                name = "Lecture_07.mp4",
                kind = AmzFileKind.VIDEO,
                sizeLabel = "148 MB",
                pct = 100f,
                active = false,
                done = true,
            ),
            AmzDownloadItem(
                id = "lib-pdf",
                name = "Report_Q4.pdf",
                kind = AmzFileKind.DOC,
                sizeLabel = "18 MB",
                pct = 100f,
                active = false,
                done = true,
            ),
            AmzDownloadItem(
                id = "lib-apk",
                name = "amz-downloader.apk",
                kind = AmzFileKind.APP,
                sizeLabel = "32 MB",
                pct = 100f,
                active = false,
                done = true,
            ),
        )
    }
    val visibleFiles = files.filter { item ->
        val matchesQuery = item.name.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            "All" -> true
            "Videos" -> item.kind == AmzFileKind.VIDEO
            "Docs" -> item.kind == AmzFileKind.DOC
            "Archives" -> item.kind == AmzFileKind.ARCHIVE
            "Apps" -> item.kind == AmzFileKind.APP
            else -> true
        }
        matchesQuery && matchesFilter
    }

    AmzScreen {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = AmzDimens.screenX)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(
                    title = "Library",
                    name = profileName,
                    photoUri = profilePhoto,
                    onAvatarClick = onAvatarClick,
                    action = {
                        RoundIconButton(
                            size = AmzDimens.touch44,
                            background = AmzColors.Surface2,
                            contentDescription = if (grid) "List layout" else "Grid layout",
                            onClick = { grid = !grid },
                        ) {
                            Icon(
                                imageVector = if (grid) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = null,
                                tint = AmzColors.TextPrimary,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(AmzDimens.childGap))
                AmzSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search files",
                )
                Spacer(Modifier.height(AmzDimens.childGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AmzDimens.chipGap),
                ) {
                    listOf("All", "Videos", "Docs", "Archives", "Apps").forEach { label ->
                        AmzChip(
                            label = label,
                            selected = filter == label,
                            onClick = { filter = label },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(AmzDimens.childGap))
                Text("Captured files", style = AmzType.smallSection)
                Spacer(Modifier.height(AmzDimens.smallGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
                ) {
                    detected.take(3).forEach { file ->
                        Box(
                            modifier = Modifier
                                .size(AmzDimens.capturedTile)
                                .clip(RoundedCornerShape(AmzDimens.capturedTileRadius))
                                .amzPress(0.95f) { onOpenDetails("lib-iso") },
                        ) {
                            FileTile(
                                ext = file.kind.extension,
                                tileSize = AmzDimens.capturedTile,
                                radius = AmzDimens.capturedTileRadius,
                                fontSize = AmzType.sectionTitle.fontSize,
                                gradient = file.kind.gradient(),
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(AmzDimens.capturedBadgeInset)
                                    .size(AmzDimens.capturedBadge)
                                    .clip(AmzShapes.round)
                                    .background(AmzColors.Black60),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Captured from web",
                                    tint = AmzColors.TextPrimary,
                                    modifier = Modifier.size(AmzDimens.capturedGlobe / 2),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(AmzDimens.childGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
                ) {
                    StatCard(
                        number = stats.files.toString(),
                        label = "Files",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        number = stats.totalSize,
                        label = "Total size",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        mono = true,
                    )
                }
                Spacer(Modifier.height(AmzDimens.childGap))
                Text("Files", style = AmzType.smallSection)
                Spacer(Modifier.height(AmzDimens.smallGap))
                if (grid) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
                        horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
                        contentPadding = PaddingValues(bottom = AmzDimens.navHeight + AmzDimens.navBottom),
                    ) {
                        gridItems(visibleFiles) { file ->
                            LibraryGridTile(file, onClick = { onOpenDetails(file.id) })
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AmzDimens.row8),
                        contentPadding = PaddingValues(bottom = AmzDimens.navHeight + AmzDimens.navBottom),
                    ) {
                        items(visibleFiles) { file ->
                            LibraryListTile(file, onClick = { onOpenDetails(file.id) })
                        }
                    }
                }
                BottomNav(selected = 2, onSelect = onTab)
            }
            RoundIconButton(
                size = AmzDimens.libraryImportCircle,
                background = AmzColors.TextPrimary,
                contentDescription = "Add download",
                onClick = onAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = AmzDimens.navHeight + AmzDimens.navBottom + AmzDimens.childGap,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AmzColors.Background,
                )
            }
        }
    }
}

@Composable
private fun LibraryGridTile(
    item: AmzDownloadItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(AmzDimens.libraryTileRadius))
            .amzPress(0.95f, onClick),
    ) {
        FileTile(
            ext = item.kind.extension,
            tileSize = AmzDimens.libraryTile,
            radius = AmzDimens.libraryTileRadius,
            fontSize = AmzType.sectionTitle.fontSize,
            gradient = item.kind.gradient(),
        )
        Spacer(Modifier.height(AmzDimens.smallGap))
        Text(
            text = item.name,
            style = AmzType.rowTitleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = item.sizeLabel, style = AmzType.tertiary)
    }
}

@Composable
private fun LibraryListTile(
    item: AmzDownloadItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AmzDimens.libraryTile)
            .clip(AmzShapes.row)
            .amzPress(0.97f, onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
    ) {
        FileTile(
            ext = item.kind.extension,
            tileSize = AmzDimens.rowTile,
            radius = AmzDimens.rowTileRadius,
            fontSize = AmzType.tertiary.fontSize,
            gradient = item.kind.gradient(),
        )
        Column {
            Text(text = item.name, style = AmzType.rowTitleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = item.sizeLabel, style = AmzType.tertiary)
        }
    }
}
