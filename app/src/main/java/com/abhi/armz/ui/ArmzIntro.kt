package com.abhi.armz.ui

import android.animation.ValueAnimator
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhi.armz.ui.theme.InterFamily
import com.abhi.armz.ui.theme.SwiftTheme
import com.abhi.armz.ui.theme.rememberSwiftHaptics
import kotlinx.coroutines.delay

/**
 * Cold-start intro: the ARMz mark draws itself, then the wordmark springs up and the whole
 * layer cross-fades away to reveal the Downloads large title. Never longer than 1.2 seconds,
 * skippable by tap, and skipped entirely when the system removes animations.
 */
@Composable
fun ArmzIntro(onFinished: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberSwiftHaptics()
    val palette = SwiftTheme.palette
    val reduceMotion = remember {
        !ValueAnimator.areAnimatorsEnabled() ||
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
    }
    var started by remember { mutableStateOf(false) }
    var wordVisible by remember { mutableStateOf(false) }
    val glyphProgress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 660, easing = FastOutSlowInEasing),
        label = "introGlyph",
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (wordVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "introWordAlpha",
    )
    val wordOffset by animateFloatAsState(
        targetValue = if (wordVisible) 0f else 16f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "introWordOffset",
    )

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            onFinished()
            return@LaunchedEffect
        }
        started = true
        delay(230)
        wordVisible = true
        haptics.impact()
        delay(800)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Skip intro",
                onClick = onFinished,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ArmzGlyph(
                progress = if (reduceMotion) 1f else glyphProgress,
                tint = palette.blue,
                modifier = Modifier.size(112.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "ARMz",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                letterSpacing = (-1).sp,
                color = palette.label,
                modifier = Modifier.graphicsLayer {
                    alpha = if (reduceMotion) 1f else wordAlpha
                    translationY = if (reduceMotion) 0f else wordOffset
                },
            )
        }
    }
}

@Composable
private fun ArmzGlyph(
    progress: Float,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 108f
        val stroke = Stroke(
            width = 5.5f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        fun drawTrimmed(path: Path, localProgress: Float) {
            val measure = PathMeasure().apply { setPath(path, false) }
            val destination = Path()
            measure.getSegment(0f, measure.length * localProgress.coerceIn(0f, 1f), destination, true)
            drawPath(destination, tint, style = stroke)
        }
        val shaft = Path().apply {
            moveTo(54f * scale, 27f * scale)
            lineTo(54f * scale, 61f * scale)
        }
        val chevron = Path().apply {
            moveTo(39f * scale, 47f * scale)
            lineTo(54f * scale, 63f * scale)
            lineTo(69f * scale, 47f * scale)
        }
        // Tray rim has a subtle Z step, echoing the ARMz mark.
        val tray = Path().apply {
            moveTo(32f * scale, 70f * scale)
            lineTo(44f * scale, 70f * scale)
            lineTo(44f * scale, 66f * scale)
            lineTo(64f * scale, 66f * scale)
            lineTo(64f * scale, 70f * scale)
            lineTo(76f * scale, 70f * scale)
            moveTo(32f * scale, 73f * scale)
            lineTo(32f * scale, 76f * scale)
            lineTo(76f * scale, 76f * scale)
            lineTo(76f * scale, 73f * scale)
        }
        drawTrimmed(shaft, progress * 1.35f)
        drawTrimmed(chevron, (progress - 0.25f) * 1.6f)
        drawTrimmed(tray, (progress - 0.45f) * 1.8f)
        if (progress >= 1f) {
            drawCircle(
                color = tint.copy(alpha = 0.12f),
                radius = 4.5f * scale,
                center = Offset(54f * scale, 63f * scale),
            )
        }
    }
}
