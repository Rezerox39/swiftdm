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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.amzui.AmzUiViewModel
import com.mochyy.mochymusic.amzui.components.HeroCard
import com.mochyy.mochymusic.amzui.components.PillBar
import com.mochyy.mochymusic.amzui.components.RowCard
import com.mochyy.mochymusic.amzui.components.ScreenTopBar
import com.mochyy.mochymusic.amzui.components.amzPress
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import com.mochyy.mochymusic.amzui.theme.AmzType

@Composable
fun HomeScreen(
    viewModel: AmzUiViewModel,
    profileName: String,
    profilePhoto: String?,
    onAvatarClick: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onOpenStats: () -> Unit,
    onTab: (Int) -> Unit,
) {
    val hero by viewModel.hero.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    AmzScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = AmzDimens.screenX),
        ) {
            ScreenTopBar(
                title = "Downloads",
                name = profileName,
                photoUri = profilePhoto,
                onAvatarClick = onAvatarClick,
            )
            Spacer(Modifier.height(AmzDimens.childGap))
            PillBar(
                hero = hero,
                activeCount = active.size,
                onDownload = { viewModel.add("https://amz.local/${hero.nextName}") },
            )
            Spacer(Modifier.height(AmzDimens.childGap))
            HeroCard(
                hero = hero,
                onTogglePause = viewModel::togglePause,
                onOpen = { downloads.firstOrNull()?.let { onOpenDetails(it.id) } },
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Active queue", style = AmzType.sectionTitle)
                Text(
                    text = "View stats ›",
                    style = AmzType.secondary.copy(
                        color = AmzColors.TextPrimary,
                        textDecoration = TextDecoration.Underline,
                    ),
                    modifier = Modifier.amzPress(0.94f, onOpenStats),
                )
            }
            Spacer(Modifier.height(AmzDimens.smallGap))
            downloads.forEachIndexed { index, item ->
                RowCard(
                    item = item,
                    rank = index + 2,
                    onToggle = {
                        if (item.active) viewModel.pause(item.id) else viewModel.resume(item.id)
                    },
                    onOpen = { onOpenDetails(item.id) },
                )
                if (index != downloads.lastIndex) Spacer(Modifier.height(AmzDimens.row8))
            }
            Spacer(Modifier.height(AmzDimens.childGap))
        }
        BottomNav(selected = 0, onSelect = onTab)
    }
}
