package com.mochyy.mochymusic.downloader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.ui.component.rememberDeviceTilt
import com.mochyy.mochymusic.ui.screens.setup.onboarding.IdCardAspect
import com.mochyy.mochymusic.ui.screens.setup.onboarding.ProfileIdCard
import kotlinx.coroutines.launch

/**
 * The Mochy profile card with its motion restored: it leans with the gyro, flips on tap and
 * stretches open on a double tap. All the moving values are read inside graphicsLayer/draw
 * lambdas, so none of them recompose the screen at sensor speed.
 */
@Composable
fun DownloaderAnimatedCard(
    username: String,
    photo: Any?,
    cardNumber: String,
    joinedText: String,
    modifier: Modifier = Modifier,
    onPhotoClick: (() -> Unit)? = null,
    enableTilt: Boolean = true,
    showEditHint: Boolean = false,
) {
    val tilt = rememberDeviceTilt(maxDegrees = if (enableTilt) 14f else 0f)
    val flip = remember { Animatable(0f) }
    val expand = remember { Animatable(0f) }
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    var expanded by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    val settle = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 320f)
    val flipSpec = tween<Float>(durationMillis = 620, easing = FastOutSlowInEasing)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IdCardAspect)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val target = if (flip.value < 0.5f) 1f else 0f
                            scope.launch { flip.animateTo(target, flipSpec) }
                        },
                        onDoubleTap = {
                            expanded = !expanded
                            scope.launch {
                                expand.animateTo(if (expanded) 1f else 0f, settle)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pressed = true },
                        onDrag = { change, amount ->
                            change.consume()
                            scope.launch {
                                dragY.snapTo((dragY.value + amount.x * 0.12f).coerceIn(-18f, 18f))
                                dragX.snapTo((dragX.value - amount.y * 0.12f).coerceIn(-14f, 14f))
                            }
                        },
                        onDragEnd = {
                            pressed = false
                            scope.launch {
                                dragX.animateTo(0f, settle)
                                dragY.animateTo(0f, settle)
                            }
                        },
                        onDragCancel = {
                            pressed = false
                            scope.launch {
                                dragX.animateTo(0f, settle)
                                dragY.animateTo(0f, settle)
                            }
                        },
                    )
                }
                .graphicsLayer {
                    rotationY = flip.value * 180f + dragY.value + tilt.rotationY
                    rotationX = dragX.value + tilt.rotationX
                    val scale = 1f + expand.value * 0.035f
                    scaleX = scale
                    scaleY = scale
                    cameraDistance = 18f * density
                },
        ) {
            ProfileIdCard(
                username = username,
                photo = photo,
                cardNumber = cardNumber,
                joinedText = joinedText,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (flip.value < 0.5f) 1f else 0f },
                silver = false,
                showIdCardOf = true,
                sheen = { (0.5f + (dragY.value + tilt.rotationY) / 45f).coerceIn(0f, 1f) },
                onPhotoClick = onPhotoClick,
                showEditHint = showEditHint,
                pressed = { pressed },
            )

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 180f
                        alpha = if (flip.value >= 0.5f) 1f else 0f
                    },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "AMz member",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = cardNumber,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = "Tap to flip back · double tap to extend",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Member since $joinedText", fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Mochy · GPL-3.0",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
