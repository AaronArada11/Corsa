package com.corsa.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentFiles(private val context: Context) {
    private val directory get() = File(context.filesDir, "documents")

    fun managedFile(path: String): File? = runCatching {
        File(path).canonicalFile.takeIf { it.parentFile == directory.canonicalFile }
    }.getOrNull()

    suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val extension = when (context.contentResolver.getType(uri)) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> error("Choose a JPEG, PNG, or PDF file")
        }
        directory.mkdirs()
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Unable to read attachment" }
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 20 * 1024 * 1024) { "Attachment must be 20 MB or smaller" }
                        output.write(buffer, 0, count)
                    }
                    require(total > 0) { "Attachment is empty" }
                }
            }
            target.absolutePath
        } catch (error: Exception) { target.delete(); throw error }
    }

    suspend fun thumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = managedFile(path)?.takeIf { it.isFile } ?: return@withContext null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        options.inSampleSize = 1
        while (options.outWidth / options.inSampleSize > 512 || options.outHeight / options.inSampleSize > 512) options.inSampleSize *= 2
        options.inJustDecodeBounds = false
        BitmapFactory.decodeFile(file.path, options)
    }

    fun delete(path: String) { managedFile(path)?.delete() }
}
