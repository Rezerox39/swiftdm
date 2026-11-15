package com.abhi.armz.ui.lab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhi.armz.engine.DownloadSnapshot
import com.abhi.armz.engine.DownloadStatus
import com.abhi.armz.engine.SegmentSnapshot
import com.abhi.armz.ui.common.Formatters
import com.abhi.armz.ui.components.AppIcon
import com.abhi.armz.ui.components.Hairline
import com.abhi.armz.ui.components.InsetGroup
import com.abhi.armz.ui.components.SymbolIcon
import com.abhi.armz.ui.components.pressable
import com.abhi.armz.ui.theme.SwiftShape
import com.abhi.armz.ui.theme.SwiftTheme
import com.abhi.armz.ui.theme.SwiftType
import com.abhi.armz.ui.theme.rememberSwiftHaptics

/**
 * P1 debug-only test bench for the segmented engine. It is deliberately plain: the production
 * download UI arrives in P3, this screen exists so real transfers and per-segment behaviour can be
 * observed on a phone without waiting for the full app.
 */
@Composable
fun EngineLabScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EngineLabViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberSwiftHaptics()
    val palette = SwiftTheme.palette

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        LabTopBar(onBack = onBack)
        Hairline()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item(key = "url") {
                UrlCard(
                    url = state.url,
                    onUrlChange = viewModel::onUrlChange,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            item(key = "segments") {
                SegmentsCard(
                    segments = state.segments,
                    onSegmentsChange = {
                        haptics.selection()
                        viewModel.onSegmentsChange(it)
                    },
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            item(key = "actions") {
                ActionsCard(
                    state = state,
                    onStart = {
                        haptics.impact()
                        viewModel.start()
                    },
                    onPause = {
                        haptics.selection()
                        viewModel.pause()
                    },
                    onResume = {
                        haptics.impact()
                        viewModel.resume()
                    },
                    onCancel = {
                        haptics.selection()
                        viewModel.cancel()
                    },
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            val error = state.error
            if (error != null) {
                item(key = "error") {
                    ErrorCard(message = error, modifier = Modifier.padding(top = 14.dp))
                }
            }
            if (state.files.isNotEmpty() || state.snapshot?.status == DownloadStatus.Completed) {
                item(key = "files") {
                    FilesCard(
                        files = state.files,
                        directory = "files/engine-lab",
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
            if (state.snapshot != null || state.busy) {
                item(key = "status") {
                    StatusCard(
                        snapshot = state.snapshot,
                        busy = state.busy,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
            state.snapshot?.segments?.takeIf { it.isNotEmpty() }?.let { segments ->
                item(key = "segment-list") {
                    SegmentListCard(
                        segments = segments,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
            item(key = "note") {
                Text(
                    text = "Debug build only. Lab downloads use a local publisher and stay in the " +
                        "app-private engine-lab folder so you can inspect exactly what the engine wrote. " +
                        "Production downloads publish one file into public Downloads/ARMz.",
                    style = SwiftType.caption,
                    color = palette.tertiaryLabel,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun LabTopBar(onBack: () -> Unit) {
    val palette = SwiftTheme.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .pressable(onClickLabel = "Back", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(
                icon = AppIcon.ArrowBack,
                contentDescription = null,
                size = 22.dp,
                tint = palette.blue,
            )
        }
        Text(
            text = "Engine Lab",
            style = SwiftType.headline,
            color = palette.label,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            text = "P2",
            style = SwiftType.caption2,
            color = palette.tertiaryLabel,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
        )
    }
}

@Composable
private fun UrlCard(
    url: String,
    onUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    val focusManager: FocusManager = LocalFocusManager.current
    InsetGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "URL",
                style = SwiftType.caption2.copy(fontWeight = FontWeight.SemiBold),
                color = palette.secondaryLabel,
            )
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = SwiftType.subhead.copy(color = palette.label),
                singleLine = true,
                cursorBrush = SolidColor(palette.blue),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SwiftShape.card)
                            .background(palette.fill)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        if (url.isEmpty()) {
                            Text(
                                text = "https://example.com/file.zip",
                                style = SwiftType.subhead,
                                color = palette.tertiaryLabel,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Default: proof.ovh.net 100 MB file with ETag and byte ranges",
                style = SwiftType.caption,
                color = palette.tertiaryLabel,
            )
        }
    }
}

@Composable
private fun SegmentsCard(
    segments: Int,
    onSegmentsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    InsetGroup(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Segments",
                    style = SwiftType.headline,
                    color = palette.label,
                )
                Text(
                    text = "Parallel connections, 1–16",
                    style = SwiftType.footnote,
                    color = palette.secondaryLabel,
                )
            }
            StepperButton(
                label = "−",
                contentDescription = "One segment fewer",
                enabled = segments > 1,
                onClick = { onSegmentsChange(-1) },
            )
            Text(
                text = segments.toString(),
                style = SwiftType.title,
                color = palette.label,
                modifier = Modifier
                    .width(44.dp)
                    .padding(horizontal = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepperButton(
                label = "+",
                contentDescription = "One segment more",
                enabled = segments < 16,
                onClick = { onSegmentsChange(1) },
            )
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = SwiftTheme.palette
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(palette.fill)
            .pressable(
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SwiftType.headline,
            color = if (enabled) palette.blue else palette.tertiaryLabel,
        )
    }
}

@Composable
private fun ActionsCard(
    state: EngineLabUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = state.snapshot?.status
    InsetGroup(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                state.busy -> {
                    ActionPill(label = "Starting…", style = ActionStyle.Primary, enabled = false, onClick = {})
                }
                status == null || status.isTerminal -> {
                    ActionPill(label = "Start", style = ActionStyle.Primary, onClick = onStart)
                }
                status == DownloadStatus.Paused || status == DownloadStatus.Failed ||
                    status == DownloadStatus.Cancelled -> {
                    ActionPill(label = "Resume", style = ActionStyle.Primary, onClick = onResume)
                    ActionPill(label = "Cancel", style = ActionStyle.Destructive, onClick = onCancel)
                }
                else -> {
                    ActionPill(
                        label = "Pause",
                        style = ActionStyle.Tinted,
                        enabled = status == DownloadStatus.Probing ||
                            status == DownloadStatus.Downloading ||
                            status == DownloadStatus.Finalizing,
                        onClick = onPause,
                    )
                    ActionPill(label = "Cancel", style = ActionStyle.Destructive, onClick = onCancel)
                }
            }
        }
    }
}

private enum class ActionStyle { Primary, Tinted, Destructive }

@Composable
private fun ActionPill(
    label: String,
    style: ActionStyle,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val palette = SwiftTheme.palette
    val background = when (style) {
        ActionStyle.Primary -> palette.blue
        ActionStyle.Tinted -> palette.blue.copy(alpha = 0.12f)
        ActionStyle.Destructive -> palette.red.copy(alpha = 0.12f)
    }
    val foreground = when (style) {
        ActionStyle.Primary -> Color.White
        ActionStyle.Tinted -> palette.blue
        ActionStyle.Destructive -> palette.red
    }
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(SwiftShape.button)
            .background(if (enabled) background else background.copy(alpha = 0.4f))
            .pressable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SwiftType.subhead.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) foreground else foreground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun StatusCard(
    snapshot: DownloadSnapshot?,
    busy: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    val status = snapshot?.status
    val accent = statusColor(status ?: DownloadStatus.Probing)
    val fraction = snapshot?.fraction ?: 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 220),
        label = "labProgress",
    )

    InsetGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (busy && snapshot == null) palette.blue else accent),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (busy && snapshot == null) "Preparing" else statusLabel(status),
                    style = SwiftType.headline,
                    color = palette.label,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (snapshot == null) "—" else Formatters.percent(fraction),
                    style = SwiftType.title,
                    color = palette.label,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ProgressTrack(fraction = animatedFraction, tint = accent)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = progressText(snapshot),
                style = SwiftType.subhead,
                color = palette.secondaryLabel,
            )
            val finalizeStep = snapshot?.finalizeStep
            if (finalizeStep != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assembling · ${finalizeStep.label}",
                    style = SwiftType.footnote,
                    color = palette.blue,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Metric(label = "Speed", value = Formatters.speed(snapshot?.bytesPerSecond ?: 0L))
                Metric(label = "ETA", value = Formatters.eta(snapshot?.etaSeconds))
                Metric(
                    label = "Mode",
                    value = when {
                        snapshot == null -> "—"
                        snapshot.rangeSupported -> "Ranged"
                        else -> "Single"
                    },
                )
            }
            val fileName = snapshot?.fileName
            if (!fileName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = fileName,
                    style = SwiftType.footnote,
                    color = palette.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val sha = snapshot?.sha256
            if (sha != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SHA-256 ${sha.take(24)}…",
                    style = SwiftType.caption,
                    color = palette.tertiaryLabel,
                )
            }
            val error = snapshot?.error
            if (error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error.message,
                    style = SwiftType.footnote,
                    color = palette.red,
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    val palette = SwiftTheme.palette
    Column {
        Text(
            text = label.uppercase(),
            style = SwiftType.caption2,
            color = palette.tertiaryLabel,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = SwiftType.subhead.copy(fontWeight = FontWeight.Medium),
            color = palette.label,
        )
    }
}

@Composable
private fun SegmentListCard(
    segments: List<SegmentSnapshot>,
    modifier: Modifier = Modifier,
) {
    InsetGroup(modifier = modifier) {
        Column {
            Text(
                text = "Segments (${segments.size})",
                style = SwiftType.caption2.copy(fontWeight = FontWeight.SemiBold),
                color = SwiftTheme.palette.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            )
            segments.sortedBy { it.index }.forEachIndexed { position, segment ->
                if (position > 0) {
                    Hairline(modifier = Modifier.padding(start = 16.dp))
                }
                SegmentRow(segment)
            }
        }
    }
}

@Composable
private fun SegmentRow(segment: SegmentSnapshot) {
    val palette = SwiftTheme.palette
    val accent = when (segment.state) {
        com.abhi.armz.engine.SegmentState.Done -> palette.green
        com.abhi.armz.engine.SegmentState.Failed -> palette.red
        com.abhi.armz.engine.SegmentState.Active -> palette.blue
        com.abhi.armz.engine.SegmentState.Pending -> palette.gray
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (segment.index + 1).toString(),
            style = SwiftType.caption2.copy(fontWeight = FontWeight.SemiBold),
            color = palette.secondaryLabel,
            modifier = Modifier.width(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${Formatters.bytes(segment.start)} – ${Formatters.bytes(segment.end)}",
                style = SwiftType.caption,
                color = palette.secondaryLabel,
            )
            Spacer(modifier = Modifier.height(5.dp))
            ProgressTrack(fraction = segment.fraction, tint = accent, height = 4.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = Formatters.percent(segment.fraction),
            style = SwiftType.caption,
            color = palette.tertiaryLabel,
            modifier = Modifier.width(38.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun ProgressTrack(
    fraction: Float,
    tint: Color,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val palette = SwiftTheme.palette
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(palette.fill),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(tint),
            )
        }
    }
}

@Composable
private fun FilesCard(
    files: List<LabFile>,
    directory: String,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    InsetGroup(modifier = modifier) {
        Column {
            Text(
                text = "ON DISK · $directory",
                style = SwiftType.caption2.copy(fontWeight = FontWeight.SemiBold),
                color = palette.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            )
            if (files.isEmpty()) {
                Text(
                    text = "No files yet. A single .part file appears during transfer and is renamed after publish.",
                    style = SwiftType.footnote,
                    color = palette.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            } else {
                files.forEachIndexed { index, file ->
                    if (index > 0) Hairline(modifier = Modifier.padding(start = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = SwiftType.subhead.copy(fontWeight = FontWeight.Medium),
                                color = palette.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = Formatters.bytes(file.sizeBytes),
                                style = SwiftType.caption,
                                color = palette.secondaryLabel,
                            )
                        }
                        if (file.name.endsWith(".part")) {
                            Text(
                                text = "PART",
                                style = SwiftType.caption2.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.orange,
                            )
                        } else {
                            Text(
                                text = "ONE FILE",
                                style = SwiftType.caption2.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.green,
                            )
                        }
                    }
                }
            }
            Text(
                text = "Reported exactly as on disk, with sizes, so chunked output is impossible to hide.",
                style = SwiftType.caption,
                color = palette.tertiaryLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    val palette = SwiftTheme.palette
    InsetGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Could not start",
                style = SwiftType.subhead.copy(fontWeight = FontWeight.SemiBold),
                color = palette.red,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = SwiftType.footnote,
                color = palette.secondaryLabel,
            )
        }
    }
}

@Composable
private fun statusColor(status: DownloadStatus): Color {
    val palette = SwiftTheme.palette
    return when (status) {
        DownloadStatus.Completed -> palette.green
        DownloadStatus.Failed -> palette.red
        DownloadStatus.Paused, DownloadStatus.Cancelled -> palette.orange
        DownloadStatus.Probing, DownloadStatus.Downloading, DownloadStatus.Finalizing -> palette.blue
        DownloadStatus.Idle -> palette.gray
    }
}

private fun statusLabel(status: DownloadStatus?): String = when (status) {
    null, DownloadStatus.Idle -> "Idle"
    DownloadStatus.Probing -> "Probing"
    DownloadStatus.Downloading -> "Downloading"
    DownloadStatus.Finalizing -> "Assembling"
    DownloadStatus.Paused -> "Paused"
    DownloadStatus.Completed -> "Completed"
    DownloadStatus.Failed -> "Failed"
    DownloadStatus.Cancelled -> "Cancelled"
}

private fun progressText(snapshot: DownloadSnapshot?): String {
    if (snapshot == null) return "Waiting for the probe…"
    val downloaded = Formatters.bytes(snapshot.downloadedBytes)
    return if (snapshot.totalBytes > 0L) {
        "$downloaded of ${Formatters.bytes(snapshot.totalBytes)}"
    } else {
        "$downloaded · size unknown"
    }
}
