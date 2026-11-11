package com.corsa.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.corsa.app.data.local.CorsaDatabase
import com.corsa.app.model.DeleteOutcome
import com.corsa.app.model.SaveOutcome
import com.corsa.app.model.VehicleDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CorsaRepositoryVehicleTest {

    private lateinit var context: Context
    private lateinit var db: CorsaDatabase
    private lateinit var repository: CorsaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = CorsaDatabase.buildInMemoryDatabase(context)
        repository = CorsaRepository(db, currentYearProvider = { 2026 })
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun saveVehicle_validDraft_persistsToRoom() = runBlocking {
        val draft = VehicleDraft(
            brand = "  Toyota ",
            model = " Corolla ",
            year = " 2022 ",
            plateNumber = "   ",
            currentMileageKm = " 15000 "
        )

        val outcome = repository.saveVehicle(draft)
        assertTrue(outcome is SaveOutcome.Success)
        val id = (outcome as SaveOutcome.Success).id

        val vehicle = repository.getVehicle(id)
        assertNotNull(vehicle)
        assertEquals("Toyota", vehicle?.brand)
        assertEquals("Corolla", vehicle?.model)
        assertNull(vehicle?.plateNumber)
    }

    @Test
    fun saveVehicle_invalidDraft_noDatabaseChange() = runBlocking {
        val invalidDraft = VehicleDraft(
            brand = "",
            model = "",
            year = "1800",
            currentMileageKm = "-1"
        )
        val outcome = repository.saveVehicle(invalidDraft)
        assertTrue(outcome is SaveOutcome.ValidationErrors)

        val all = repository.observeAllVehicles().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun updateVehicle_missingId_returnsMissing() = runBlocking {
        val draft = VehicleDraft(
            id = 999L,
            brand = "Honda",
            model = "Fit",
            year = "2020",
            currentMileageKm = "10000"
        )
        val outcome = repository.saveVehicle(draft)
        assertEquals(SaveOutcome.MissingRecord, outcome)
    }

    @Test
    fun deleteVehicle_removesFromRoom() = runBlocking {
        val saveOutcome = repository.saveVehicle(
            VehicleDraft(brand = "Subaru", model = "WRX", year = "2021", currentMileageKm = "20000")
        )
        val id = (saveOutcome as SaveOutcome.Success).id

        assertEquals(DeleteOutcome.Deleted, repository.deleteVehicle(id))
        assertNull(repository.getVehicle(id))
        assertEquals(DeleteOutcome.Missing, repository.deleteVehicle(id))
    }
}
