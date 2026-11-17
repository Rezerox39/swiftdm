package com.abhi.armz.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String,
    val etag: String?,
    val lastModified: String?,
    val segmentCount: Int,
    val bytesPerSecond: Long,
    val sha256: String?,
    val errorMessage: String?,
    val contentUri: String? = null,
    val mimeType: String? = null,
    /** Encoded Cookie/User-Agent/Referer captured from the in-app browser, if any. */
    val requestHeaders: String? = null,
    /** Where the link was added: manual, browser, share, etc. */
    val source: String? = null,
    val finalizeStep: String? = null,
    val finalizeProgress: Float = 0f,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "download_segments",
    primaryKeys = ["downloadId", "segmentIndex"],
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["downloadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("downloadId")],
)
data class SegmentEntity(
    val downloadId: String,
    val segmentIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long,
    val state: String,
)
