package com.example.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Copies user-selected gallery videos and images (picked via zero-permission Android Photo Picker)
 * into app-private persistent storage so courses remain playable across app restarts.
 */
class MediaStorageManager(private val context: Context) {

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
}
