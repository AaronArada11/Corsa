package com.corsa.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.corsa.app.data.local.*
import com.corsa.app.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.graphics.Bitmap
import androidx.core.content.FileProvider

@RunWith(AndroidJUnit4::class)
class DocumentFilesTest {
    @Test fun copiesPrivateImageAndBuildsBoundedPreview() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val files = DocumentFiles(context)
        val source = File(context.filesDir, "documents/source-${System.nanoTime()}.png")
        source.parentFile!!.mkdirs()
        val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        var copied: String? = null
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.documents", source)
            copied = files.import(uri)
            assertNotEquals(source.path, copied)
            val preview = files.thumbnail(copied)!!
            assertTrue(preview.width <= 512)
            assertTrue(preview.height <= 512)
            preview.recycle()
            assertTrue(source.exists())
        } finally { source.delete(); copied?.let(files::delete) }
    }

    @Test fun rejectsExternalPathsAndCleansCommittedAttachments() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val files = DocumentFiles(context)
        assertNull(files.managedFile(File(context.filesDir, "outside.pdf").path))
        assertNull(files.managedFile(File(context.filesDir, "documents/../outside.pdf").path))
        val db = CorsaDatabase.buildInMemoryDatabase(context)
        val attachment = File(context.filesDir, "documents/test-${System.nanoTime()}.pdf")
        attachment.parentFile!!.mkdirs()
        attachment.writeText("test")
        try {
            val vehicle = db.vehicleDao().insert(VehicleEntity(brand = "A", model = "B", year = 2020, currentMileageKm = 0))
            val repository = CorsaRepository(db, documentFiles = files)
            assertTrue(repository.saveDocument(DocumentDraft(vehicleId = vehicle, title = "Invalid", filePath = "/outside.pdf")) is SaveOutcome.ValidationErrors)
            val id = (repository.saveDocument(DocumentDraft(vehicleId = vehicle, title = "Private", filePath = attachment.path)) as SaveOutcome.Success).id
            assertTrue(attachment.exists())
            repository.deleteDocument(id)
            assertFalse(attachment.exists())
        } finally { db.close(); attachment.delete() }
    }
}
