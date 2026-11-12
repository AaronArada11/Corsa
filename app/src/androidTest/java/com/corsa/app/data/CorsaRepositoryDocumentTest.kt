package com.corsa.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.corsa.app.data.local.CorsaDatabase
import com.corsa.app.data.local.VehicleEntity
import com.corsa.app.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CorsaRepositoryDocumentTest {
    @Test fun savesExactMoneyNormalizesAndRejectsReassignment() = runBlocking {
        val db = CorsaDatabase.buildInMemoryDatabase(ApplicationProvider.getApplicationContext<Context>())
        try {
            val vehicle = db.vehicleDao().insert(VehicleEntity(brand = "A", model = "B", year = 2020, currentMileageKm = 0))
            val repository = CorsaRepository(db)
            val draft = DocumentDraft(vehicleId = vehicle, title = " Insurance ", issueDate = "2026-11-12",
                cost = "30000000.01", physicalLocation = " Home safe ", description = " Resale proof ")
            val id = (repository.saveDocument(draft) as SaveOutcome.Success).id
            val stored = repository.getDocument(id)!!
            assertEquals(3_000_000_001, stored.costMinor)
            assertEquals("Home safe", stored.physicalLocation)
            assertEquals("Resale proof", stored.description)
            assertEquals(listOf(id), repository.observeVehicleDocuments(vehicle).first().map { it.id })
            assertEquals(SaveOutcome.MissingRecord, repository.saveDocument(draft.copy(id = id, vehicleId = vehicle + 1)))
            for (cost in listOf("-1", "0.001", "9999999999999999999999999", "abc")) {
                assertTrue(repository.saveDocument(draft.copy(cost = cost)) is SaveOutcome.ValidationErrors)
            }
            assertTrue(repository.saveDocument(draft.copy(issueDate = "2026-02-30")) is SaveOutcome.ValidationErrors)
            assertTrue(repository.saveDocument(draft.copy(issueDate = "")) is SaveOutcome.ValidationErrors)
            assertEquals(DeleteOutcome.Deleted, repository.deleteDocument(id))
            assertEquals(DeleteOutcome.Missing, repository.deleteDocument(id))
        } finally { db.close() }
    }
}
