package com.corsa.app.ui.dashboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Bounded decoding of existing app-private photos; never dereference external paths. */
internal object DashboardPhoto {
    fun load(privateRoot: File, path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val root = privateRoot.canonicalFile
            val file = File(path).canonicalFile
            if (!file.path.startsWith(root.path + File.separator) || !file.isFile || !file.canRead()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: Exception) {
            null
        }
    }
}
