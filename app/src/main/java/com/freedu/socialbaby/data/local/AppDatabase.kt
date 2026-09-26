package com.freedu.socialbaby.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freedu.socialbaby.data.local.dao.MediaItemDao
import com.freedu.socialbaby.data.local.dao.ShelfDao
import com.freedu.socialbaby.data.local.dao.WatchLogDao
import com.freedu.socialbaby.data.local.entity.MediaItemEntity
import com.freedu.socialbaby.data.local.entity.ShelfEntity
import com.freedu.socialbaby.data.local.entity.WatchLogEntity

@Database(
    entities = [MediaItemEntity::class, ShelfEntity::class, WatchLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun shelfDao(): ShelfDao
    abstract fun watchLogDao(): WatchLogDao
}
