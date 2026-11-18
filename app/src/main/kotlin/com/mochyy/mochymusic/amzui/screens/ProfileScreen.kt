package com.mochyy.mochymusic.amzui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.amzui.AmzUiViewModel
import com.mochyy.mochymusic.amzui.components.AmzSwitch
import com.mochyy.mochymusic.amzui.components.IdCard
import com.mochyy.mochymusic.amzui.components.ScreenTopBar
import com.mochyy.mochymusic.amzui.components.StatCard
import com.mochyy.mochymusic.amzui.components.amzPress
import com.mochyy.mochymusic.amzui.data.AmzPrefs
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzDuration
import com.mochyy.mochymusic.amzui.theme.AmzEase
import com.mochyy.mochymusic.amzui.theme.AmzShapes
import com.mochyy.mochymusic.amzui.theme.AmzType
import com.mochyy.mochymusic.amzui.theme.rememberAmzReducedMotion
import com.mochyy.mochymusic.utils.rememberPreference
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    viewModel: AmzUiViewModel,
    profileName: String,
    profilePhoto: String?,
    onAvatarClick: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onImport: () -> Unit,
    onTab: (Int) -> Unit,
) {
    val hero by viewModel.hero.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val autoCapture = rememberPreference(AmzPrefs.AutoCapture, false)
    var flipped by remember { mutableStateOf(false) }
    val reduced = rememberAmzReducedMotion()
    val rotation by animateFloatAsState(
        targetValue = if (flipped && !reduced) 180f else 0f,
        animationSpec = tween(AmzDuration.Sheet, easing = AmzEase),
        label = "cardFlip",
    )
    val scroll = rememberScrollState()

    AmzScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = AmzDimens.screenX),
        ) {
            ScreenTopBar(
                title = "Profile",
                name = profileName,
                photoUri = profilePhoto,
                onAvatarClick = onAvatarClick,
            )
            Spacer(Modifier.height(AmzDimens.childGap))
            IdCard(
                name = profileName.ifBlank { "A" },
                photoUri = profilePhoto,
                modifier = Modifier
                    .amzPress(0.97f) { flipped = !flipped }
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
            )
            Spacer(Modifier.height(AmzDimens.smallGap))
            Text("Not signed in", style = AmzType.tertiary)
            Spacer(Modifier.height(AmzDimens.childGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
            ) {
                StatCard(
                    number = String.format(Locale.US, "%.1f", hero.speed),
                    label = "MB/s now",
                    onClick = onOpenStats,
                    modifier = Modifier.weight(1f),
                    mono = true,
                )
                StatCard(
                    number = active.size.toString(),
                    label = "Active",
                    onClick = onOpenStats,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    number = downloads.size.toString(),
                    label = "Queue",
                    onClick = onOpenStats,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(AmzDimens.childGap))
            ProfileRow(
                label = "View stats ›",
                onClick = onOpenStats,
                underlined = true,
            )
            ProfileRow(label = "Theme", onClick = onOpenSettings)
            ProfileRow(label = "Import local files", onClick = onImport)
            ProfileRow(
                label = "Auto-capture",
                trailing = {
                    AmzSwitch(
                        checked = autoCapture.value,
                        onCheckedChange = { autoCapture.value = it },
                    )
                },
            )
            ProfileRow(
                label = "Clear queue",
                onClick = { downloads.forEach { viewModel.cancel(it.id) } },
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = BuildConfig.VERSION_NAME,
                style = AmzType.version.copy(color = AmzColors.White8),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
            )
            Spacer(Modifier.height(AmzDimens.childGap))
        }
        BottomNav(selected = 3, onSelect = onTab)
    }
}

@Composable
private fun ProfileRow(
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    underlined: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AmzDimens.browserRowHeight)
            .clip(AmzShapes.row)
            .background(AmzColors.Surface2)
            .then(if (onClick != null) Modifier.amzPress(0.97f, onClick) else Modifier)
            .padding(horizontal = AmzDimens.statPadX),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = AmzType.rowTitle.copy(
                textDecoration = if (underlined) TextDecoration.Underline else null,
            ),
        )
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AmzColors.TextTertiary,
            )
        }
    }
}
