@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mochyy.mochymusic.downloader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.constants.AnimationSpeedKey
import com.mochyy.mochymusic.constants.ElementBlurKey
import com.mochyy.mochymusic.constants.EnableElementBlurAnimatorKey
import com.mochyy.mochymusic.constants.PureBlackKey
import com.mochyy.mochymusic.constants.SelectedThemeColorKey
import com.mochyy.mochymusic.constants.SlimNavBarKey
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.utils.AnimationSpeeds
import com.mochyy.mochymusic.ui.utils.animationSpeedLabel
import com.mochyy.mochymusic.utils.rememberPreference
import kotlin.math.roundToInt

/** The downloader's lean settings page: only Mochy look controls that this app actually uses. */
@Composable
fun DownloaderSettingsScreen(
    viewModel: DownloaderViewModel,
    state: DownloaderUiState,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (animations, setAnimations) = rememberPreference(EnableElementBlurAnimatorKey, true)
    val (speed, setSpeed) = rememberPreference(AnimationSpeedKey, 1f)
    val (elementBlur, setElementBlur) = rememberPreference(ElementBlurKey, true)
    val (pureBlack, setPureBlack) = rememberPreference(PureBlackKey, false)
    val (slimNav, setSlimNav) = rememberPreference(SlimNavBarKey, false)
    val (cardTilt, setCardTilt) = rememberPreference(DownloaderPrefs.CardTiltKey, true)
    val (immersive, setImmersive) = rememberPreference(DownloaderPrefs.ImmersiveKey, true)
    val (autoHide, setAutoHide) = rememberPreference(DownloaderPrefs.ImmersiveAutoHideKey, true)
    val (accent, setAccent) = rememberPreference(SelectedThemeColorKey, DownloaderPrefs.DefaultAccentArgb)

    val speedIndex = AnimationSpeeds.indexOfFirst { it >= speed }.coerceAtLeast(0)
    val accents = listOf(
        DownloaderPrefs.DefaultAccentArgb,
        0xFF8AB4F8.toInt(),
        0xFFF28B82.toInt(),
        0xFF81C995.toInt(),
        0xFFFFD663.toInt(),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SettingCard(title = "Motion", subtitle = "The Mochy animation system, shared with every screen.") {
                SettingSwitch(
                    title = "Animations",
                    subtitle = "Page wipes, card flips and springy navigation.",
                    checked = animations,
                    onChange = setAnimations,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Speed · ${animationSpeedLabel(speed)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = speedIndex.toFloat(),
                    onValueChange = { value ->
                        val index = value.roundToInt().coerceIn(0, AnimationSpeeds.lastIndex)
                        setSpeed(AnimationSpeeds[index])
                    },
                    valueRange = 0f..AnimationSpeeds.lastIndex.toFloat(),
                    steps = AnimationSpeeds.size - 2,
                )
                SettingSwitch(
                    title = "Element blur",
                    subtitle = "Soft blur behind the frosted navigation glass.",
                    checked = elementBlur,
                    onChange = setElementBlur,
                )
            }
        }

        item {
            SettingCard(title = "Look", subtitle = "Small changes that keep the app uncluttered.") {
                SettingSwitch(
                    title = "Pure black",
                    subtitle = "A true black canvas instead of Mochy's dark grey.",
                    checked = pureBlack,
                    onChange = setPureBlack,
                )
                Spacer(Modifier.height(10.dp))
                SettingSwitch(
                    title = "Slim nav pill",
                    subtitle = "A shorter floating pill at the bottom.",
                    checked = slimNav,
                    onChange = setSlimNav,
                )
                Spacer(Modifier.height(14.dp))
                Text("Accent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    accents.forEach { argb ->
                        val selected = argb == accent
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, Color.White, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { setAccent(argb) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingCard(title = "Profile card", subtitle = "The card you set up on first launch.") {
                SettingSwitch(
                    title = "Gyro tilt",
                    subtitle = "Lean the card with the phone's gyroscope.",
                    checked = cardTilt,
                    onChange = setCardTilt,
                )
                OutlinedButton(
                    onClick = onReplayIntro,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Replay greeting")
                }
            }
        }

        item {
            SettingCard(title = "Immersive", subtitle = "A distraction-free view for one download.") {
                SettingSwitch(
                    title = "Immersive download focus",
                    subtitle = "Open the full-screen progress view by tapping a download.",
                    checked = immersive,
                    onChange = setImmersive,
                )
                Spacer(Modifier.height(10.dp))
                SettingSwitch(
                    title = "Auto-hide controls",
                    subtitle = "Fade the controls after a few seconds; tap to bring them back.",
                    checked = autoHide,
                    onChange = setAutoHide,
                )
            }
        }

        item {
            SettingCard(title = "Downloads", subtitle = "Default speed profile for new links.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        item {
            SettingCard(title = "Attribution", subtitle = "Built on the AMz engine and Mochy design language.") {
                Text(
                    text = "Mochy UI © Mochy contributors · GPL-3.0. AMz engine © ARMz contributors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppSwitch(checked = checked, onCheckedChange = onChange)
    }
}
