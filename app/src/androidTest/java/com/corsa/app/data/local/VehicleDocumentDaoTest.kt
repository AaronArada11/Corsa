package com.corsa.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleDocumentDaoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun documentsAreIsolatedAndCascadeWithTheirVehicle() = runBlocking {
        val db = CorsaDatabase.buildInMemoryDatabase(context)
        try {
            val first = db.vehicleDao().insert(VehicleEntity(brand = "A", model = "A", year = 2020, currentMileageKm = 0))
            val second = db.vehicleDao().insert(VehicleEntity(brand = "B", model = "B", year = 2020, currentMileageKm = 0))
            val dao = db.vehicleDocumentDao()
            val id = dao.insert(VehicleDocumentEntity(vehicleId = first, title = "Insurance", documentType = "INSURANCE", costMinor = 3_000_000_000))
            dao.insert(VehicleDocumentEntity(vehicleId = second, title = "Registration", documentType = "REGISTRATION"))
            assertEquals(3_000_000_000, dao.getById(id)?.costMinor)
            assertEquals(listOf(id), dao.observeForVehicle(first).first().map { it.id })
            db.vehicleDao().deleteById(first)
            assertNull(dao.getById(id))
            assertEquals(1, dao.observeForVehicle(second).first().size)
            try {
                dao.insert(VehicleDocumentEntity(vehicleId = first, title = "Orphan", documentType = "OTHER"))
                fail("Missing vehicle must reject a document")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        } finally { db.close() }
    }

    @Test fun versionOneMigrationPreservesVehicles() = runBlocking {
        val name = "document-migration-${System.nanoTime()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE vehicle (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, brand TEXT NOT NULL, model TEXT NOT NULL, year INTEGER NOT NULL, plateNumber TEXT, currentMileageKm INTEGER NOT NULL, photoPath TEXT)")
            old.execSQL("INSERT INTO vehicle VALUES(41, 'Toyota', 'Corolla', 2020, 'ABC', 12345, NULL)")
            old.version = 1
        }
        val db = CorsaDatabase.buildFileDatabase(context, name)
        try {
            assertEquals("Corolla", db.vehicleDao().getById(41)?.model)
            val id = db.vehicleDocumentDao().insert(VehicleDocumentEntity(vehicleId = 41, title = "Migrated", documentType = "OTHER"))
            assertNotNull(db.vehicleDocumentDao().getById(id))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
