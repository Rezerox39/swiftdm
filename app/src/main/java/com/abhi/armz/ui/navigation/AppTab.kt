package com.abhi.armz.ui.navigation

import com.abhi.armz.ui.components.AppIcon

/** The three top-level destinations. */
enum class AppTab(val label: String, val icon: AppIcon) {
    Downloads(label = "Downloads", icon = AppIcon.Download),
    Browser(label = "Browser", icon = AppIcon.Globe),
    Settings(label = "Settings", icon = AppIcon.Settings),
}
