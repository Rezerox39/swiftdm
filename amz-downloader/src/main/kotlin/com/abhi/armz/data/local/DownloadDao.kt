package com.abhi.armz.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun find(id: String): DownloadEntity?

    @Query(
        "SELECT * FROM downloads WHERE status IN ('Probing', 'Downloading', 'Finalizing') " +
            "ORDER BY createdAt ASC"
    )
    suspend fun interrupted(): List<DownloadEntity>

    @Query(
        "UPDATE downloads SET status = :status, errorMessage = NULL, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    suspend fun markActive(id: String, status: String, updatedAt: Long)

    @Query("SELECT * FROM download_segments WHERE downloadId = :id ORDER BY segmentIndex")
    suspend fun segments(id: String): List<SegmentEntity>

    @Upsert
    suspend fun upsertDownload(entity: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(entities: List<SegmentEntity>)

    @Upsert
    suspend fun upsertSegments(entities: List<SegmentEntity>)

    @Query("DELETE FROM download_segments WHERE downloadId = :id AND segmentIndex = :segmentIndex")
    suspend fun deleteSegment(id: String, segmentIndex: Int)

    @Query("DELETE FROM download_segments WHERE downloadId = :id")
    suspend fun deleteSegments(id: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)
}
