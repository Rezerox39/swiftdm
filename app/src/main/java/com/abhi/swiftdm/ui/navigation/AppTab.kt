package com.abhi.swiftdm.ui.navigation

import com.abhi.swiftdm.ui.components.AppIcon

/** The three top-level destinations. */
enum class AppTab(val label: String, val icon: AppIcon) {
    Downloads(label = "Downloads", icon = AppIcon.Download),
    Browser(label = "Browser", icon = AppIcon.Globe),
    Settings(label = "Settings", icon = AppIcon.Settings),
}
