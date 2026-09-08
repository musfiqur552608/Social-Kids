package com.example.socialbaby.data.mapper

import com.example.socialbaby.data.local.entity.MediaItemEntity
import com.example.socialbaby.data.local.entity.MediaType
import com.example.socialbaby.data.local.entity.ShelfEntity
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.model.Shelf

fun MediaItemEntity.toDomain(): MediaItem {
    return when (type) {
        MediaType.LOCAL_VIDEO.name -> MediaItem.LocalVideo(
            id = id, title = title, thumbnailPath = thumbnailPath,
            shelfId = shelfId, sortOrder = sortOrder, dateAdded = dateAdded,
            lastWatchedPositionMs = lastWatchedPositionMs, lastWatchedAt = lastWatchedAt,
            totalWatchTimeMs = totalWatchTimeMs, isFavorite = isFavorite,
            sourceUri = sourceUri ?: ""
        )
        MediaType.LOCAL_IMAGE.name -> MediaItem.LocalImage(
            id = id, title = title, thumbnailPath = thumbnailPath,
            shelfId = shelfId, sortOrder = sortOrder, dateAdded = dateAdded,
            lastWatchedPositionMs = lastWatchedPositionMs, lastWatchedAt = lastWatchedAt,
            totalWatchTimeMs = totalWatchTimeMs, isFavorite = isFavorite,
            sourceUri = sourceUri ?: ""
        )
        MediaType.YOUTUBE_LINK.name -> MediaItem.YoutubeLink(
            id = id, title = title, thumbnailPath = thumbnailPath,
            shelfId = shelfId, sortOrder = sortOrder, dateAdded = dateAdded,
            lastWatchedPositionMs = lastWatchedPositionMs, lastWatchedAt = lastWatchedAt,
            totalWatchTimeMs = totalWatchTimeMs, isFavorite = isFavorite,
            externalUrl = externalUrl ?: "", platformVideoId = platformVideoId ?: ""
        )
        MediaType.ONLINE_VIDEO.name -> MediaItem.OnlineVideo(
            id = id, title = title, thumbnailPath = thumbnailPath,
            shelfId = shelfId, sortOrder = sortOrder, dateAdded = dateAdded,
            lastWatchedPositionMs = lastWatchedPositionMs, lastWatchedAt = lastWatchedAt,
            totalWatchTimeMs = totalWatchTimeMs, isFavorite = isFavorite,
            externalUrl = externalUrl ?: ""
        )
        MediaType.ONLINE_IMAGE.name -> MediaItem.OnlineImage(
            id = id, title = title, thumbnailPath = thumbnailPath,
            shelfId = shelfId, sortOrder = sortOrder, dateAdded = dateAdded,
            lastWatchedPositionMs = lastWatchedPositionMs, lastWatchedAt = lastWatchedAt,
            totalWatchTimeMs = totalWatchTimeMs, isFavorite = isFavorite,
            externalUrl = externalUrl ?: ""
        )
        MediaType.GENERIC_LINK.name -> MediaItem.GenericLink(
            id = id, title = title, thumbnailPath = thumbnailPath,
            shelfId = shelfId, sortOrder = sortOrder, dateAdded = dateAdded,
            lastWatchedPositionMs = lastWatchedPositionMs, lastWatchedAt = lastWatchedAt,
            totalWatchTimeMs = totalWatchTimeMs, isFavorite = isFavorite,
            externalUrl = externalUrl ?: ""
        )
        else -> MediaItem.OnlineVideo(
            id = id, title = title, thumbnailPath = thumbnailPath,
            shelfId = shelfId, sortOrder = sortOrder, dateAdded = dateAdded,
            lastWatchedPositionMs = lastWatchedPositionMs, lastWatchedAt = lastWatchedAt,
            totalWatchTimeMs = totalWatchTimeMs, isFavorite = isFavorite,
            externalUrl = externalUrl ?: sourceUri ?: ""
        )
    }
}

fun MediaItem.toEntity(): MediaItemEntity {
    val typeName = when (this) {
        is MediaItem.LocalVideo -> MediaType.LOCAL_VIDEO.name
        is MediaItem.LocalImage -> MediaType.LOCAL_IMAGE.name
        is MediaItem.YoutubeLink -> MediaType.YOUTUBE_LINK.name
        is MediaItem.OnlineVideo -> MediaType.ONLINE_VIDEO.name
        is MediaItem.OnlineImage -> MediaType.ONLINE_IMAGE.name
        is MediaItem.GenericLink -> MediaType.GENERIC_LINK.name
    }
    val sourceUri = when (this) {
        is MediaItem.LocalVideo -> sourceUri
        is MediaItem.LocalImage -> sourceUri
        else -> null
    }
    val externalUrl = when (this) {
        is MediaItem.YoutubeLink -> externalUrl
        is MediaItem.OnlineVideo -> externalUrl
        is MediaItem.OnlineImage -> externalUrl
        is MediaItem.GenericLink -> externalUrl
        else -> null
    }
    val platformVideoId = when (this) {
        is MediaItem.YoutubeLink -> platformVideoId
        else -> null
    }
    return MediaItemEntity(
        id = id,
        type = typeName,
        sourceUri = sourceUri,
        externalUrl = externalUrl,
        platformVideoId = platformVideoId,
        title = title,
        thumbnailPath = thumbnailPath,
        shelfId = shelfId,
        sortOrder = sortOrder,
        dateAdded = dateAdded,
        lastWatchedPositionMs = lastWatchedPositionMs,
        lastWatchedAt = lastWatchedAt,
        totalWatchTimeMs = totalWatchTimeMs,
        isFavorite = isFavorite
    )
}

fun ShelfEntity.toDomain(): Shelf = Shelf(
    id = id,
    name = name,
    kind = try { com.example.socialbaby.domain.model.ShelfKind.valueOf(kind) } catch (e: Exception) { com.example.socialbaby.domain.model.ShelfKind.SHELF },
    sortOrder = sortOrder
)

fun Shelf.toEntity(): ShelfEntity = ShelfEntity(
    id = id,
    name = name,
    kind = kind.name,
    sortOrder = sortOrder
)
