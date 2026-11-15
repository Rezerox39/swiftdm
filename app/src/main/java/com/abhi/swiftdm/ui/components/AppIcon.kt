package com.abhi.swiftdm.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhi.swiftdm.R

/** Material Symbols Rounded, weight 400, subsetted to the glyphs SwiftDM uses. */
val MaterialSymbolsFamily = FontFamily(
    Font(R.font.material_symbols_rounded, FontWeight.Normal),
)

/**
 * Icon set. Codepoints come from the upstream Material Symbols metadata and are
 * rendered through the bundled subset font.
 */
enum class AppIcon(val glyph: String) {
    Download("\uF090"),
    DownloadDone("\uF091"),
    ArrowDownward("\uE5DB"),
    ArrowUpward("\uE5D8"),
    ArrowBack("\uE5C4"),
    ArrowForward("\uE5C8"),
    Globe("\uE64C"),
    Settings("\uE8B8"),
    Add("\uE145"),
    Close("\uE5CD"),
    Search("\uEF7A"),
    MoreHoriz("\uE5D3"),
    MoreVert("\uE5D4"),
    ChevronRight("\uE5CC"),
    ChevronLeft("\uE5CB"),
    ExpandMore("\uE5CF"),
    Check("\uE668"),
    CheckCircle("\uF0BE"),
    Error("\uF8B6"),
    Pause("\uE034"),
    PlayArrow("\uE037"),
    Stop("\uE047"),
    Delete("\uE92E"),
    Folder("\uE2C7"),
    FolderOpen("\uE2C8"),
    InsertDriveFile("\uE66D"),
    Movie("\uE404"),
    Image("\uE3F4"),
    MusicNote("\uE405"),
    Description("\uE873"),
    Archive("\uE149"),
    Android("\uE859"),
    Bolt("\uEA0B"),
    Wifi("\uE63E"),
    WifiOff("\uE648"),
    Speed("\uE9E4"),
    Timer("\uE425"),
    Link("\uE250"),
    ContentCopy("\uE14D"),
    Share("\uE80D"),
    OpenInNew("\uE89E"),
    Refresh("\uE5D5"),
    Replay("\uE042"),
    CloudDownload("\uE2C0"),
    History("\uE8B3"),
    Home("\uE9B2"),
    Lock("\uE899"),
    Visibility("\uE8F4"),
    VisibilityOff("\uE8F5"),
    Tune("\uE429"),
    HourglassEmpty("\uE88B"),
    Schedule("\uEFD6"),
    Save("\uE161"),
    Upload("\uF09B"),
    Language("\uEA07"),
    Sync("\uE627"),
    DoneAll("\uE877"),
    PauseCircle("\uE1A2"),
    PlayCircle("\uE1C4"),
    Cancel("\uE888"),
    Circle("\uEF4A"),
    Notifications("\uE7F5"),
}

/**
 * Draws a Material Symbols glyph. The text size is adjusted for the user font
 * scale so the optical icon size stays at [size].
 */
@Composable
fun SymbolIcon(
    icon: AppIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    val density = LocalDensity.current
    val fontSize = with(density) { (size.toPx() / fontScale).toSp() }
    val description = contentDescription
    val semantics = if (description == null) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.clearAndSetSemantics { this.contentDescription = description }
    }
    Box(
        modifier = modifier.then(semantics).size(size),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon.glyph,
            color = tint,
            fontSize = fontSize,
            lineHeight = fontSize,
            fontFamily = MaterialSymbolsFamily,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}
