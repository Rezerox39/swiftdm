package com.abhi.swiftdm.ui

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
import com.abhi.swiftdm.BuildConfig
import com.abhi.swiftdm.ui.common.PlaceholderScreen
import com.abhi.swiftdm.ui.components.AppIcon
import com.abhi.swiftdm.ui.components.FloatingTabBar
import com.abhi.swiftdm.ui.downloads.DownloadsScreen
import com.abhi.swiftdm.ui.lab.EngineLabScreen
import com.abhi.swiftdm.ui.navigation.AppTab
import com.abhi.swiftdm.ui.theme.SwiftTheme

@Composable
fun SwiftDmApp() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Downloads) }
    var showEngineLab by rememberSaveable { mutableStateOf(false) }
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
                AppTab.Downloads -> DownloadsScreen(
                    onOpenEngineLab = if (BuildConfig.DEBUG) {
                        { showEngineLab = true }
                    } else {
                        null
                    },
                )
                AppTab.Browser -> PlaceholderScreen(
                    title = "Browser",
                    message = "A minimal WebView browser with a capsule address bar arrives with URL capture in P5.",
                    icon = AppIcon.Globe,
                )
                AppTab.Settings -> PlaceholderScreen(
                    title = "Settings",
                    message = "Segments, concurrency, Wi-Fi only, speed limit, save location and appearance arrive in P5.",
                    icon = AppIcon.Settings,
                )
            }

            FloatingTabBar(
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
