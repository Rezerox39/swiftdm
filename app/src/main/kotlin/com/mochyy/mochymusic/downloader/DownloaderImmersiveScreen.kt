package com.mochyy.mochymusic.downloader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.ui.component.rememberDeviceTilt
import kotlinx.coroutines.delay

/** A full-bleed focus view for one download: the player's immersive idea, applied to progress. */
@Composable
fun DownloaderImmersiveScreen(
    row: DownloaderRow,
    onClose: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    autoHideControls: Boolean = true,
) {
    val tilt = rememberDeviceTilt(maxDegrees = 9f)
    var controlsVisible by remember { mutableStateOf(true) }
    var revealTick by remember { mutableIntStateOf(0) }
    val progress by animateFloatAsState(
        targetValue = row.fraction,
        animationSpec = tween(420),
        label = "immersive progress",
    )
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(260),
        label = "immersive controls",
    )

    LaunchedEffect(row.id, revealTick, autoHideControls) {
        controlsVisible = true
        if (!autoHideControls) return@LaunchedEffect
        delay(3_400)
        controlsVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .pointerInput(row.id) {
                detectTapGestures {
                    revealTick += 1
                }
            },
    ) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    alpha = 0.12f
                    translationX = -90f
                    translationY = -120f
                    rotationX = tilt.rotationX
                    rotationY = tilt.rotationY
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    alpha = 0.08f
                    translationX = 100f
                    translationY = 100f
                    rotationX = -tilt.rotationX
                    rotationY = -tilt.rotationY
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .graphicsLayer { alpha = controlsAlpha },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Exit immersive view")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Immersive download",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = DownloaderUiMapper.statusLabel(row),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = "${(progress * 100f).toInt()}%",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(248.dp)
                    .graphicsLayer {
                        rotationX = tilt.rotationX * 0.6f
                        rotationY = tilt.rotationY * 0.6f
                    },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 9.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100f).toInt()}%",
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = DownloaderUiMapper.formatBytes(row.downloadedBytes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "of ${DownloaderUiMapper.formatBytes(row.totalBytes)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = row.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${DownloaderUiMapper.formatRate(row.bytesPerSecond)}  ·  ETA ${DownloaderUiMapper.formatEta(row.etaSeconds)}  ·  ${row.segments} parts",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            if (!row.finalizeStep.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = row.finalizeStep.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
            if (!row.error.isNullOrBlank() && row.status == "Failed") {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = row.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
                .graphicsLayer { alpha = controlsAlpha },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (row.isActive) {
                IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                }
                Spacer(Modifier.size(24.dp))
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }
            } else {
                IconButton(onClick = onResume, enabled = !row.isCompleted) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.White)
                }
            }
            if (row.isCompleted) {
                Spacer(Modifier.size(24.dp))
                IconButton(onClick = onOpen) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = Color.White)
                }
            }
        }
    }
}
