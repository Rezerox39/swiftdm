package com.mochyy.mochymusic.amzui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.amzui.data.AmzDownloadItem
import com.mochyy.mochymusic.amzui.data.AmzFileKind
import com.mochyy.mochymusic.amzui.data.AmzHeroSnapshot
import com.mochyy.mochymusic.amzui.data.gradient
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzDuration
import com.mochyy.mochymusic.amzui.theme.AmzEase
import com.mochyy.mochymusic.amzui.theme.AmzShapes
import com.mochyy.mochymusic.amzui.theme.AmzType
import com.mochyy.mochymusic.amzui.theme.amzEnter
import com.mochyy.mochymusic.amzui.theme.amzPop
import com.mochyy.mochymusic.amzui.theme.amzPress
import com.mochyy.mochymusic.amzui.theme.amzPulse
import com.mochyy.mochymusic.amzui.theme.amzShimmer
import com.mochyy.mochymusic.amzui.theme.cssLinearGradient
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FileTile(
    ext: String,
    tileSize: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    gradient: Brush,
) {
    Box(
        modifier = modifier
            .size(tileSize)
            .clip(RoundedCornerShape(radius))
            .background(gradient)
            .border(AmzDimens.borderThin, AmzColors.White28, RoundedCornerShape(radius))
            .drawBehind {
                drawRect(brush = Brush.verticalGradient(listOf(AmzColors.White28, Color.Transparent)))
                drawRect(
                    color = AmzColors.Black25,
                    topLeft = Offset(0f, this.size.height * 0.7f),
                    size = Size(this.size.width, this.size.height * 0.3f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ext,
            style = AmzType.statNumber.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.1f,
                color = AmzColors.TextPrimary,
            ),
        )
    }
}

@Composable
fun RoundIconButton(
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressedScale: Float = 0.94f,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(AmzShapes.round)
            .background(background)
            .amzPress(pressedScale, onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
fun AmzChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(AmzDimens.chipHeight)
            .clip(AmzShapes.chip)
            .background(if (selected) AmzColors.TextPrimary else AmzColors.Surface2)
            .amzPress(0.95f, onClick)
            .padding(horizontal = AmzDimens.chipPaddingX),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (selected) AmzType.chipSelected else AmzType.chip,
        )
    }
}

@Composable
fun ProgressFill(
    progress: Float,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(AmzDuration.Tick),
        label = "progressFill",
    )
    val shape = RoundedCornerShape(AmzDimens.progressRadius)
    Box(
        modifier = modifier
            .height(AmzDimens.progressTrack)
            .clip(shape)
            .background(AmzColors.White14),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(shape)
                .background(
                    if (hero) {
                        cssLinearGradient(
                            90f,
                            0f to AmzColors.AccentBlueStart,
                            1f to AmzColors.TextPrimary,
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.White, Color.White))
                    },
                )
                .amzShimmer(
                    durationMs = if (hero) 1600 else 1800,
                    highlight = if (hero) AmzColors.White80 else AmzColors.AccentBlueEnd,
                ),
        )
    }
}

@Composable
fun GlassNavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Icons.Default.Home to "Home",
        Icons.Default.Search to "Browser",
        Icons.Default.Folder to "Files",
        Icons.Default.Person to "Profile",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AmzDimens.navSide)
            .height(AmzDimens.navHeight)
            .clip(AmzShapes.round)
            .background(AmzColors.NavGlass)
            .border(AmzDimens.borderThin, AmzColors.White10, AmzShapes.round)
            .padding(AmzDimens.navPad)
            .amzEnter(delayMs = 300),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, (icon, label) ->
            val isSelected = selected == index
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 1.35f else 1f,
                animationSpec = tween(AmzDuration.Pop, easing = AmzEase),
                label = "navWeight",
            )
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(AmzDimens.navTabHeight)
                    .clip(AmzShapes.round)
                    .background(if (isSelected) AmzColors.NavSelected else Color.Transparent)
                    .amzPress(0.95f) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) AmzColors.TextPrimary else AmzColors.NavInactive,
                    modifier = Modifier.size(AmzDimens.navIcon),
                )
            }
        }
    }
}

@Composable
fun PillBar(
    hero: AmzHeroSnapshot,
    activeCount: Int,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = if (hero.clip) {
        buildAnnotatedString {
            append("Link copied · ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(hero.nextName) }
        }
    } else {
        AnnotatedString("")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AmzDimens.pillBarHeight)
            .clip(AmzShapes.round)
            .background(AmzColors.SurfacePill)
            .border(AmzDimens.borderThin, AmzColors.White8, AmzShapes.round)
            .padding(start = AmzDimens.screenX, end = AmzDimens.microGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hero.clip) {
            Text(
                text = text,
                style = AmzType.chip,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(AmzDimens.gap10))
            Box(
                modifier = Modifier
                    .height(AmzDimens.pillDownloadHeight)
                    .clip(AmzShapes.round)
                    .background(AmzColors.TextPrimary)
                    .amzPress(0.94f, onDownload)
                    .padding(horizontal = AmzDimens.chipPaddingX),
                contentAlignment = Alignment.Center,
            ) {
                Text("Download", style = AmzType.chipSelected)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(AmzDimens.pillDot)
                    .clip(AmzShapes.round)
                    .background(AmzColors.Success)
                    .amzPulse(AmzDuration.LivePulse),
            )
            Spacer(Modifier.width(AmzDimens.smallGap))
            val speed = if (hero.paused) 0f else hero.speed + 3.2f * activeCount
            Text(
                text = String.format(Locale.US, "%.1f MB/s", speed),
                style = AmzType.tabularSecondary.copy(color = AmzColors.TextPrimary),
            )
            Spacer(Modifier.width(AmzDimens.smallGap))
            Text("·", style = AmzType.secondary)
            Spacer(Modifier.width(AmzDimens.smallGap))
            Text(
                text = "$activeCount active",
                style = AmzType.secondary,
            )
        }
    }
}

@Composable
fun RowCard(
    item: AmzDownloadItem,
    rank: Int,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(AmzDimens.rowHeight)
            .fillMaxWidth()
            .clip(AmzShapes.row)
            .background(cardSurface())
            .border(AmzDimens.borderThin, AmzColors.White7, AmzShapes.row),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = AmzDimens.childGap, end = AmzDimens.gap10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmzDimens.gap12),
        ) {
            Text(
                text = rank.toString(),
                style = AmzType.rankNumber,
                modifier = Modifier
                    .width(AmzDimens.rowRankWidth)
                    .amzPress(0.97f, onOpen),
            )
            FileTile(
                ext = item.kind.extension,
                tileSize = AmzDimens.rowTile,
                radius = AmzDimens.rowTileRadius,
                fontSize = AmzType.tertiary.fontSize,
                gradient = item.kind.gradient(),
                modifier = Modifier.amzPress(0.97f, onOpen),
            )
            Column(modifier = Modifier.weight(1f).amzPress(0.97f, onOpen)) {
                Text(
                    text = item.name,
                    style = AmzType.rowTitleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val status = when {
                    item.done -> "Done · ${item.sizeLabel}"
                    item.active -> "${item.pct.roundToInt()}% · ${item.sizeLabel}"
                    item.pct > 0f -> "Paused · ${item.sizeLabel}"
                    else -> "Waiting · ${item.sizeLabel}"
                }
                Text(text = status, style = AmzType.tabularSecondary)
            }
            val icon = when {
                item.done -> Icons.Default.Check
                item.active -> Icons.Default.Pause
                else -> Icons.Default.PlayArrow
            }
            RoundIconButton(
                size = AmzDimens.rowButton,
                background = if (item.done) AmzColors.Success else AmzColors.Surface3,
                contentDescription = if (item.done) "Done" else if (item.active) "Pause" else "Resume",
                onClick = onToggle,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (item.done) AmzColors.TextPrimary else AmzColors.TextPrimary,
                    modifier = Modifier
                        .size(AmzDimens.icon16)
                        .amzPop(item.active),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .height(AmzDimens.rowLine)
                .fillMaxWidth(item.pct / 100f)
                .background(
                    cssLinearGradient(
                        90f,
                        0f to AmzColors.AccentBlueStart,
                        1f to AmzColors.TextPrimary,
                    ),
                ),
        )
    }
}

@Composable
fun HeroCard(
    hero: AmzHeroSnapshot,
    onTogglePause: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = 5.9f * hero.pct / 100f
    val minutes = max(
        1,
        (5.9f * 1024f * (1f - hero.pct / 100f) / max(hero.speed, 1f) / 60f).roundToInt(),
    )
    val stat = if (hero.paused) {
        "Tap play to resume"
    } else {
        String.format(Locale.US, "%.1f MB/s · %d min left", hero.speed, minutes)
    }
    Box(
        modifier = modifier
            .height(AmzDimens.heroHeight)
            .fillMaxWidth()
            .clip(AmzShapes.hero)
            .background(
                cssLinearGradient(
                    150f,
                    0f to AmzColors.HeroStart,
                    0.55f to AmzColors.HeroMiddle,
                    1f to AmzColors.HeroEnd,
                ),
            )
            .border(AmzDimens.borderThin, AmzColors.White10, AmzShapes.hero)
            .drawBehind {
                drawRect(brush = Brush.verticalGradient(listOf(AmzColors.White14, Color.Transparent)))
            },
    ) {
        Box(
            modifier = Modifier
                .size(AmzDimens.heroGlow)
                .offset(x = 0.dp, y = -AmzDimens.heroArtOffsetY)
                .align(Alignment.TopEnd)
                .clip(AmzShapes.round)
                .background(AmzColors.HeroGlow)
                .blur(AmzDimens.heroBlur)
                .amzPulse(AmzDuration.HeroPulse),
        )
        Column(modifier = Modifier.fillMaxSize().padding(AmzDimens.heroPad)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .amzPress(0.95f, onOpen),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
            ) {
                FileTile(
                    ext = "ISO",
                    tileSize = AmzDimens.heroTile,
                    radius = AmzDimens.heroTileRadius,
                    fontSize = AmzType.secondary.fontSize,
                    gradient = AmzFileKind.DISK.gradient(),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hero.paused) "Paused" else "Downloading now",
                        style = AmzType.tertiary.copy(color = AmzColors.HeroLabel),
                    )
                    Text(
                        text = hero.name,
                        style = AmzType.heroTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                RoundIconButton(
                    size = AmzDimens.heroPause,
                    background = AmzColors.TextPrimary,
                    contentDescription = if (hero.paused) "Resume" else "Pause",
                    onClick = onTogglePause,
                ) {
                    Icon(
                        imageVector = if (hero.paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = AmzColors.Background,
                        modifier = Modifier
                            .size(AmzDimens.heroIcon)
                            .amzPop(hero.paused),
                    )
                }
            }
            Spacer(Modifier.height(AmzDimens.gap12))
            ProgressFill(progress = hero.pct / 100f, hero = true)
            Spacer(Modifier.height(AmzDimens.smallGap))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = String.format(Locale.US, "%.1f of 5.9 GB", done),
                    style = AmzType.tabularTertiary.copy(color = AmzColors.HeroStat),
                )
                Text(text = stat, style = AmzType.tabularTertiary.copy(color = AmzColors.HeroStat))
            }
        }
        Sparkline(
            values = hero.speeds,
            paused = hero.paused,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = AmzDimens.heroPad)
                .height(AmzDimens.sparkHeight),
        )
    }
}

@Composable
private fun Sparkline(
    values: List<Float>,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.verticalGradient(
        listOf(AmzColors.SparklineBottom, AmzColors.SparklineTop),
    )
    Canvas(modifier) {
        val count = AmzDimens.sparkBars
        val gap = AmzDimens.sparkGap.toPx()
        val barWidth = (size.width - gap * (count - 1)) / count
        repeat(count) { index ->
            val value = values.getOrElse(index) { 0f }
            val barHeight = if (paused) AmzDimens.rowLine.toPx() else (value * AmzDimens.sparkBarScale).dp.toPx().coerceAtMost(size.height)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(AmzDimens.sparkRadius.toPx()),
            )
        }
    }
}

@Composable
fun cardSurface(): Brush = cssLinearGradient(
    145f,
    0f to AmzColors.SurfaceTop,
    1f to AmzColors.SurfaceBottom,
)

@Composable
fun StatCard(
    number: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(AmzDimens.statCardHeight)
            .clip(AmzShapes.stat)
            .background(cardSurface())
            .border(AmzDimens.borderThin, AmzColors.White7, AmzShapes.stat)
            .amzPress(0.95f, onClick)
            .padding(horizontal = AmzDimens.statPadX, vertical = AmzDimens.statPadY),
    ) {
        Column {
            Text(
                text = number,
                style = if (mono) AmzType.monoStat else AmzType.statNumber,
            )
            Text(text = label, style = AmzType.statLabel)
        }
    }
}

@Composable
fun AmzSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val knob by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(AmzDuration.Switch, easing = AmzEase),
        label = "switchKnob",
    )
    val track by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(AmzDuration.Switch, easing = AmzEase),
        label = "switchTrack",
    )
    val trackColor = if (track > 0.5f) AmzColors.Success else AmzColors.SwitchOffTrack
    Box(
        modifier = modifier
            .size(AmzDimens.switchW, AmzDimens.switchH)
            .clip(AmzShapes.round)
            .background(trackColor)
            .amzPress(0.94f) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .size(AmzDimens.switchKnob)
                .offset(
                    x = AmzDimens.switchInset + (AmzDimens.switchW - AmzDimens.switchKnob - AmzDimens.switchInset * 2) * knob,
                    y = AmzDimens.switchInset,
                )
                .clip(AmzShapes.round)
                .background(AmzColors.TextPrimary),
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = AmzType.sectionTitle)
        if (trailing != null) {
            Spacer(Modifier.width(AmzDimens.smallGap))
            trailing()
        }
    }
}

@Composable
fun IdCard(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    interactiveAvatar: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(AmzDimens.idCardHeight)
            .fillMaxWidth()
            .clip(AmzShapes.stat)
            .background(
                cssLinearGradient(
                    150f,
                    0f to AmzColors.IdStart,
                    1f to AmzColors.IdEnd,
                ),
            )
            .border(AmzDimens.borderThin, AmzColors.White8, AmzShapes.stat)
            .drawBehind {
                drawRect(brush = Brush.verticalGradient(listOf(AmzColors.White10, Color.Transparent)))
            }
            .amzShimmer(durationMs = AmzDuration.Sheen, highlight = AmzColors.White10)
            .padding(AmzDimens.idCardPad),
    ) {
        Text(
            text = "PROFILE USR#2026-1118",
            style = AmzType.monoSmall.copy(color = AmzColors.TextSecondary),
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = AmzDimens.sheetPadEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
        ) {
            Avatar(
                name = name,
                photoUri = photoUri,
                size = AmzDimens.idAvatar,
                interactive = interactiveAvatar,
                onClick = onAvatarClick,
            )
            Column {
                Text(
                    text = name,
                    style = AmzType.statNumber.copy(fontSize = AmzDimens.idName),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "joined at Nov 18, 2026",
                    style = AmzType.tertiary,
                )
            }
        }
        Text(
            text = "AMZ · ID CARD",
            style = AmzType.monoSmall.copy(color = AmzColors.TextTertiary),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .rotate(90f),
        )
    }
}

@Composable
fun Avatar(
    name: String,
    photoUri: String?,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onClick: (() -> Unit)? = null,
    letterSize: androidx.compose.ui.unit.TextUnit = AmzType.rowTitle.fontSize,
) {
    val avatarModifier = modifier
        .size(size)
        .clip(AmzShapes.round)
        .then(
            if (onClick != null) Modifier.amzPress(0.94f, onClick) else Modifier,
        )
    if (!photoUri.isNullOrBlank()) {
        AsyncImage(
            model = photoUri,
            contentDescription = "Profile picture",
            modifier = avatarModifier.background(AmzColors.Surface3),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    } else if (interactive) {
        Box(
            modifier = avatarModifier
                .background(AmzColors.Surface3)
                .drawBehind {
                    drawRoundRect(
                        color = AmzColors.TextTertiary,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.toPx() / 2),
                        style = Stroke(
                            width = AmzDimens.line2.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Choose profile picture",
                tint = AmzColors.TextPrimary,
                modifier = Modifier.size(size / 2),
            )
        }
    } else {
        Box(
            modifier = avatarModifier.background(
                cssLinearGradient(
                    145f,
                    0f to AmzColors.AvatarStart,
                    1f to AmzColors.AvatarEnd,
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(Locale.getDefault()).ifBlank { "A" },
                style = AmzType.statNumber.copy(fontSize = letterSize),
            )
        }
    }
}

@Composable
fun SheetContainer(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(AmzColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = AmzDimens.componentTopPad)
                .clip(AmzShapes.sheet)
                .background(AmzColors.Sheet)
                .padding(
                    top = AmzDimens.sheetPadTop,
                    start = AmzDimens.sheetPadX,
                    end = AmzDimens.sheetPadX,
                    bottom = AmzDimens.sheetPadBottom,
                )
                .amzEnter(kind = com.mochyy.mochymusic.amzui.theme.AmzEnterKind.SheetUp),
            verticalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
        ) {
            Box(
                modifier = Modifier
                    .size(AmzDimens.sheetHandleW, AmzDimens.sheetHandleH)
                    .clip(AmzShapes.round)
                    .background(AmzColors.SheetHandle)
                    .align(Alignment.CenterHorizontally),
            )
            content()
        }
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AmzDimens.buttonHeight)
            .clip(AmzShapes.round)
            .background(
                when {
                    !enabled -> AmzColors.Surface3
                    secondary -> AmzColors.Surface3
                    else -> AmzColors.TextPrimary
                },
            )
            .then(if (enabled) Modifier.amzPress(0.94f, onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AmzType.button.copy(
                color = if (!enabled) AmzColors.TextTertiary else if (secondary) AmzColors.TextPrimary else AmzColors.Background,
            ),
        )
    }
}

@Composable
fun AmzSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AmzDimens.searchHeight)
            .clip(AmzShapes.round)
            .background(AmzColors.Surface2)
            .padding(horizontal = AmzDimens.screenX),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = AmzColors.TextTertiary,
                modifier = Modifier.size(AmzDimens.icon18),
            )
            Spacer(Modifier.width(AmzDimens.smallGap))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = AmzType.secondary,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = AmzType.secondary)
                    inner()
                },
            )
        }
    }
}

@Composable
fun ScreenTopBar(
    title: String,
    name: String,
    photoUri: String?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = AmzType.screenTitle)
        Row(horizontalArrangement = Arrangement.spacedBy(AmzDimens.smallGap)) {
            action?.invoke()
            Avatar(
                name = name,
                photoUri = photoUri,
                size = AmzDimens.avatarTile,
                onClick = onAvatarClick,
            )
        }
    }
}
