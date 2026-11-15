package com.abhi.swiftdm.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * iOS system colours, mapped 1:1 to the Human Interface Guidelines values.
 * Light and dark variants are both declared here so screens never branch on
 * the theme themselves.
 */
object IosColors {
    // Accents
    val BlueLight = Color(0xFF007AFF)
    val BlueDark = Color(0xFF0A84FF)
    val GreenLight = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val RedLight = Color(0xFFFF3B30)
    val RedDark = Color(0xFFFF453A)
    val OrangeLight = Color(0xFFFF9500)
    val OrangeDark = Color(0xFFFF9F0A)
    val YellowLight = Color(0xFFFFCC00)
    val YellowDark = Color(0xFFFFD60A)
    val TealLight = Color(0xFF30B0C7)
    val TealDark = Color(0xFF40C8E0)
    val PurpleLight = Color(0xFFAF52DE)
    val PurpleDark = Color(0xFFBF5AF2)

    // System greys
    val GrayLight = Color(0xFF8E8E93)
    val Gray2Light = Color(0xFFAEAEB2)
    val Gray3Light = Color(0xFFC7C7CC)
    val Gray4Light = Color(0xFFD1D1D6)
    val Gray5Light = Color(0xFFE5E5EA)
    val Gray6Light = Color(0xFFF2F2F7)

    val GrayDark = Color(0xFF8E8E93)
    val Gray2Dark = Color(0xFF636366)
    val Gray3Dark = Color(0xFF48484A)
    val Gray4Dark = Color(0xFF3A3A3C)
    val Gray5Dark = Color(0xFF2C2C2E)
    val Gray6Dark = Color(0xFF1C1C1E)
}

/**
 * Colours that Material 3 does not model (grouped backgrounds, hairlines,
 * glass tints). Provided through [LocalAppPalette] so every screen reads the
 * same tokens.
 */
@Immutable
data class AppPalette(
    val isDark: Boolean,
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val teal: Color,
    val purple: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val segmentTrack: Color,
    val segmentThumb: Color,
    val separator: Color,
    val separatorOpaque: Color,
    val fill: Color,
    val fillSecondary: Color,
    val gray: Color,
    val gray2: Color,
    val gray3: Color,
    val gray4: Color,
    val gray5: Color,
    val gray6: Color,
    val glassTint: Color,
    val scrim: Color,
) {
    val cardShadow: Color
        get() = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.08f)
}

val LightAppPalette = AppPalette(
    isDark = false,
    blue = IosColors.BlueLight,
    green = IosColors.GreenLight,
    red = IosColors.RedLight,
    orange = IosColors.OrangeLight,
    yellow = IosColors.YellowLight,
    teal = IosColors.TealLight,
    purple = IosColors.PurpleLight,
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    tertiaryLabel = Color(0x4D3C3C43),
    quaternaryLabel = Color(0x2E3C3C43),
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFFFFFFF),
    segmentTrack = Color(0x14787880),
    segmentThumb = Color(0xFFFFFFFF),
    separator = Color(0x4A3C3C43),
    separatorOpaque = Color(0xFFC6C6C8),
    fill = Color(0x1F767680),
    fillSecondary = Color(0x2E767680),
    gray = IosColors.GrayLight,
    gray2 = IosColors.Gray2Light,
    gray3 = IosColors.Gray3Light,
    gray4 = IosColors.Gray4Light,
    gray5 = IosColors.Gray5Light,
    gray6 = IosColors.Gray6Light,
    glassTint = Color(0xE6F9F9FB),
    scrim = Color(0x66000000),
)

val DarkAppPalette = AppPalette(
    isDark = true,
    blue = IosColors.BlueDark,
    green = IosColors.GreenDark,
    red = IosColors.RedDark,
    orange = IosColors.OrangeDark,
    yellow = IosColors.YellowDark,
    teal = IosColors.TealDark,
    purple = IosColors.PurpleDark,
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    tertiaryLabel = Color(0x4DEBEBF5),
    quaternaryLabel = Color(0x2EEBEBF5),
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    cardElevated = Color(0xFF2C2C2E),
    segmentTrack = Color(0xFF2C2C2E),
    segmentThumb = Color(0xFF636366),
    separator = Color(0x5A545458),
    separatorOpaque = Color(0xFF38383A),
    fill = Color(0x3D787880),
    fillSecondary = Color(0x52787880),
    gray = IosColors.GrayDark,
    gray2 = IosColors.Gray2Dark,
    gray3 = IosColors.Gray3Dark,
    gray4 = IosColors.Gray4Dark,
    gray5 = IosColors.Gray5Dark,
    gray6 = IosColors.Gray6Dark,
    glassTint = Color(0xE61C1C1E),
    scrim = Color(0x99000000),
)

val LocalAppPalette = staticCompositionLocalOf { LightAppPalette }
