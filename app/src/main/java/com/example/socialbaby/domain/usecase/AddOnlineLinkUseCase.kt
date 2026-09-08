package com.example.socialbaby.domain.usecase

import android.content.Context
import com.example.socialbaby.data.remote.OEmbedApi
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.util.UrlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject

class AddOnlineLinkUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val oEmbedApi: OEmbedApi
) {
    suspend operator fun invoke(url: String, shelfId: Long?): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val parsed = UrlParser.parse(url) ?: return@withContext Result.failure(IllegalArgumentException("Unsupported URL"))
            var title = parsed.defaultTitle
            var thumbnailUrl: String? = null
            var thumbnailLocalPath: String? = null

            // Platform-specific oEmbed only for YouTube
            when (parsed.platform) {
                UrlParser.Platform.YOUTUBE -> {
                    try {
                        val resp = oEmbedApi.getYoutubeOEmbed(videoUrl = url)
                        title = resp.title ?: title
                        thumbnailUrl = resp.thumbnail_url
                    } catch (e: Exception) {
                        // keep defaults
                    }
                    if (thumbnailUrl != null) {
                        try {
                            val fileName = "thumb_${parsed.videoId.hashCode()}_${System.currentTimeMillis()}.jpg"
                            val file = File(context.cacheDir, fileName)
                            URL(thumbnailUrl).openStream().use { input ->
                                FileOutputStream(file).use { output -> input.copyTo(output) }
                            }
                            thumbnailLocalPath = file.absolutePath
                        } catch (e: Exception) {
                            thumbnailLocalPath = thumbnailUrl
                        }
                    } else {
                        thumbnailLocalPath = "https://img.youtube.com/vi/${parsed.videoId}/hqdefault.jpg"
                    }
                }
                UrlParser.Platform.ONLINE_VIDEO -> {
                    // For direct video links, try to use same URL thumbnail placeholder?
                    // No extra fetch; leave blank -> card shows play icon
                    title = title.ifBlank { "Online Video" }
                }
                UrlParser.Platform.ONLINE_IMAGE -> {
                    title = title.ifBlank { "Online Image" }
                    // Use the image itself as thumbnail (cached via Coil, no need to copy)
                    thumbnailLocalPath = url
                }
                UrlParser.Platform.GENERIC -> {
                    title = title.ifBlank { "Link" }
                }
            }

            val item: MediaItem = when (parsed.platform) {
                UrlParser.Platform.YOUTUBE -> MediaItem.YoutubeLink(title = title, thumbnailPath = thumbnailLocalPath, shelfId = shelfId, externalUrl = url, platformVideoId = parsed.videoId)
                UrlParser.Platform.ONLINE_VIDEO -> MediaItem.OnlineVideo(title = title, thumbnailPath = thumbnailLocalPath, shelfId = shelfId, externalUrl = url)
                UrlParser.Platform.ONLINE_IMAGE -> MediaItem.OnlineImage(title = title, thumbnailPath = thumbnailLocalPath ?: url, shelfId = shelfId, externalUrl = url)
                UrlParser.Platform.GENERIC -> MediaItem.GenericLink(title = title, thumbnailPath = thumbnailLocalPath, shelfId = shelfId, externalUrl = url)
            }
            val id = mediaRepository.insert(item)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
