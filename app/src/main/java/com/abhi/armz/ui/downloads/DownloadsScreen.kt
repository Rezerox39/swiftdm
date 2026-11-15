package com.abhi.armz.ui.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abhi.armz.engine.DownloadStatus
import com.abhi.armz.engine.FinalizeStep
import com.abhi.armz.ui.common.Formatters
import com.abhi.armz.ui.components.AppIcon
import com.abhi.armz.ui.components.InsetGroup
import com.abhi.armz.ui.components.LargeTitleCollapseBar
import com.abhi.armz.ui.components.LargeTitleHeader
import com.abhi.armz.ui.components.SegmentedControl
import com.abhi.armz.ui.components.SymbolIcon
import com.abhi.armz.ui.components.pressable
import com.abhi.armz.ui.theme.SwiftMotion
import com.abhi.armz.ui.theme.SwiftShape
import com.abhi.armz.ui.theme.SwiftTheme
import com.abhi.armz.ui.theme.SwiftType
import com.abhi.armz.ui.theme.rememberSwiftHaptics
import kotlinx.coroutines.delay

enum class DownloadFilter(val label: String) {
    All("All"),
    Active("Active"),
    Done("Done"),
}

private val tabularFootnote = SwiftType.footnote.copy(fontFeatureSettings = "tnum")
private val tabularCaption = SwiftType.caption.copy(fontFeatureSettings = "tnum")

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    val haptics = rememberSwiftHaptics()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val items by viewModel.downloads.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var selectedFilter by rememberSaveable { mutableStateOf(DownloadFilter.All) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var addInitialUrl by rememberSaveable { mutableStateOf("") }
    var contextItem by remember { mutableStateOf<DownloadItem?>(null) }
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 16
        }
    }
    val clipboardUrl = clipboardUrl(clipboard.getText()?.text)
    val filtered = when (selectedFilter) {
        DownloadFilter.All -> items
        DownloadFilter.Active -> items.filter { !it.isDone }
        DownloadFilter.Done -> items.filter { it.isDone }
    }.sortedWith(compareBy<DownloadItem> { it.isDone }.thenByDescending { it.createdAt })

    fun openAdd(useClipboard: Boolean) {
        addInitialUrl = if (useClipboard) clipboardUrl else ""
        showAddSheet = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 164.dp),
        ) {
            item(key = "large-title") {
                LargeTitleHeader(
                    title = "Downloads",
                    subtitle = "Assembled into one file, saved to Downloads/ARMz.",
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
            item(key = "gap") { Spacer(modifier = Modifier.height(18.dp)) }

            if (filtered.isEmpty()) {
                item(key = "empty-state") {
                    EmptyDownloads(
                        filter = selectedFilter,
                        hasClipboardLink = clipboardUrl.isNotEmpty(),
                        onPaste = { openAdd(useClipboard = true) },
                        onAdd = { openAdd(useClipboard = false) },
                    )
                }
            } else {
                items(items = filtered, key = { it.id }) { item ->
                    DownloadRow(
                        item = item,
                        onOpen = { openDownload(context, item) },
                        onShare = { shareDownload(context, item) },
                        onResume = { viewModel.resume(item) },
                        onPause = { viewModel.pause(item) },
                        onDelete = {
                            haptics.impact()
                            viewModel.delete(item)
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(item.url))
                            haptics.selection()
                        },
                        onLongPress = { contextItem = item },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        LargeTitleCollapseBar(
            visible = collapsed,
            title = "Downloads",
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 92.dp)
                .size(56.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(palette.blue)
                .pressable(onClickLabel = "Add download") {
                    haptics.selection()
                    openAdd(useClipboard = clipboardUrl.isNotEmpty())
                },
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(
                icon = AppIcon.Add,
                contentDescription = null,
                size = 26.dp,
                tint = Color.White,
            )
        }
    }

    if (showAddSheet) {
        AddDownloadSheet(
            initialUrl = addInitialUrl,
            viewModel = viewModel,
            onDismiss = {
                showAddSheet = false
                viewModel.clearProbe()
            },
        )
    }

    contextItem?.let { item ->
        DownloadActionsSheet(
            item = item,
            onDismiss = { contextItem = null },
            onOpen = {
                contextItem = null
                openDownload(context, item)
            },
            onShare = {
                contextItem = null
                shareDownload(context, item)
            },
            onCopy = {
                clipboard.setText(AnnotatedString(item.url))
                haptics.selection()
                contextItem = null
            },
            onDelete = {
                haptics.impact()
                viewModel.delete(item)
                contextItem = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadRow(
    item: DownloadItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    val haptics = rememberSwiftHaptics()
    var previousStatus by remember(item.id) { mutableStateOf(item.status) }
    var justCompleted by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.status) {
        if (previousStatus != DownloadStatus.Completed && item.status == DownloadStatus.Completed) {
            justCompleted = true
            haptics.impact()
            delay(1500)
            justCompleted = false
        }
        previousStatus = item.status
    }

    val rightLabel = when {
        item.isDone -> "Open"
        item.canPause -> "Pause"
        else -> "Resume"
    }
    val rightIcon = when {
        item.isDone -> AppIcon.OpenInNew
        item.canPause -> AppIcon.Pause
        else -> AppIcon.PlayArrow
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.selection()
                    when {
                        item.isDone -> onOpen()
                        item.canPause -> onPause()
                        else -> onResume()
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    val swipeColor by animateColorAsState(
        targetValue = when (dismissState.targetValue) {
            SwipeToDismissBoxValue.StartToEnd -> palette.blue
            SwipeToDismissBoxValue.EndToStart -> palette.red
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        animationSpec = tween(140),
        label = "swipeColor",
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(SwiftShape.cardLarge)
                    .background(if (swipeColor == Color.Transparent) palette.card else swipeColor)
                    .padding(horizontal = 18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissState.targetValue != SwipeToDismissBoxValue.EndToStart) {
                        SymbolIcon(
                            icon = rightIcon,
                            contentDescription = null,
                            size = 20.dp,
                            tint = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rightLabel,
                            style = SwiftType.caption,
                            color = Color.White,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        Text(
                            text = "Delete",
                            style = SwiftType.caption,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SymbolIcon(
                            icon = AppIcon.Delete,
                            contentDescription = null,
                            size = 20.dp,
                            tint = Color.White,
                        )
                    }
                }
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SwiftShape.cardLarge)
                .background(palette.card)
                .pressable(
                    onClickLabel = rowClickLabel(item),
                    onLongClickLabel = "More actions",
                    onLongClick = onLongPress,
                    onClick = {
                        when {
                            item.isDone -> onOpen()
                            item.canPause -> Unit
                            else -> onResume()
                        }
                    },
                )
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FileTypeTile(fileName = item.fileName)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.fileName.ifBlank { "Download" },
                        style = SwiftType.subhead.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!item.isDone && item.totalBytes > 0L) {
                        Text(
                            text = Formatters.percent(rowFraction(item)),
                            style = tabularCaption.copy(fontWeight = FontWeight.Medium),
                            color = palette.secondaryLabel,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                when {
                    item.status == DownloadStatus.Finalizing -> {
                        AssemblingBar()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Assembling… · ${item.finalizeStep?.label ?: "Verifying"}",
                            style = tabularCaption.copy(fontWeight = FontWeight.Medium),
                            color = palette.blue,
                            maxLines = 1,
                        )
                    }
                    item.isDone -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CompletedCheck(item.isDone, justCompleted)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${Formatters.bytes(item.totalBytes)} · Downloads/ARMz",
                                style = tabularFootnote,
                                color = palette.secondaryLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    item.status == DownloadStatus.Failed || item.status == DownloadStatus.Cancelled -> {
                        Text(
                            text = item.error ?: "Interrupted · tap to resume",
                            style = SwiftType.footnote,
                            color = palette.red,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    item.status == DownloadStatus.Paused -> {
                        MetadataText(item = item, lead = "Paused")
                    }
                    else -> {
                        AnimatedProgress(item = item)
                        Spacer(modifier = Modifier.height(6.dp))
                        MetadataText(item = item, lead = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataText(item: DownloadItem, lead: String?) {
    val palette = SwiftTheme.palette
    val animatedBytes by animateFloatAsState(
        targetValue = item.downloadedBytes.toFloat(),
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "animatedBytes",
    )
    val downloaded = Formatters.bytes(animatedBytes.toLong())
    val total = if (item.totalBytes > 0L) Formatters.bytes(item.totalBytes) else "unknown size"
    val speed = Formatters.speed(item.bytesPerSecond)
    val eta = Formatters.eta(item.etaSeconds)
    val text = buildString {
        if (lead != null) append(lead).append(" · ")
        append(downloaded).append(" of ").append(total)
        if (item.bytesPerSecond > 0L) {
            append(" · ").append(speed)
            if (item.etaSeconds != null) append(" · ").append(eta)
        }
    }
    Text(
        text = text,
        style = tabularFootnote,
        color = palette.secondaryLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AnimatedProgress(item: DownloadItem) {
    val palette = SwiftTheme.palette
    val animatedFraction by animateFloatAsState(
        targetValue = rowFraction(item),
        animationSpec = tween(durationMillis = 540, easing = FastOutSlowInEasing),
        label = "rowProgress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(palette.fill),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(palette.blue),
        )
    }
}

@Composable
private fun AssemblingBar() {
    val palette = SwiftTheme.palette
    val transition = rememberInfiniteTransition(label = "assembling")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "assemblingShift",
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape),
    ) {
        drawRoundRect(color = palette.fill, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
        val band = size.width * 0.36f
        val start = -band + shift * (size.width + band)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, palette.blue, Color.Transparent),
                start = Offset(start, 0f),
                end = Offset(start + band, 0f),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
        )
    }
}

@Composable
private fun CompletedCheck(visible: Boolean, trigger: Boolean) {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        )
    }
    val palette = SwiftTheme.palette
    Canvas(
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer {
                scaleX = if (trigger) 1.08f else 1f
                scaleY = if (trigger) 1.08f else 1f
            },
    ) {
        val path = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.54f)
            lineTo(size.width * 0.43f, size.height * 0.77f)
            lineTo(size.width * 0.84f, size.height * 0.28f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val destination = Path()
        measure.getSegment(0f, measure.length * progress.value.coerceIn(0f, 1f), destination, true)
        drawPath(
            path = destination,
            color = palette.green,
            style = Stroke(
                width = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private enum class FileKind { Video, Audio, Image, Archive, Apk, Document, Generic }

@Composable
private fun FileTypeTile(fileName: String) {
    val palette = SwiftTheme.palette
    val kind = kindOf(fileName)
    val (icon, tint) = when (kind) {
        FileKind.Video -> AppIcon.Movie to palette.purple
        FileKind.Audio -> AppIcon.MusicNote to palette.orange
        FileKind.Image -> AppIcon.Image to palette.teal
        FileKind.Archive -> AppIcon.Archive to palette.orange
        FileKind.Apk -> AppIcon.Android to palette.green
        FileKind.Document -> AppIcon.Description to palette.blue
        FileKind.Generic -> AppIcon.InsertDriveFile to palette.gray
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(SwiftShape.iconTile)
            .background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(icon = icon, contentDescription = null, size = 22.dp, tint = tint)
    }
}

private fun kindOf(fileName: String): FileKind {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "mp4", "mkv", "webm", "mov", "avi", "m4v" -> FileKind.Video
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus" -> FileKind.Audio
        "jpg", "jpeg", "png", "webp", "gif", "heic" -> FileKind.Image
        "zip", "7z", "rar", "tar", "gz", "xz" -> FileKind.Archive
        "apk" -> FileKind.Apk
        "pdf", "epub", "txt", "csv", "json", "html", "xml" -> FileKind.Document
        else -> FileKind.Generic
    }
}

@Composable
private fun EmptyDownloads(
    filter: DownloadFilter,
    hasClipboardLink: Boolean,
    onPaste: () -> Unit,
    onAdd: () -> Unit,
) {
    val palette = SwiftTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(SwiftShape.cardLarge)
            .background(palette.card)
            .padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyIllustration()
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = when (filter) {
                DownloadFilter.All -> "No downloads yet"
                DownloadFilter.Active -> "Nothing downloading"
                DownloadFilter.Done -> "Nothing here yet"
            },
            style = SwiftType.headline,
            color = palette.label,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when (filter) {
                DownloadFilter.All -> "Paste a link or tap + to start. Everything is assembled into one file."
                DownloadFilter.Active -> "Tap + to start a transfer. Progress and Assembling will appear here."
                DownloadFilter.Done -> "Completed downloads land here, ready to open or share."
            },
            style = SwiftType.footnote,
            color = palette.secondaryLabel,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (filter != DownloadFilter.Done) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(SwiftShape.button)
                        .background(palette.blue.copy(alpha = 0.12f))
                        .pressable(onClickLabel = if (hasClipboardLink) "Paste copied link" else "Paste link", onClick = onPaste)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SymbolIcon(
                            icon = if (hasClipboardLink) AppIcon.ContentCopy else AppIcon.Link,
                            contentDescription = null,
                            size = 17.dp,
                            tint = palette.blue,
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = if (hasClipboardLink) "Paste copied link" else "Paste Link",
                            style = SwiftType.subhead.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.blue,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(SwiftShape.button)
                    .background(palette.blue)
                    .pressable(onClickLabel = "Add download", onClick = onAdd)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Add Download",
                    style = SwiftType.subhead.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun EmptyIllustration() {
    val palette = SwiftTheme.palette
    Canvas(modifier = Modifier.size(108.dp)) {
        drawCircle(color = palette.blue.copy(alpha = 0.10f), radius = size.minDimension / 2)
        drawCircle(
            color = palette.blue.copy(alpha = 0.16f),
            radius = size.minDimension / 2.9f,
            center = center,
        )
        val stroke = Stroke(
            width = 5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val shaft = Path().apply {
            moveTo(center.x, size.height * 0.28f)
            lineTo(center.x, size.height * 0.55f)
        }
        val chevron = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.45f)
            lineTo(center.x, size.height * 0.58f)
            lineTo(size.width * 0.62f, size.height * 0.45f)
        }
        val tray = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.65f)
            lineTo(size.width * 0.30f, size.height * 0.72f)
            lineTo(size.width * 0.70f, size.height * 0.72f)
            lineTo(size.width * 0.70f, size.height * 0.65f)
        }
        drawPath(shaft, palette.blue, style = stroke)
        drawPath(chevron, palette.blue, style = stroke)
        drawPath(tray, palette.blue, style = stroke)
        drawCircle(
            color = palette.green.copy(alpha = 0.55f),
            radius = 3.dp.toPx(),
            center = Offset(size.width * 0.78f, size.height * 0.26f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadActionsSheet(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = SwiftTheme.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.background,
        contentColor = palette.label,
        shape = SwiftShape.sheet,
        scrimColor = palette.scrim,
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
        ) {
            Text(
                text = item.fileName.ifBlank { "Download" },
                style = SwiftType.headline,
                color = palette.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
            )
            ActionRow(
                icon = AppIcon.OpenInNew,
                label = "Open",
                enabled = item.contentUri != null,
                tint = palette.blue,
                onClick = onOpen,
            )
            ActionRow(
                icon = AppIcon.Share,
                label = "Share",
                enabled = item.contentUri != null,
                tint = palette.blue,
                onClick = onShare,
            )
            ActionRow(
                icon = AppIcon.ContentCopy,
                label = "Copy link",
                enabled = true,
                tint = palette.blue,
                onClick = onCopy,
            )
            ActionRow(
                icon = AppIcon.Delete,
                label = "Delete",
                enabled = true,
                tint = palette.red,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: AppIcon,
    label: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    val palette = SwiftTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(SwiftShape.card)
            .pressable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(
            icon = icon,
            contentDescription = null,
            size = 21.dp,
            tint = if (enabled) tint else palette.tertiaryLabel,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = SwiftType.body,
            color = if (enabled) palette.label else palette.tertiaryLabel,
        )
    }
}

private fun rowFraction(item: DownloadItem): Float = item.fraction

private fun rowClickLabel(item: DownloadItem): String = when {
    item.isDone -> "Open ${item.fileName}"
    item.canPause -> "Downloading ${item.fileName}"
    else -> "Resume ${item.fileName}"
}

private fun clipboardUrl(text: String?): String {
    val trimmed = text?.trim().orEmpty()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else ""
}

private fun openDownload(context: Context, item: DownloadItem) {
    val uri = item.contentUri?.let(Uri::parse) ?: return
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, item.mimeType ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Open with"))
}

private fun shareDownload(context: Context, item: DownloadItem) {
    val uri = item.contentUri?.let(Uri::parse) ?: return
    val intent = Intent(Intent.ACTION_SEND)
        .setType(item.mimeType ?: "*/*")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share"))
}
