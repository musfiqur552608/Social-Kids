package com.example.socialbaby.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    foreignKeys = [
        ForeignKey(
            entity = ShelfEntity::class,
            parentColumns = ["id"],
            childColumns = ["shelfId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("shelfId"), Index("type")]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // MediaType name
    val sourceUri: String? = null, // content:// for local
    val externalUrl: String? = null, // original pasted link
    val platformVideoId: String? = null,
    val title: String,
    val thumbnailPath: String? = null, // local cached file path or remote URL
    val shelfId: Long? = null,
    val sortOrder: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastWatchedPositionMs: Long = 0,
    val lastWatchedAt: Long? = null,
    val totalWatchTimeMs: Long = 0,
    val isFavorite: Boolean = false
)

enum class MediaType {
    LOCAL_VIDEO,
    LOCAL_IMAGE,
    YOUTUBE_LINK,
    ONLINE_VIDEO,
    ONLINE_IMAGE,
    GENERIC_LINK
}

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String, // ShelfKind
    val sortOrder: Int = 0
)

enum class ShelfKind {
    SHELF,
    PLAYLIST,
    SHORTS_FEED,
    PHOTOS
}

@Entity(
    tableName = "watch_logs",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mediaItemId"), Index("watchedAt")]
)
data class WatchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaItemId: Long,
    val watchedAt: Long = System.currentTimeMillis(),
    val durationMs: Long
)
