package com.example.socialbaby.domain.model

sealed class MediaItem {
    abstract val id: Long
    abstract val title: String
    abstract val thumbnailPath: String?
    abstract val shelfId: Long?
    abstract val sortOrder: Int
    abstract val dateAdded: Long
    abstract val lastWatchedPositionMs: Long
    abstract val lastWatchedAt: Long?
    abstract val totalWatchTimeMs: Long
    abstract val isFavorite: Boolean

    data class LocalVideo(
        override val id: Long = 0,
        override val title: String,
        override val thumbnailPath: String? = null,
        override val shelfId: Long? = null,
        override val sortOrder: Int = 0,
        override val dateAdded: Long = System.currentTimeMillis(),
        override val lastWatchedPositionMs: Long = 0,
        override val lastWatchedAt: Long? = null,
        override val totalWatchTimeMs: Long = 0,
        override val isFavorite: Boolean = false,
        val sourceUri: String
    ) : MediaItem()

    data class LocalImage(
        override val id: Long = 0,
        override val title: String,
        override val thumbnailPath: String? = null,
        override val shelfId: Long? = null,
        override val sortOrder: Int = 0,
        override val dateAdded: Long = System.currentTimeMillis(),
        override val lastWatchedPositionMs: Long = 0,
        override val lastWatchedAt: Long? = null,
        override val totalWatchTimeMs: Long = 0,
        override val isFavorite: Boolean = false,
        val sourceUri: String
    ) : MediaItem()

    data class YoutubeLink(
        override val id: Long = 0,
        override val title: String,
        override val thumbnailPath: String? = null,
        override val shelfId: Long? = null,
        override val sortOrder: Int = 0,
        override val dateAdded: Long = System.currentTimeMillis(),
        override val lastWatchedPositionMs: Long = 0,
        override val lastWatchedAt: Long? = null,
        override val totalWatchTimeMs: Long = 0,
        override val isFavorite: Boolean = false,
        val externalUrl: String,
        val platformVideoId: String
    ) : MediaItem()

    // --- Generic platform-agnostic online media (works 100% in-app, no embed) ---
    data class OnlineVideo(
        override val id: Long = 0,
        override val title: String,
        override val thumbnailPath: String? = null,
        override val shelfId: Long? = null,
        override val sortOrder: Int = 0,
        override val dateAdded: Long = System.currentTimeMillis(),
        override val lastWatchedPositionMs: Long = 0,
        override val lastWatchedAt: Long? = null,
        override val totalWatchTimeMs: Long = 0,
        override val isFavorite: Boolean = false,
        val externalUrl: String
    ) : MediaItem()

    data class OnlineImage(
        override val id: Long = 0,
        override val title: String,
        override val thumbnailPath: String? = null,
        override val shelfId: Long? = null,
        override val sortOrder: Int = 0,
        override val dateAdded: Long = System.currentTimeMillis(),
        override val lastWatchedPositionMs: Long = 0,
        override val lastWatchedAt: Long? = null,
        override val totalWatchTimeMs: Long = 0,
        override val isFavorite: Boolean = false,
        val externalUrl: String
    ) : MediaItem()

    data class GenericLink(
        override val id: Long = 0,
        override val title: String,
        override val thumbnailPath: String? = null,
        override val shelfId: Long? = null,
        override val sortOrder: Int = 0,
        override val dateAdded: Long = System.currentTimeMillis(),
        override val lastWatchedPositionMs: Long = 0,
        override val lastWatchedAt: Long? = null,
        override val totalWatchTimeMs: Long = 0,
        override val isFavorite: Boolean = false,
        val externalUrl: String
    ) : MediaItem()
}

enum class MediaItemType {
    LOCAL_VIDEO, LOCAL_IMAGE, YOUTUBE_LINK, ONLINE_VIDEO, ONLINE_IMAGE, GENERIC_LINK
}

fun MediaItem.typeName(): String = when (this) {
    is MediaItem.LocalVideo -> "LOCAL_VIDEO"
    is MediaItem.LocalImage -> "LOCAL_IMAGE"
    is MediaItem.YoutubeLink -> "YOUTUBE_LINK"
    is MediaItem.OnlineVideo -> "ONLINE_VIDEO"
    is MediaItem.OnlineImage -> "ONLINE_IMAGE"
    is MediaItem.GenericLink -> "GENERIC_LINK"
}
