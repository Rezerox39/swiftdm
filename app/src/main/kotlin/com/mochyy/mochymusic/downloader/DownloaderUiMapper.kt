package com.mochyy.mochymusic.downloader

import com.abhi.armz.data.local.DownloadEntity
import kotlin.math.max

enum class DownloaderFilter(val label: String) {
    All("All"),
    Active("Active"),
    Completed("Done"),
    Failed("Failed"),
}

data class DownloaderRow(
    val id: String,
    val title: String,
    val url: String,
    val status: String,
    val fraction: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
    val etaSeconds: Long?,
    val segments: Int,
    val finalizeStep: String?,
    val error: String?,
    val contentUri: String?,
    val filePath: String?,
    val mimeType: String?,
    val source: String?,
) {
    val isActive: Boolean get() = status in ACTIVE_STATUSES
    val isPaused: Boolean get() = status == "Paused"
    val isCompleted: Boolean get() = status == "Completed"

    companion object {
        private val ACTIVE_STATUSES = setOf("Probing", "Downloading", "Finalizing")
    }
}

data class DownloaderStats(
    val active: Int,
    val completed: Int,
    val failed: Int,
    val bytesPerSecond: Long,
    val downloadedBytes: Long,
)

object DownloaderUiMapper {
    fun toRow(entity: DownloadEntity): DownloaderRow {
        val known = entity.totalBytes > 0L
        val fraction = if (known) {
            (entity.downloadedBytes.toDouble() / entity.totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }
        return DownloaderRow(
            id = entity.id,
            title = entity.fileName.ifBlank { entity.url.substringAfterLast('/').ifBlank { entity.url } },
            url = entity.url,
            status = entity.status,
            fraction = fraction,
            downloadedBytes = max(0L, entity.downloadedBytes),
            totalBytes = entity.totalBytes,
            bytesPerSecond = max(0L, entity.bytesPerSecond),
            etaSeconds = if (known && entity.bytesPerSecond > 0L) {
                max(0L, (entity.totalBytes - entity.downloadedBytes) / entity.bytesPerSecond)
            } else {
                null
            },
            segments = entity.segmentCount,
            finalizeStep = entity.finalizeStep,
            error = entity.errorMessage,
            contentUri = entity.contentUri,
            filePath = entity.filePath,
            mimeType = entity.mimeType,
            source = entity.source,
        )
    }

    fun stats(rows: List<DownloaderRow>): DownloaderStats = DownloaderStats(
        active = rows.count { it.isActive },
        completed = rows.count { it.isCompleted },
        failed = rows.count { it.status == "Failed" },
        bytesPerSecond = rows.filter { it.isActive }.sumOf { it.bytesPerSecond },
        downloadedBytes = rows.filter { it.isCompleted }.sumOf { it.totalBytes.coerceAtLeast(0L) },
    )

    fun filter(rows: List<DownloaderRow>, filter: DownloaderFilter): List<DownloaderRow> = when (filter) {
        DownloaderFilter.All -> rows
        DownloaderFilter.Active -> rows.filter { it.isActive || it.isPaused }
        DownloaderFilter.Completed -> rows.filter { it.isCompleted }
        DownloaderFilter.Failed -> rows.filter { it.status == "Failed" || it.status == "Cancelled" }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> String.format("%.2f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000L -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000L -> String.format("%.0f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }

    fun formatRate(bytesPerSecond: Long): String = when {
        bytesPerSecond <= 0L -> "—"
        bytesPerSecond >= 1_000_000L -> String.format("%.1f MB/s", bytesPerSecond / 1_000_000.0)
        bytesPerSecond >= 1_000L -> String.format("%.0f KB/s", bytesPerSecond / 1_000.0)
        else -> "$bytesPerSecond B/s"
    }

    fun formatEta(seconds: Long?): String {
        if (seconds == null) return "—"
        return when {
            seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun statusLabel(row: DownloaderRow): String = when (row.status) {
        "Probing" -> "Checking link"
        "Downloading" -> if (row.segments > 1) "Downloading · ${row.segments} parts" else "Downloading"
        "Finalizing" -> row.finalizeStep ?: "Finalizing"
        "Paused" -> "Paused"
        "Completed" -> "Ready"
        "Failed" -> row.error ?: "Failed"
        "Cancelled" -> "Cancelled"
        else -> row.status.replaceFirstChar { it.uppercase() }
    }
}
