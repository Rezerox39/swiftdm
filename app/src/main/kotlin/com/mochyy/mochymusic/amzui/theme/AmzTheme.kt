package com.mochyy.mochymusic.amzui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Every color in the AMz canvas. Screens must not declare colors of their own. */
object AmzColors {
    val Background = Color(0xFF000000)
    val SurfaceTop = Color(0xFF1C1C20)
    val SurfaceBottom = Color(0xFF111113)
    val Surface2 = Color(0xFF1C1C1E)
    val SurfacePill = Color(0xFF141416)
    val Surface3 = Color(0xFF2A2A2D)
    val IdStart = Color(0xFF2A2A2E)
    val IdEnd = Color(0xFF131315)
    val SwitchOffTrack = Color(0xFF3A3A3C)
    val NavSelected = Color(0xFF38383B)
    val Sheet = Color(0xFF0E0E10)
    val SheetHandle = Color(0xFF3A3A3C)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA8A8B0)
    val TextTertiary = Color(0xFF636366)
    val ChipText = Color(0xFFEBEBF5)
    val NavInactive = Color(0xFFC7C7CC)

    val AccentBlueStart = Color(0xFF7DB8FF)
    val AccentBlueEnd = Color(0xFF0A84FF)
    val Success = Color(0xFF30D158)
    val Error = Color(0xFFE5484D)

    val AvatarStart = Color(0xFF5E5CE6)
    val AvatarEnd = Color(0xFF0A84FF)

    val HeroStart = Color(0xFF2A1654)
    val HeroMiddle = Color(0xFF0F0B1F)
    val HeroEnd = Color(0xFF08080A)
    val HeroGlow = Color(0xFFA35CE0)
    val HeroLabel = Color(0xFFB9A8D8)
    val HeroStat = Color(0xFFC4BAD8)
    val DetailsTop = Color(0xFFB068EA)
    val DetailsMiddle = Color(0xFF4A2470)
    val DetailsBody = Color(0xFFC9B8DC)
    val DetailsLabel = Color(0xFFB9B9C0)

    val FileVideoStart = Color(0xFFE5484D)
    val FileVideoEnd = Color(0xFF7A1B2B)
    val FileArchiveStart = Color(0xFFF5A524)
    val FileArchiveEnd = Color(0xFF7A4A00)
    val FileDocStart = Color(0xFF3E63DD)
    val FileDocEnd = Color(0xFF1B2A6B)
    val FileAppStart = Color(0xFF30A46C)
    val FileAppEnd = Color(0xFF0E4B2E)
    val FileDiskStart = Color(0xFFA35CE0)
    val FileDiskEnd = Color(0xFF3B1E5C)
    val FileOtherStart = Color(0xFF636366)
    val FileOtherEnd = Color(0xFF2A2A2D)

    val White7 = Color(0x12FFFFFF)
    val White8 = Color(0x14FFFFFF)
    val White10 = Color(0x1AFFFFFF)
    val White14 = Color(0x24FFFFFF)
    val White16 = Color(0x29FFFFFF)
    val White18 = Color(0x2EFFFFFF)
    val White22 = Color(0x38FFFFFF)
    val White28 = Color(0x47FFFFFF)
    val White80 = Color(0xCCFFFFFF)
    val Black25 = Color(0x40000000)
    val Black50 = Color(0x80000000)
    val Black60 = Color(0x99000000)
    val NavGlass = Color(0xF01C1C1E)
    val BottomFade = Color(0xC7000000)
    val SparklineBottom = Color(0x26A35CE0)
    val SparklineTop = Color(0xCCBE8CFF)
}

/** Every dimension used by the canvas. 1 canvas px = 1 dp. */
object AmzDimens {
    val statusTopMin = 52.dp
    val statusTopExtra = 12.dp
    val screenX = 16.dp
    val childGap = 14.dp
    val smallGap = 8.dp
    val tinyGap = 4.dp

    val touch44 = 44.dp
    val card12 = 12.dp
    val row8 = 8.dp
    val sectionTitle20 = 20.dp

    val cardRadius = 28.dp
    val rowRadius = 22.dp
    val statRadius = 24.dp
    val heroRadius = 32.dp
    val sheetRadius = 36.dp
    val chipRadius = 20.dp
    val pillRadius = 999.dp

    val chipHeight = 40.dp
    val chipPaddingX = 16.dp
    val chipGap = 8.dp
    val buttonHeight = 56.dp
    val fieldHeight = 54.dp
    val searchHeight = 48.dp
    val sheetHandleW = 44.dp
    val sheetHandleH = 5.dp
    val sheetHandleTop = 10.dp
    val sheetPadTop = 40.dp
    val sheetPadX = 24.dp
    val sheetPadBottom = 28.dp

    val switchW = 52.dp
    val switchH = 32.dp
    val switchKnob = 26.dp
    val switchInset = 3.dp

    val navSide = 16.dp
    val navBottom = 22.dp
    val navHeight = 78.dp
    val navPad = 8.dp
    val navTabHeight = 62.dp
    val navIcon = 28.dp

    val splashLogo = 96.dp
    val plusButton = 44.dp
    val avatarTile = 44.dp
    val pillBarHeight = 44.dp
    val pillDownloadHeight = 34.dp
    val pillDot = 8.dp

    val heroHeight = 176.dp
    val heroPad = 18.dp
    val heroTile = 60.dp
    val heroTileRadius = 18.dp
    val heroPause = 48.dp
    val heroIcon = 20.dp
    val heroGlow = 180.dp
    val sparkHeight = 30.dp
    val sparkBars = 18
    val sparkGap = 3.dp
    val sparkRadius = 4.dp
    val progressTrack = 8.dp
    val progressRadius = 4.dp

    val rowHeight = 60.dp
    val rowRankWidth = 18.dp
    val rowTile = 44.dp
    val rowTileRadius = 14.dp
    val rowButton = 40.dp
    val rowLine = 3.dp

    val capturedTile = 128.dp
    val capturedTileRadius = 28.dp
    val capturedBadge = 26.dp
    val capturedBadgeInset = 10.dp
    val capturedGlobe = 30.dp
    val bottomFade = 170.dp

    val detailsArtHeight = 560.dp
    val detailsTopBarTop = 52.dp
    val detailsTitleTop = 372.dp
    val detailsProgressTop = 470.dp
    val detailsControlsTop = 546.dp
    val detailsCardHeight = 124.dp
    val detailsCardPad = 16.dp
    val detailsCancel = 64.dp
    val detailsMain = 88.dp
    val detailsMainIcon = 34.dp
    val detailsConnBarHeight = 32.dp
    val detailsConnBarRadius = 9.dp
    val detailsConnGap = 6.dp
    val detailsIsoText = 150.sp

    val idCardHeight = 150.dp
    val idCardPad = 16.dp
    val idAvatar = 76.dp
    val idLabel = 11.sp
    val idName = 28.sp
    val idNote = 12.sp

    val statCardHeight = 92.dp
    val statPadX = 16.dp
    val statPadY = 14.dp

    val importIcon = 48.dp
    val importIconInner = 24.dp
    val setupTitleTop = 10.dp
    val setupLabelTop = 10.dp
    val setupPictureLabelTop = 8.dp

    val browserBack = 48.dp
    val browserTile = 52.dp
    val browserRowHeight = 64.dp
    val browserButton = 40.dp

    val libraryTile = 72.dp
    val libraryTileRadius = 20.dp
    val libraryImportCircle = 46.dp
    val feedbackIcon = 32.dp

    val profileStatNumber = 38.sp
    val profileVersion = 44.sp
}

/** Inter family bundled in res/font, including the 800 weight used by every title. */
val AmzInter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private val titleSpacing = (-0.03).em
private val monoSpacing = (-0.04).em
private val tabular = TextStyle(fontFeatureSettings = "tnum")

object AmzType {
    val screenTitle = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = titleSpacing,
    )
    val onboardingTitle = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        lineHeight = 40.sp,
        letterSpacing = titleSpacing,
    )
    val profileTitle = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 33.sp,
        letterSpacing = titleSpacing,
    )
    val detailsTitle = profileTitle
    val sectionTitle = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = titleSpacing,
    )
    val smallSection = sectionTitle.copy(fontSize = 20.sp, lineHeight = 24.sp)
    val rowTitle = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        letterSpacing = titleSpacing,
    )
    val rowTitleSmall = rowTitle.copy(fontSize = 16.sp)
    val body = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp,
    )
    val secondary = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = AmzColors.TextSecondary,
    )
    val tertiary = secondary.copy(fontSize = 13.sp, lineHeight = 17.sp, color = AmzColors.TextTertiary)
    val chip = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = AmzColors.ChipText,
    )
    val chipSelected = chip.copy(fontWeight = FontWeight.SemiBold, color = AmzColors.Background)
    val button = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 21.sp,
    )
    val statNumber = TextStyle(
        fontFamily = AmzInter,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = titleSpacing,
    )
    val rankNumber = statNumber.copy(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = AmzColors.TextSecondary,
    )
    val statLabel = secondary.copy(fontSize = 14.sp)
    val monoStat = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = monoSpacing,
        fontFeatureSettings = "tnum",
    )
    val monoSmall = monoStat.copy(fontSize = 13.sp, lineHeight = 16.sp)
    val version = statNumber.copy(fontSize = 44.sp, lineHeight = 48.sp)
    val tabularNumber = statNumber.copy(fontFeatureSettings = "tnum")
    val tabularSecondary = secondary.copy(fontFeatureSettings = "tnum")
    val tabularTertiary = tertiary.copy(fontFeatureSettings = "tnum")
}

object AmzShapes {
    val card = RoundedCornerShape(AmzDimens.cardRadius)
    val row = RoundedCornerShape(AmzDimens.rowRadius)
    val stat = RoundedCornerShape(AmzDimens.statRadius)
    val hero = RoundedCornerShape(AmzDimens.heroRadius)
    val sheet = RoundedCornerShape(
        topStart = AmzDimens.sheetRadius,
        topEnd = AmzDimens.sheetRadius,
    )
    val chip = RoundedCornerShape(AmzDimens.chipRadius)
    val round = RoundedCornerShape(AmzDimens.pillRadius)
    val field = RoundedCornerShape(18.dp)
}

/**
 * Converts a CSS angle to Compose gradient endpoints. CSS 0deg points up and 90deg points
 * right; Compose y grows downward, so the vertical component is inverted.
 */
fun cssLinearGradient(
    angleDeg: Float,
    vararg stops: Pair<Float, Color>,
): Brush = ShaderBrush { size ->
    val radians = Math.toRadians(angleDeg.toDouble())
    val dx = sin(radians).toFloat()
    val dy = -cos(radians).toFloat()
    val reach = (abs(dx) * size.width + abs(dy) * size.height) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    Brush.linearGradient(
        colorStops = stops.map { it.first to it.second }.toTypedArray(),
        start = Offset(center.x - dx * reach, center.y - dy * reach),
        end = Offset(center.x + dx * reach, center.y + dy * reach),
    ).createShader(size)
}

@Composable
fun AmzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AmzColors.Background,
            surface = AmzColors.Surface2,
            primary = AmzColors.AccentBlueEnd,
            secondary = AmzColors.TextSecondary,
            onBackground = AmzColors.TextPrimary,
            onSurface = AmzColors.TextPrimary,
        ),
        typography = MaterialTypography(),
        content = content,
    )
}
