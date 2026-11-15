package com.abhi.swiftdm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = IosColors.BlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF001B3D),
    inversePrimary = IosColors.BlueDark,
    secondary = IosColors.GreenLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCF7E3),
    onSecondaryContainer = Color(0xFF00210B),
    tertiary = IosColors.OrangeLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7C7),
    onTertiaryContainer = Color(0xFF2B1700),
    background = LightAppPalette.background,
    onBackground = LightAppPalette.label,
    surface = LightAppPalette.card,
    onSurface = LightAppPalette.label,
    surfaceVariant = LightAppPalette.gray6,
    onSurfaceVariant = LightAppPalette.secondaryLabel,
    surfaceTint = IosColors.BlueLight,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF2F2F7),
    error = IosColors.RedLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = IosColors.Gray3Light,
    outlineVariant = LightAppPalette.separatorOpaque,
    scrim = LightAppPalette.scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = IosColors.BlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00325C),
    onPrimaryContainer = Color(0xFFD6E9FF),
    inversePrimary = IosColors.BlueLight,
    secondary = IosColors.GreenDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0B3A1C),
    onSecondaryContainer = Color(0xFFDCF7E3),
    tertiary = IosColors.OrangeDark,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF4A2B00),
    onTertiaryContainer = Color(0xFFFFE7C7),
    background = DarkAppPalette.background,
    onBackground = DarkAppPalette.label,
    surface = DarkAppPalette.card,
    onSurface = DarkAppPalette.label,
    surfaceVariant = DarkAppPalette.gray5,
    onSurfaceVariant = DarkAppPalette.secondaryLabel,
    surfaceTint = IosColors.BlueDark,
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1C1C1E),
    error = IosColors.RedDark,
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = IosColors.Gray3Dark,
    outlineVariant = DarkAppPalette.separatorOpaque,
    scrim = DarkAppPalette.scrim,
)

/** Theme tokens that are not part of the Material 3 colour roles. */
object SwiftTheme {
    val palette: AppPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAppPalette.current
}

@Composable
fun SwiftDmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkAppPalette else LightAppPalette
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = SwiftTypeScale,
            shapes = SwiftShapes,
            content = content,
        )
    }
}
