package com.mochyy.mochymusic.amzui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.amzui.AmzUiViewModel
import com.mochyy.mochymusic.amzui.components.ProgressFill
import com.mochyy.mochymusic.amzui.components.RoundIconButton
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzDuration
import com.mochyy.mochymusic.amzui.theme.AmzShapes
import com.mochyy.mochymusic.amzui.theme.AmzType
import com.mochyy.mochymusic.amzui.theme.amzPulse
import com.mochyy.mochymusic.amzui.theme.cssLinearGradient
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DetailsScreen(
    viewModel: AmzUiViewModel,
    id: String,
    onBack: () -> Unit,
) {
    val item by viewModel.detail(id).collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    AmzScreen {
        if (item == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Download not found", style = AmzType.secondary)
            }
            return@AmzScreen
        }
        val download = item!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AmzDimens.detailsArtHeight)
                    .clip(RoundedCornerShape(AmzDimens.cardRadius))
                    .background(
                        cssLinearGradient(
                            160f,
                            0f to AmzColors.DetailsTop,
                            0.48f to AmzColors.DetailsMiddle,
                            1f to AmzColors.Background,
                        ),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .size(AmzDimens.heroGlow)
                        .align(Alignment.TopCenter)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(AmzColors.DetailsTop)
                        .blur(AmzDimens.detailsArtBlur)
                        .amzPulse(AmzDuration.HeroPulse),
                )
                Text(
                    text = download.kind.extension,
                    style = AmzType.statNumber.copy(
                        fontSize = AmzDimens.detailsIsoText,
                        lineHeight = AmzDimens.detailsIsoText * 1.05f,
                        color = AmzColors.White22,
                    ),
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AmzDimens.screenX, vertical = AmzDimens.screenX),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundIconButton(
                        size = AmzDimens.touch44,
                        background = AmzColors.Black50,
                        contentDescription = "Back",
                        onClick = onBack,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = AmzColors.TextPrimary,
                        )
                    }
                    Box {
                        RoundIconButton(
                            size = AmzDimens.touch44,
                            background = AmzColors.Black50,
                            contentDescription = "More options",
                            onClick = { menuExpanded = true },
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = AmzColors.TextPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy link") },
                                onClick = { menuExpanded = false },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Change folder") },
                                onClick = { menuExpanded = false },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.cancel(id)
                                    onBack()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            horizontal = AmzDimens.sheetPadX,
                            vertical = AmzDimens.sheetPadX,
                        ),
                ) {
                    Text(
                        text = download.name,
                        style = AmzType.detailsTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(AmzDimens.childGap))
                    Text(
                        text = when {
                            download.done -> "Completed"
                            download.active -> String.format(
                                Locale.US,
                                "%.1f MB/s · %d min left · %d connections",
                                download.speedMbps,
                                download.etaMinutes,
                                download.connections.count { it > 0 },
                            )
                            else -> "Paused"
                        },
                        style = AmzType.tabularSecondary.copy(color = AmzColors.DetailsBody),
                    )
                    Spacer(Modifier.height(AmzDimens.smallGap))
                    ProgressFill(progress = download.pct / 100f, hero = true)
                    Spacer(Modifier.height(AmzDimens.microGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${download.pct.roundToInt()}%",
                            style = AmzType.tabularTertiary,
                        )
                        Text(text = download.sizeLabel, style = AmzType.tabularTertiary)
                    }
                    Spacer(Modifier.height(AmzDimens.childGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundIconButton(
                            size = AmzDimens.detailsCancel,
                            background = AmzColors.Surface3,
                            contentDescription = "Cancel download",
                            onClick = {
                                viewModel.cancel(id)
                                onBack()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = AmzColors.Error,
                            )
                        }
                        Spacer(Modifier.width(AmzDimens.childGap))
                        RoundIconButton(
                            size = AmzDimens.detailsMain,
                            background = AmzColors.TextPrimary,
                            contentDescription = if (download.active) "Pause" else "Resume",
                            onClick = {
                                if (download.active) viewModel.pause(id) else viewModel.resume(id)
                            },
                        ) {
                            Icon(
                                imageVector = if (download.active) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = AmzColors.Background,
                                modifier = Modifier.size(AmzDimens.detailsMainIcon),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(AmzDimens.childGap))
            DetailSpecs(download)
            Spacer(Modifier.height(AmzDimens.childGap))
        }
    }
}

@Composable
private fun DetailSpecs(item: com.mochyy.mochymusic.amzui.data.AmzDownloadItem) {
    val rows = listOf(
        "Size" to item.sizeLabel,
        "Type" to item.kind.extension,
        "Segments" to item.connections.size.toString(),
        "Speed" to if (item.active) String.format(Locale.US, "%.1f MB/s", item.speedMbps) else "—",
        "ETA" to if (item.active) "${item.etaMinutes} min" else "—",
        "Connections" to item.connections.count { it > 0 }.toString(),
        "Checksum" to (item.checksum ?: "—"),
        "Source" to item.source,
        "Location" to "Downloads/AMz",
        "Added" to "Just now",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AmzShapes.stat)
            .background(
                cssLinearGradient(
                    145f,
                    0f to AmzColors.SurfaceTop,
                    1f to AmzColors.SurfaceBottom,
                ),
            )
            .border(AmzDimens.borderThin, AmzColors.White7, AmzShapes.stat)
            .padding(AmzDimens.detailsCardPad),
        verticalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
    ) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = AmzType.statLabel)
                Text(
                    text = value.ifBlank { "—" },
                    style = AmzType.tabularSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = AmzDimens.screenX),
                )
            }
        }
        ConnectionBars(item.connections)
    }
}

@Composable
private fun ConnectionBars(values: List<Int>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AmzDimens.detailsConnGap),
    ) {
        values.forEach { value ->
            val height = AmzDimens.detailsConnBarHeight * (value / 100f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(AmzDimens.detailsConnBarRadius))
                    .background(
                        if (value > 0) AmzColors.AccentBlueEnd else AmzColors.Surface3,
                    ),
            )
        }
    }
}
