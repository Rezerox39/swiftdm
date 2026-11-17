package com.mochyy.mochymusic.ui.screens.amz

import com.abhi.armz.data.local.DownloadEntity

data class AmzDownloadRow(
    val id: String,
    val fileName: String,
    val status: String,
    val fraction: Float,
    val bytesPerSecond: Long,
    val errorMessage: String?,
)

object AmzDownloadsUiMapper {
    fun toRow(entity: DownloadEntity): AmzDownloadRow {
        val fraction = if (entity.totalBytes <= 0L) {
            0f
        } else {
            (entity.downloadedBytes.toDouble() / entity.totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
        }
        return AmzDownloadRow(
            id = entity.id,
            fileName = entity.fileName.ifBlank { entity.url },
            status = entity.status,
            fraction = fraction,
            bytesPerSecond = entity.bytesPerSecond,
            errorMessage = entity.errorMessage,
        )
    }
}
