@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mochyy.mochymusic.downloader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mochyy.mochymusic.ui.component.rememberRevealSeenState
import com.mochyy.mochymusic.ui.component.revealOnAppear
import java.io.File

@Composable
fun DownloadsScreen(
    viewModel: DownloaderViewModel,
    state: DownloaderUiState,
    onOpenBrowser: () -> Unit,
    onOpenImmersive: (DownloaderRow) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val reveal = rememberRevealSeenState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "composer") {
            DownloaderCard(modifier = Modifier.revealOnAppear("downloader-composer", reveal)) {
                Text("Add download", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Paste a direct file link or open the browser") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboard.getText()?.text?.let(viewModel::onInputChanged)
                            },
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste link")
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.submitLink() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Download", modifier = Modifier.padding(start = 6.dp))
                    }
                    Button(onClick = onOpenBrowser) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Browser", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Segments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 4, 8, 16, 32).forEach { count ->
                        FilterChip(
                            selected = state.segments == count,
                            onClick = { viewModel.onSegmentsChanged(count) },
                            label = { Text(if (count == 1) "1×" else "$count×") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        }

        item(key = "summary") {
            DownloaderSummary(state = state, viewModel = viewModel)
        }

        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloaderFilter.entries.forEach { option ->
                    FilterChip(
                        selected = state.filter == option,
                        onClick = { viewModel.onFilterChanged(option) },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }

        if (state.rows.isEmpty()) {
            item(key = "empty") {
                DownloaderCard {
                    Text(
                        text = "No downloads yet. Add a link or capture one in the browser.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        items(items = state.rows, key = { it.id }) { row ->
            DownloadProgressCard(
                row = row,
                revealKey = row.id,
                reveal = reveal,
                onPause = { viewModel.pause(row.id) },
                onResume = { viewModel.resume(row.id) },
                onCancel = { viewModel.cancel(row.id) },
                onRemove = { viewModel.remove(row.id) },
                onOpenImmersive = { onOpenImmersive(row) },
                onOpen = {
                    openDownload(context, row)
                },
            )
        }
    }
}

@Composable
private fun DownloaderCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DownloaderSummary(
    state: DownloaderUiState,
    viewModel: DownloaderViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${state.stats.active} active · ${DownloaderUiMapper.formatRate(state.stats.bytesPerSecond)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Text(
                    text = "${state.stats.completed} done · ${state.stats.failed} failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = viewModel::pauseAll) {
                Icon(Icons.Default.Pause, contentDescription = "Pause all")
            }
            IconButton(onClick = viewModel::resumeAll) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Resume all")
            }
            IconButton(onClick = viewModel::clearFinished) {
                Icon(Icons.Default.Delete, contentDescription = "Clear finished")
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    row: DownloaderRow,
    revealKey: String,
    reveal: com.mochyy.mochymusic.ui.component.RevealSeenState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onOpenImmersive: () -> Unit,
    onOpen: () -> Unit,
) {
    DownloaderCard(
        modifier = Modifier.revealOnAppear("download-$revealKey", reveal),
        onClick = onOpenImmersive,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.title.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    row.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    DownloaderUiMapper.statusLabel(row),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.status == "Failed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (row.isActive) {
                CircularProgressIndicator(
                    progress = { row.fraction },
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 3.dp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { row.fraction },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(DownloaderUiMapper.formatBytes(row.downloadedBytes), fontSize = 11.sp)
            Text("of ${DownloaderUiMapper.formatBytes(row.totalBytes)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(DownloaderUiMapper.formatRate(row.bytesPerSecond), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Text("ETA ${DownloaderUiMapper.formatEta(row.etaSeconds)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (!row.error.isNullOrBlank() && row.status == "Failed") {
            Spacer(Modifier.height(8.dp))
            Text(row.error.orEmpty(), fontSize = 11.sp, color = MaterialTheme.colorScheme.error, maxLines = 2)
        }

        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (row.isActive) {
                IconButton(onClick = onPause) { Icon(Icons.Default.Pause, contentDescription = "Pause") }
                IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
            } else {
                IconButton(onClick = onResume, enabled = !row.isCompleted) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                }
            }
            IconButton(onClick = onOpenImmersive) {
                Icon(Icons.Default.Speed, contentDescription = "Immersive focus")
            }
            if (row.isCompleted) {
                IconButton(onClick = onOpen) { Icon(Icons.Default.OpenInNew, contentDescription = "Open") }
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
        }
    }
}

internal fun openDownload(context: android.content.Context, row: DownloaderRow) {
    val uri = runCatching {
        row.contentUri?.let(Uri::parse)
            ?: row.filePath?.takeIf { it.isNotBlank() }?.let { path ->
                FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))
            }
    }.getOrNull() ?: return

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, row.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Open download")) }
}
