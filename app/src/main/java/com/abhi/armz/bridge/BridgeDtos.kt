package com.abhi.armz.bridge

import com.abhi.armz.data.local.DownloadEntity
import com.abhi.armz.engine.ProbeResult
import com.google.gson.Gson

/**
 * JSON DTOs shared with web/src/bridge/types.ts. Keep field names camelCase:
 * the WebView bridge is a string-only boundary and the TypeScript contract is
 * intentionally generated from the same shape.
 */
data class BridgeDownload(
    val id: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val contentUri: String?,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String,
    val bytesPerSecond: Long,
    val etaSeconds: Long?,
    val error: String?,
    val finalizeStep: String?,
    val finalizeProgress: Float,
    val segmentCount: Int,
    val createdAt: Long,
    val live: Boolean,
    val fraction: Float,
)

data class BridgeDownloadsEvent(
    val kind: String = "downloads",
    val downloads: List<BridgeDownload>,
)

data class BridgeSettingsEvent(
    val kind: String = "settings",
    val settings: BridgeSettings,
)

data class BridgeSettings(
    val wifiOnly: Boolean = false,
    val maxConcurrentDownloads: Int = 3,
    val defaultSegments: Int = 8,
    val speedLimitBytesPerSecond: Long? = null,
    val keepDownloadsAlive: Boolean = true,
    val visualEffects: String = "auto",
)

data class BridgeInsets(
    val top: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0,
    val right: Int = 0,
)

data class BridgeStartInput(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val segments: Int? = null,
)

data class BridgeIdInput(
    val id: String,
    val deleteFile: Boolean = false,
)

data class BridgeTextInput(val text: String)

data class BridgeHapticInput(val kind: String)

object BridgeMapper {
    private val gson = Gson()

    fun downloadsJson(entities: List<DownloadEntity>): String =
        gson.toJson(BridgeDownloadsEvent(downloads = entities.map(::download)))

    fun settingsJson(settings: BridgeSettings): String = gson.toJson(settings)

    fun settingsEventJson(settings: BridgeSettings): String =
        gson.toJson(BridgeSettingsEvent(settings = settings))

    fun probeJson(probe: ProbeResult?): String = gson.toJson(probe)

    fun insetsJson(insets: BridgeInsets): String = gson.toJson(insets)

    fun quote(raw: String): String = gson.toJson(raw)

    private fun download(entity: DownloadEntity): BridgeDownload {
        val active = entity.status == "Probing" || entity.status == "Downloading" || entity.status == "Finalizing"
        val fraction = if (entity.totalBytes > 0L) {
            (entity.downloadedBytes.toDouble() / entity.totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        return BridgeDownload(
            id = entity.id,
            url = entity.url,
            fileName = entity.fileName,
            filePath = entity.filePath,
            contentUri = entity.contentUri,
            mimeType = entity.mimeType,
            totalBytes = entity.totalBytes,
            downloadedBytes = entity.downloadedBytes,
            status = entity.status,
            bytesPerSecond = entity.bytesPerSecond,
            etaSeconds = null,
            error = entity.errorMessage,
            finalizeStep = entity.finalizeStep,
            finalizeProgress = entity.finalizeProgress,
            segmentCount = entity.segmentCount,
            createdAt = entity.createdAt,
            live = active,
            fraction = fraction,
        )
    }
}
