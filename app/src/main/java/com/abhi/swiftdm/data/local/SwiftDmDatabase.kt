package com.abhi.swiftdm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadEntity::class, SegmentEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SwiftDmDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
