package com.abhi.armz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadEntity::class, SegmentEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ArmzDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
