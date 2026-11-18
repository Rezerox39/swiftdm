package com.mochyy.mochymusic.amzui.data

import androidx.compose.ui.graphics.Brush
import com.mochyy.mochymusic.amzui.theme.AmzColors
import com.mochyy.mochymusic.amzui.theme.cssLinearGradient

enum class AmzFileKind(val extension: String) {
    VIDEO("MP4"),
    ARCHIVE("ZIP"),
    DOC("PDF"),
    APP("APK"),
    DISK("ISO"),
    OTHER("TXT"),
}

fun AmzFileKind.gradient(): Brush = when (this) {
    AmzFileKind.VIDEO -> cssLinearGradient(
        145f,
        0f to AmzColors.FileVideoStart,
        1f to AmzColors.FileVideoEnd,
    )
    AmzFileKind.ARCHIVE -> cssLinearGradient(
        145f,
        0f to AmzColors.FileArchiveStart,
        1f to AmzColors.FileArchiveEnd,
    )
    AmzFileKind.DOC -> cssLinearGradient(
        145f,
        0f to AmzColors.FileDocStart,
        1f to AmzColors.FileDocEnd,
    )
    AmzFileKind.APP -> cssLinearGradient(
        145f,
        0f to AmzColors.FileAppStart,
        1f to AmzColors.FileAppEnd,
    )
    AmzFileKind.DISK -> cssLinearGradient(
        145f,
        0f to AmzColors.FileDiskStart,
        1f to AmzColors.FileDiskEnd,
    )
    AmzFileKind.OTHER -> cssLinearGradient(
        145f,
        0f to AmzColors.FileOtherStart,
        1f to AmzColors.FileOtherEnd,
    )
}

data class AmzDownloadItem(
    val id: String,
    val name: String,
    val kind: AmzFileKind,
    val sizeLabel: String,
    val pct: Float,
    val active: Boolean,
    val done: Boolean = false,
    val source: String = "",
    val speedMbps: Float = 0f,
    val etaMinutes: Int = 0,
    val connections: List<Int> = listOf(100, 100, 80, 62, 48, 30, 12, 0),
    val checksum: String? = null,
)

data class AmzHeroSnapshot(
    val pct: Float = 36f,
    val speed: Float = 18.4f,
    val paused: Boolean = false,
    val clip: Boolean = true,
    val nextName: String = "Lecture_08.mp4",
    val added: Int = 0,
    val speeds: List<Float> = listOf(
        14f, 16f, 15f, 18f, 20f, 17f, 19f, 21f, 18f, 16f, 19f, 22f, 20f, 18f, 17f, 19f, 21f, 18f,
    ),
    val name: String = "ubuntu-24.04-desktop.iso",
)

data class AmzDetectedFile(
    val id: String,
    val name: String,
    val kind: AmzFileKind,
    val sizeLabel: String,
    val url: String,
)

data class AmzLibraryStats(
    val files: Int = 128,
    val totalSize: String = "14.2 GB",
    val folders: Int = 6,
    val inProgress: Int = 1,
)
