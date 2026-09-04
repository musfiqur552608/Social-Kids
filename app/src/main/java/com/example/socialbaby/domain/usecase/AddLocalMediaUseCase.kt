package com.example.socialbaby.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.util.SafUriPersister
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class AddLocalMediaUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(uri: Uri, mimeType: String?, shelfId: Long?): Long {
        SafUriPersister.persist(context, uri)
        val name = queryDisplayName(uri) ?: "Local media"
        val isImage = mimeType?.startsWith("image") == true ||
                name.lowercase().endsWith(".jpg") ||
                name.lowercase().endsWith(".jpeg") ||
                name.lowercase().endsWith(".png") ||
                name.lowercase().endsWith(".webp") ||
                name.lowercase().endsWith(".gif")

        if (isImage) {
            val item = MediaItem.LocalImage(title = name, sourceUri = uri.toString(), shelfId = shelfId, thumbnailPath = uri.toString())
            return mediaRepository.insert(item)
        } else {
            // video → generate thumbnail preview
            val thumbPath = generateVideoThumbnail(uri)
            val item = MediaItem.LocalVideo(title = name, sourceUri = uri.toString(), shelfId = shelfId, thumbnailPath = thumbPath)
            return mediaRepository.insert(item)
        }
    }

    private fun generateVideoThumbnail(uri: Uri): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bmp = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) // 1s
            retriever.release()
            if (bmp != null) {
                val file = File(context.cacheDir, "thumb_local_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bmp.recycle()
                file.absolutePath
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (e: Exception) { null }
    }
}
