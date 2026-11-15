package com.abhi.armz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhi.armz.BuildConfig
import com.abhi.armz.ui.common.PlaceholderScreen
import com.abhi.armz.ui.components.AppIcon
import com.abhi.armz.ui.components.FloatingTabBar
import com.abhi.armz.ui.downloads.DownloadsScreen
import com.abhi.armz.ui.downloads.DownloadsViewModel
import com.abhi.armz.ui.lab.EngineLabScreen
import com.abhi.armz.ui.navigation.AppTab
import com.abhi.armz.ui.settings.SettingsScreen
import com.abhi.armz.ui.theme.SwiftTheme

@Composable
fun ArmzApp(
    showIntro: Boolean = false,
    downloadsViewModel: DownloadsViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Downloads) }
    var showEngineLab by rememberSaveable { mutableStateOf(false) }
    var introVisible by rememberSaveable { mutableStateOf(showIntro) }
    val palette = SwiftTheme.palette

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        if (showEngineLab) {
            EngineLabScreen(onBack = { showEngineLab = false })
        } else {
            when (selectedTab) {
                AppTab.Downloads -> DownloadsScreen(viewModel = downloadsViewModel)
                AppTab.Browser -> PlaceholderScreen(
                    title = "Browser",
                    message = "A minimal WebView browser with a capsule address bar arrives with URL capture in P5.",
                    icon = AppIcon.Globe,
                )
                AppTab.Settings -> SettingsScreen(
                    onOpenEngineLab = if (BuildConfig.DEBUG) {
                        { showEngineLab = true }
                    } else {
                        null
                    },
                )
            }

            FloatingTabBar(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        AnimatedVisibility(
            visible = introVisible,
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(260)),
        ) {
            ArmzIntro(onFinished = { introVisible = false })
        }
    }
}
