package com.abhi.armz.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.abhi.armz.BuildConfig
import com.abhi.armz.ui.components.AppIcon
import com.abhi.armz.ui.components.Hairline
import com.abhi.armz.ui.components.InsetGroup
import com.abhi.armz.ui.components.LargeTitleHeader
import com.abhi.armz.ui.components.SymbolIcon
import com.abhi.armz.ui.components.pressable
import com.abhi.armz.ui.theme.SwiftShape
import com.abhi.armz.ui.theme.SwiftTheme
import com.abhi.armz.ui.theme.SwiftType

@Composable
fun SettingsScreen(
    onOpenEngineLab: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = SwiftTheme.palette
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 120.dp),
    ) {
        LargeTitleHeader(
            title = "Settings",
            subtitle = "Downloads that stay out of your way.",
        )
        InsetGroup {
            InfoRow(
                icon = AppIcon.Folder,
                title = "Save location",
                subtitle = "Downloads/ARMz on Android 10+",
            )
            Hairline(modifier = Modifier.padding(start = 58.dp))
            InfoRow(
                icon = AppIcon.Bolt,
                title = "Engine",
                subtitle = "Up to 16 ranged connections, resumable",
            )
            Hairline(modifier = Modifier.padding(start = 58.dp))
            InfoRow(
                icon = AppIcon.Lock,
                title = "Storage",
                subtitle = "Pending files stay hidden until Assembling finishes",
            )
        }
        Text(
            text = "ABOUT",
            style = SwiftType.caption2,
            color = palette.secondaryLabel,
            modifier = Modifier.padding(start = 36.dp, top = 24.dp, bottom = 8.dp),
        )
        InsetGroup {
            InfoRow(
                icon = AppIcon.Android,
                title = "ARMz",
                subtitle = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
        }
        if (BuildConfig.DEBUG && onOpenEngineLab != null) {
            Text(
                text = "DEVELOPER",
                style = SwiftType.caption2,
                color = palette.secondaryLabel,
                modifier = Modifier.padding(start = 36.dp, top = 24.dp, bottom = 8.dp),
            )
            InsetGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(onClickLabel = "Open Engine Lab", onClick = onOpenEngineLab)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tile(icon = AppIcon.Speed, tint = palette.orange)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Engine Lab",
                            style = SwiftType.headline,
                            color = palette.label,
                        )
                        Text(
                            text = "Raw engine test bench and on-disk diagnostics",
                            style = SwiftType.footnote,
                            color = palette.secondaryLabel,
                        )
                    }
                    SymbolIcon(
                        icon = AppIcon.ChevronRight,
                        contentDescription = null,
                        size = 18.dp,
                        tint = palette.tertiaryLabel,
                    )
                }
            }
        }
        Text(
            text = "P2 preview · foreground service, queue and notifications arrive in P3.",
            style = SwiftType.caption,
            color = palette.tertiaryLabel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 24.dp),
        )
    }
}

@Composable
private fun InfoRow(icon: AppIcon, title: String, subtitle: String) {
    val palette = SwiftTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(icon = icon, tint = palette.blue)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = SwiftType.headline, color = palette.label)
            Text(text = subtitle, style = SwiftType.footnote, color = palette.secondaryLabel)
        }
    }
}

@Composable
private fun Tile(icon: AppIcon, tint: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(SwiftShape.iconTile)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(icon = icon, contentDescription = null, size = 20.dp, tint = tint)
    }
}
