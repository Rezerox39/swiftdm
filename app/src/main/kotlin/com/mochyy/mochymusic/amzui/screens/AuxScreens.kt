package com.mochyy.mochymusic.amzui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.amzui.AmzUiViewModel
import com.mochyy.mochymusic.amzui.components.AmzSwitch
import com.mochyy.mochymusic.amzui.components.RoundIconButton
import com.mochyy.mochymusic.amzui.components.StatCard
import com.mochyy.mochymusic.amzui.data.AmzPrefs
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzType
import com.mochyy.mochymusic.utils.rememberPreference
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: AmzUiViewModel,
    onBack: () -> Unit,
) {
    val hero by viewModel.hero.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    AmzScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmzDimens.screenX),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
            ) {
                RoundIconButton(
                    size = AmzDimens.touch44,
                    background = AmzColors.Surface2,
                    contentDescription = "Back",
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = AmzColors.TextPrimary,
                    )
                }
                Text("Stats", style = AmzType.profileTitle)
            }
            Spacer(Modifier.height(AmzDimens.childGap))
            Text(
                text = String.format(Locale.US, "%.1f", hero.speed),
                style = AmzType.monoStat.copy(fontSize = AmzDimens.profileStatNumber),
            )
            Text("Current speed · MB/s", style = AmzType.statLabel)
            Spacer(Modifier.height(AmzDimens.childGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
            ) {
                StatCard(
                    number = downloads.size.toString(),
                    label = "Queue",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    number = active.size.toString(),
                    label = "Active",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(AmzDimens.childGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
            ) {
                StatCard(
                    number = stats.files.toString(),
                    label = "Files",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    number = stats.totalSize,
                    label = "Total size",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    mono = true,
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val pureBlack = rememberPreference(AmzPrefs.PureBlack, true)
    val motion = rememberPreference(AmzPrefs.Motion, true)

    AmzScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmzDimens.screenX),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AmzDimens.childGap),
            ) {
                RoundIconButton(
                    size = AmzDimens.touch44,
                    background = AmzColors.Surface2,
                    contentDescription = "Back",
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = AmzColors.TextPrimary,
                    )
                }
                Text("Settings", style = AmzType.profileTitle)
            }
            Spacer(Modifier.height(AmzDimens.childGap))
            SettingRow(label = "Pure black", checked = pureBlack.value) { pureBlack.value = it }
            SettingRow(label = "Motion", checked = motion.value) { motion.value = it }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AmzDimens.browserRowHeight)
            .padding(horizontal = AmzDimens.smallGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = AmzType.rowTitle)
        AmzSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
