package com.mochyy.mochymusic.amzui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val AmzEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
val AmzOvershoot = CubicBezierEasing(0.2f, 1.4f, 0.4f, 1f)

object AmzDuration {
    const val Splash = 1400
    const val Rise = 650
    const val Drop = 600
    const val Sheet = 600
    const val Pop = 350
    const val Tick = 300
    const val HeroPulse = 3200
    const val LivePulse = 1400
    const val Sheen = 4000
    const val Shimmer = 1800
    const val ShimmerHero = 1600
    const val Switch = 250
    const val Nav = 350
    const val Press = 200
    const val Frame = 16
}

private const val RiseMs = AmzDuration.Rise
private const val DropMs = AmzDuration.Drop
private const val SheetMs = AmzDuration.Sheet
private const val PopMs = AmzDuration.Pop

@Composable
fun rememberAmzReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

enum class AmzEnterKind { Rise, Drop, SheetUp }

/** Rise, drop and sheet-up entrances. With reduced motion, the final state is immediate. */
@Composable
fun Modifier.amzEnter(
    delayMs: Int = 0,
    kind: AmzEnterKind = AmzEnterKind.Rise,
): Modifier = composed {
    val reduced = rememberAmzReducedMotion()
    var started by remember { mutableStateOf(reduced) }
    LaunchedEffect(reduced) {
        if (reduced) {
            started = true
        } else {
            delay(delayMs.toLong())
            started = true
        }
    }
    val target = if (started) 1f else 0f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = when (kind) {
                AmzEnterKind.Rise -> RiseMs
                AmzEnterKind.Drop -> DropMs
                AmzEnterKind.SheetUp -> SheetMs
            },
            easing = AmzEase,
        ),
        label = "amzEnter",
    )
    val offsetY = when (kind) {
        AmzEnterKind.Rise -> (1f - progress) * 16f
        AmzEnterKind.Drop -> -(1f - progress) * 12f
        AmzEnterKind.SheetUp -> (1f - progress) * 60f
    }
    val scale = if (kind == AmzEnterKind.Drop) 0.96f + 0.04f * progress else 1f
    graphicsLayer {
        alpha = progress
        translationY = offsetY.dp.toPx()
        scaleX = scale
        scaleY = scale
    }
}

/** Pop used for icon swaps and badges. */
@Composable
fun Modifier.amzPop(key: Any?): Modifier = composed {
    val reduced = rememberAmzReducedMotion()
    var animatedKey by remember { mutableStateOf(key) }
    var popped by remember { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (reduced) {
            popped = true
        } else {
            popped = false
            delay(AmzDuration.Frame.toLong())
            popped = true
        }
        animatedKey = key
    }
    val target = if (popped) 1f else 0.5f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(PopMs, easing = AmzOvershoot),
        label = "amzPop",
    )
    graphicsLayer {
        alpha = if (popped) 1f else 0f
        scaleX = scale
        scaleY = scale
    }
}

/** Infinite alpha pulse. Used by the hero glow and the live dot. */
@Composable
fun Modifier.amzPulse(durationMs: Int): Modifier = composed {
    val reduced = rememberAmzReducedMotion()
    val infinite = rememberInfiniteTransition(label = "amzPulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "amzPulseAlpha",
    )
    graphicsLayer { this.alpha = if (reduced) 1f else alpha }
}

/** Infinite shimmer band. Reduced motion draws nothing. */
@Composable
fun Modifier.amzShimmer(
    durationMs: Int = 1800,
    highlight: Color = AmzColors.AccentBlueEnd,
): Modifier = composed {
    val reduced = rememberAmzReducedMotion()
    val infinite = rememberInfiniteTransition(label = "amzShimmer")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs),
            repeatMode = RepeatMode.Restart,
        ),
        label = "amzShimmerPhase",
    )
    if (reduced) {
        this
    } else {
        drawBehind {
            val bandWidth = size.width * 0.7f
            val x = -size.width * 1.3f + phase * size.width * 3.6f
            val brush = Brush.linearGradient(
                listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + bandWidth, 0f),
            )
            drawRect(brush = brush)
        }
    }
}

/** Press feedback for every canvas button and link. */
@Composable
fun Modifier.amzPress(
    pressedScale: Float = 0.94f,
    onClick: () -> Unit,
): Modifier = composed {
    val reduced = rememberAmzReducedMotion()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val target = if (pressed && !reduced) pressedScale else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(AmzDuration.Press, easing = AmzEase),
        label = "amzPress",
    )
    this
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/** Pressed lift for carousel tiles. */
@Composable
fun Modifier.amzTilePress(onClick: () -> Unit): Modifier = composed {
    val reduced = rememberAmzReducedMotion()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val targetScale = if (pressed && !reduced) 1.02f else 1f
    val targetY = if (pressed && !reduced) -4f else 0f
    val scale by animateFloatAsState(targetScale, tween(AmzDuration.Press, easing = AmzEase), label = "tileScale")
    val y by animateFloatAsState(targetY, tween(AmzDuration.Press, easing = AmzEase), label = "tileY")
    this
        .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = y.dp.toPx()
        }
}

private fun Context.animationScale(): Float = runCatching {
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}.getOrDefault(1f)
