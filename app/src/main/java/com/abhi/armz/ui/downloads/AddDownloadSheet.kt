package com.abhi.armz.ui.downloads

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhi.armz.ui.common.Formatters
import com.abhi.armz.ui.components.AppIcon
import com.abhi.armz.ui.components.SymbolIcon
import com.abhi.armz.ui.components.pressable
import com.abhi.armz.ui.theme.SwiftMotion
import com.abhi.armz.ui.theme.SwiftShape
import com.abhi.armz.ui.theme.SwiftTheme
import com.abhi.armz.ui.theme.SwiftType
import com.abhi.armz.ui.theme.rememberSwiftHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDownloadSheet(
    initialUrl: String,
    viewModel: DownloadsViewModel,
    onDismiss: () -> Unit,
) {
    val palette = SwiftTheme.palette
    val haptics = rememberSwiftHaptics()
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val probe by viewModel.probe.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var segments by rememberSaveable { mutableIntStateOf(8) }
    val validUrl = url.trim().startsWith("http://") || url.trim().startsWith("https://")
    val canStart = validUrl && !probe.loading && url.trim() == probe.url && probe.result != null

    LaunchedEffect(url) { viewModel.probe(url) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        contentColor = palette.label,
        scrimColor = palette.scrim,
        shape = SwiftShape.sheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(38.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(palette.gray3),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
        ) {
            Text(
                text = "New Download",
                style = SwiftType.title,
                color = palette.label,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            UrlField(
                url = url,
                onUrlChange = { url = it },
                onSubmit = { focusManager.clearFocus() },
            )
            Spacer(modifier = Modifier.height(14.dp))
            ProbeSummary(probe = probe, loading = probe.loading)
            Spacer(modifier = Modifier.height(20.dp))
            SegmentStepper(
                segments = segments,
                onChange = {
                    haptics.selection()
                    segments = it
                },
            )
            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(SwiftShape.button)
                    .background(if (canStart) palette.blue else palette.blue.copy(alpha = 0.35f))
                    .pressable(
                        enabled = canStart,
                        onClickLabel = "Download",
                        onClick = {
                            haptics.impact()
                            viewModel.startDownload(url, segments)
                            onDismiss()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Download",
                    style = SwiftType.button,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(
                    icon = AppIcon.Folder,
                    contentDescription = null,
                    size = 14.dp,
                    tint = palette.tertiaryLabel,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Saves to Downloads/ARMz · Assembling runs before completion",
                    style = SwiftType.caption,
                    color = palette.tertiaryLabel,
                )
            }
        }
    }
}

@Composable
private fun UrlField(
    url: String,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val palette = SwiftTheme.palette
    BasicTextField(
        value = url,
        onValueChange = onUrlChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = SwiftType.body.copy(color = palette.label),
        singleLine = true,
        cursorBrush = SolidColor(palette.blue),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SwiftShape.card)
                    .background(palette.card)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(
                    icon = AppIcon.Link,
                    contentDescription = null,
                    size = 20.dp,
                    tint = if (url.isBlank()) palette.tertiaryLabel else palette.blue,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (url.isEmpty()) {
                        Text(
                            text = "Paste a link",
                            style = SwiftType.body,
                            color = palette.tertiaryLabel,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun ProbeSummary(
    probe: ProbeUiState,
    loading: Boolean,
) {
    val palette = SwiftTheme.palette
    when {
        loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SwiftShape.iconTile)
                        .background(palette.fill),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBar(fraction = 0.7f, height = 13.dp)
                    Spacer(modifier = Modifier.height(7.dp))
                    SkeletonBar(fraction = 0.42f, height = 10.dp)
                }
            }
        }
        probe.error != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon(
                    icon = AppIcon.Error,
                    contentDescription = null,
                    size = 20.dp,
                    tint = palette.red,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = probe.error,
                    style = SwiftType.footnote,
                    color = palette.red,
                )
            }
        }
        probe.result != null -> {
            val result = probe.result!!
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SwiftShape.iconTile)
                        .background(palette.blue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(
                        icon = AppIcon.InsertDriveFile,
                        contentDescription = null,
                        size = 21.dp,
                        tint = palette.blue,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.fileName ?: "download",
                        style = SwiftType.subhead.copy(fontWeight = FontWeight.Medium),
                        color = palette.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(
                                if (result.contentLength > 0L) {
                                    Formatters.bytes(result.contentLength)
                                } else {
                                    "Size unknown"
                                }
                            )
                            append(" · ")
                            append(if (result.rangeSupported) "Ranged" else "Single stream")
                        },
                        style = SwiftType.caption,
                        color = palette.secondaryLabel,
                    )
                }
            }
        }
        else -> {
            Text(
                text = "Paste a direct file link. The size and filename appear after a quick probe.",
                style = SwiftType.footnote,
                color = palette.tertiaryLabel,
            )
        }
    }
}

@Composable
private fun SkeletonBar(fraction: Float, height: androidx.compose.ui.unit.Dp) {
    val palette = SwiftTheme.palette
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonShift",
    )
    val brush = Brush.linearGradient(
        colors = listOf(palette.fill, palette.fillSecondary, palette.fill),
        start = Offset(-260f + shift * 720f, 0f),
        end = Offset(40f + shift * 720f, 0f),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
            .height(height)
            .clip(CircleShape)
            .background(brush),
    )
}

@Composable
private fun SegmentStepper(segments: Int, onChange: (Int) -> Unit) {
    val palette = SwiftTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SwiftShape.card)
            .background(palette.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Segments",
                style = SwiftType.headline,
                color = palette.label,
            )
            Text(
                text = "More connections can be faster on large files",
                style = SwiftType.caption,
                color = palette.secondaryLabel,
            )
        }
        StepperButton(
            label = "−",
            description = "One segment fewer",
            enabled = segments > 1,
            onClick = { onChange(segments - 1) },
        )
        Text(
            text = segments.toString(),
            style = SwiftType.headline,
            color = palette.label,
            modifier = Modifier.width(38.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(
            label = "+",
            description = "One segment more",
            enabled = segments < 16,
            onClick = { onChange(segments + 1) },
        )
    }
}

@Composable
private fun StepperButton(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = SwiftTheme.palette
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(palette.fill)
            .pressable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SwiftType.headline,
            color = if (enabled) palette.blue else palette.tertiaryLabel,
        )
    }
}
