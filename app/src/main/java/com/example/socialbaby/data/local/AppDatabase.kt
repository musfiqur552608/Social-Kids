package com.example.socialbaby.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.socialbaby.data.local.dao.MediaItemDao
import com.example.socialbaby.data.local.dao.ShelfDao
import com.example.socialbaby.data.local.dao.WatchLogDao
import com.example.socialbaby.data.local.entity.MediaItemEntity
import com.example.socialbaby.data.local.entity.ShelfEntity
import com.example.socialbaby.data.local.entity.WatchLogEntity

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
