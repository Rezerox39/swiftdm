package com.mochyy.mochymusic.amzui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.mochyy.mochymusic.amzui.components.GlassNavBar
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.AmzDimens
import kotlin.math.max

/** Every canvas screen starts below max(52dp, statusBarInset + 12dp) on pure black. */
@Composable
fun AmzScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = max(AmzDimens.statusTopMin, statusTop + AmzDimens.statusTopExtra)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topPadding)
            .consumeWindowInsets(WindowInsets.statusBars)
            .background(AmzColors.Background),
        content = content,
    )
}

/** Bottom navigation consumes gesture-nav inset before its own 22dp offset. */
@Composable
fun BottomNav(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = AmzDimens.navBottom),
    ) {
        GlassNavBar(selected = selected, onSelect = onSelect)
    }
}

@Composable
fun VerticalSpacer(height: Dp) = Spacer(Modifier.height(height))
