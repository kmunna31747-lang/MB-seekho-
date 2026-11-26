package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SelectedVideoInfo(
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long?,
    val mimeType: String?
)

data class StoredVideoResult(
    val videoUrl: String,
    val previewImageUrl: String?,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long = 0L
)

/**
 * Validates, inspects, previews, and stores user-selected gallery videos and profile photos
 * picked via the zero-permission Android Photo Picker into app-private persistent storage.
 */
class MediaStorageManager(private val context: Context) {

    companion object {
        private const val MAX_VIDEO_BYTES = 500L * 1024L * 1024L // 500 MB safety cap
    }

    suspend fun inspectVideoUri(sourceUri: Uri): Result<SelectedVideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = context.contentResolver.getType(sourceUri)
            var displayName: String? = null
            var sizeBytes = -1L

            context.contentResolver.query(
                sourceUri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) sizeBytes = cursor.getLong(sizeIdx)
                }
            }

            if (sizeBytes < 0L) {
                sizeBytes = context.contentResolver.openAssetFileDescriptor(sourceUri, "r")?.use {
                    it.length
                } ?: -1L
            }

            val durationMs = extractVideoDurationMs(sourceUri)
            val cleanName = displayName?.takeIf { it.isNotBlank() }
                ?: sourceUri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "selected_video.mp4"

            SelectedVideoInfo(
                displayName = cleanName,
                sizeBytes = sizeBytes,
                durationMs = durationMs,
                mimeType = mimeType
            )
        }
    }

    suspend fun extractVideoPreviewBitmap(sourceUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, sourceUri)
            extractBestFrame(retriever)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun validateAndCopyVideo(
        sourceUri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<StoredVideoResult> = withContext(Dispatchers.IO) {
        runCatching {
            val info = inspectVideoUri(sourceUri).getOrThrow()

            if (info.mimeType != null && !info.mimeType.startsWith("video/")) {
                throw IllegalArgumentException("Please select a valid video file.")
            }
            if (info.sizeBytes == 0L) {
                throw IllegalArgumentException("The selected video file is empty.")
            }
            if (info.sizeBytes > MAX_VIDEO_BYTES) {
                throw IllegalArgumentException("Video exceeds the 500 MB size limit. Please select a smaller video.")
            }

            val extFromMime = info.mimeType?.let {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
            }
            val extFromName = info.displayName.substringAfterLast('.', "").lowercase()
                .takeIf { it.length in 2..5 && it.all { ch -> ch.isLetterOrDigit() } }
            val extension = extFromMime ?: extFromName ?: "mp4"

            val videoDir = File(context.filesDir, "mb_seekho_media/videos").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(videoDir, "video_${System.currentTimeMillis()}.$extension")

            val copiedBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (copied > MAX_VIDEO_BYTES) {
                            output.flush()
                            destFile.delete()
                            throw IllegalArgumentException("Video exceeds the 500 MB size limit.")
                        }
                        if (info.sizeBytes > 0L) {
                            onProgress((copied.toFloat() / info.sizeBytes.toFloat()).coerceIn(0.05f, 0.92f))
                        } else {
                            onProgress(0.55f)
                        }
                    }
                    output.flush()
                    copied
                }
            } ?: throw IllegalStateException("Unable to read the selected video file from your gallery.")

            if (copiedBytes <= 0L) {
                destFile.delete()
                throw IllegalArgumentException("The selected video file could not be read or is empty.")
            }

            onProgress(0.95f)
            val savedVideoUri = Uri.fromFile(destFile)
            val previewImageUrl = savePreviewFrameForVideo(destFile)
            onProgress(1f)

            StoredVideoResult(
                videoUrl = savedVideoUri.toString(),
                previewImageUrl = previewImageUrl,
                displayName = info.displayName,
                sizeBytes = copiedBytes,
                durationMs = (info.durationMs ?: 0L).coerceAtLeast(0L)
            )
        }
    }

    suspend fun copyMediaToAppStorage(
        sourceUri: Uri,
        subFolder: String,
        extension: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = File(context.filesDir, "mb_seekho_media/$subFolder").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "${subFolder}_${System.currentTimeMillis()}.$extension"
            val destFile = File(targetDir, fileName)

            val totalBytes = context.contentResolver.openAssetFileDescriptor(sourceUri, "r")?.use {
                it.length
            } ?: -1L

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (totalBytes > 0) {
                            onProgress((copied.toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.98f))
                        }
                    }
                    output.flush()
                }
            } ?: error("Unable to open selected media file")

            onProgress(1f)
            Uri.fromFile(destFile).toString()
        }
    }

    private fun savePreviewFrameForVideo(videoFile: File): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val frame = extractBestFrame(retriever) ?: return null
            val previewDir = File(context.filesDir, "mb_seekho_media/previews").apply {
                if (!exists()) mkdirs()
            }
            val previewFile = File(previewDir, "preview_${System.currentTimeMillis()}.jpg")
            FileOutputStream(previewFile).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 84, out)
                out.flush()
            }
            frame.recycle()
            Uri.fromFile(previewFile).toString()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractVideoDurationMs(sourceUri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, sourceUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractBestFrame(retriever: MediaMetadataRetriever): Bitmap? {
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        val targetUs = if (durationMs > 2000L) 1_000_000L else 0L
        val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val isPortrait = if (rotation == 90 || rotation == 270) rawWidth > rawHeight else rawHeight > rawWidth
        val reqW = if (isPortrait) 540 else 720
        val reqH = if (isPortrait) 960 else 405

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                targetUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                reqW,
                reqH
            ) ?: retriever.getScaledFrameAtTime(
                0L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                reqW,
                reqH
            )
        } else {
            retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    }
}
