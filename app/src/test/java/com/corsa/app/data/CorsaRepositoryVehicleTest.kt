package com.corsa.app.data

import com.corsa.app.data.local.CorsaDatabase
import com.corsa.app.data.local.VehicleDao
import com.corsa.app.data.local.VehicleEntity
import com.corsa.app.model.DeleteOutcome
import com.corsa.app.model.SaveOutcome
import com.corsa.app.model.VehicleDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CorsaRepositoryVehicleTest {

    private class FakeVehicleDao : VehicleDao {
        private val storage = mutableMapOf<Long, VehicleEntity>()
        private val state = MutableStateFlow<Map<Long, VehicleEntity>>(emptyMap())
        private var nextId = 1L

        override suspend fun insert(vehicle: VehicleEntity): Long {
            val assignedId = if (vehicle.id == 0L) nextId++ else vehicle.id
            val stored = vehicle.copy(id = assignedId)
            storage[assignedId] = stored
            state.update { storage.toMap() }
            return assignedId
        }

        override suspend fun update(vehicle: VehicleEntity): Int {
            return if (storage.containsKey(vehicle.id)) {
                storage[vehicle.id] = vehicle
                state.update { storage.toMap() }
                1
            } else {
                0
            }
        }

        override suspend fun getById(id: Long): VehicleEntity? {
            return storage[id]
        }

        override fun observeById(id: Long): Flow<VehicleEntity?> {
            return state.map { it[id] }
        }

        override fun observeAll(): Flow<List<VehicleEntity>> {
            return state.map { it.values.toList().sortedBy { it.id } }
        }

        override suspend fun deleteById(id: Long): Int {
            val removed = storage.remove(id)
            state.update { storage.toMap() }
            return if (removed != null) 1 else 0
        }

        override suspend fun advanceMileageIfHigher(id: Long, newMileageKm: Long): Int {
            val current = storage[id] ?: return 0
            return if (newMileageKm > current.currentMileageKm) {
                storage[id] = current.copy(currentMileageKm = newMileageKm)
                state.update { storage.toMap() }
                1
            } else {
                0
            }
        }
    }

    private lateinit var fakeDao: FakeVehicleDao
    private lateinit var repository: CorsaRepository

    @Before
    fun setup() {
        fakeDao = FakeVehicleDao()
        repository = CorsaRepository(
            vehicleDao = fakeDao,
            currentYearProvider = { 2026 }
        )
    }

    @Test
    fun saveVehicle_validDraft_savesAndNormalizesPlate() = runBlocking {
        val draft = VehicleDraft(
            brand = "  Toyota  ",
            model = "  Yaris  ",
            year = " 2024 ",
            plateNumber = "   ",
            currentMileageKm = " 12000 "
        )

        val outcome = repository.saveVehicle(draft)
        assertTrue(outcome is SaveOutcome.Success)
        val savedId = (outcome as SaveOutcome.Success).id

        val vehicle = repository.getVehicle(savedId)
        assertNotNull(vehicle)
        assertEquals("Toyota", vehicle?.brand)
        assertEquals("Yaris", vehicle?.model)
        assertEquals(2024, vehicle?.year)
        assertNull(vehicle?.plateNumber)
        assertEquals(12000L, vehicle?.currentMileageKm)
    }

    @Test
    fun saveVehicle_invalidDraft_returnsValidationErrorsWithoutPersisting() = runBlocking {
        val blankDraft = VehicleDraft(
            brand = "",
            model = "",
            year = "1800",
            currentMileageKm = "-50"
        )

        val outcome = repository.saveVehicle(blankDraft)
        assertTrue(outcome is SaveOutcome.ValidationErrors)
        val errors = (outcome as SaveOutcome.ValidationErrors).errors
        assertEquals("Brand is required", errors["brand"])
        assertEquals("Model is required", errors["model"])
        assertEquals("Year must be between 1886 and 2027", errors["year"])
        assertEquals("Mileage cannot be negative", errors["currentMileageKm"])

        val all = repository.observeAllVehicles().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun saveVehicle_yearInNextCalendarYear_isValid() = runBlocking {
        val draft = VehicleDraft(
            brand = "Honda",
            model = "Civic",
            year = "2027",
            currentMileageKm = "0"
        )

        val outcome = repository.saveVehicle(draft)
        assertTrue(outcome is SaveOutcome.Success)
    }

    @Test
    fun saveVehicle_yearBeyondNextCalendarYear_isRejected() = runBlocking {
        val draft = VehicleDraft(
            brand = "Honda",
            model = "Civic",
            year = "2028",
            currentMileageKm = "0"
        )

        val outcome = repository.saveVehicle(draft)
        assertTrue(outcome is SaveOutcome.ValidationErrors)
    }

    @Test
    fun saveVehicle_numericOverflow_isRejectedGracefully() = runBlocking {
        val draft = VehicleDraft(
            brand = "Mazda",
            model = "3",
            year = "99999999999999999999",
            currentMileageKm = "9999999999999999999999999999"
        )

        val outcome = repository.saveVehicle(draft)
        assertTrue(outcome is SaveOutcome.ValidationErrors)
        val errors = (outcome as SaveOutcome.ValidationErrors).errors
        assertEquals("Valid year is required", errors["year"])
        assertEquals("Valid mileage is required", errors["currentMileageKm"])
    }

    @Test
    fun updateVehicle_missingId_returnsMissingRecord() = runBlocking {
        val draft = VehicleDraft(
            id = 999L,
            brand = "BMW",
            model = "M3",
            year = "2021",
            currentMileageKm = "45000"
        )

        val outcome = repository.saveVehicle(draft)
        assertEquals(SaveOutcome.MissingRecord, outcome)
    }

    @Test
    fun deleteVehicle_existingAndMissing() = runBlocking {
        val saveOutcome = repository.saveVehicle(
            VehicleDraft(brand = "Audi", model = "A4", year = "2020", currentMileageKm = "30000")
        )
        val id = (saveOutcome as SaveOutcome.Success).id

        val deleteOutcome1 = repository.deleteVehicle(id)
        assertEquals(DeleteOutcome.Deleted, deleteOutcome1)
        assertNull(repository.getVehicle(id))

        val deleteOutcome2 = repository.deleteVehicle(id)
        assertEquals(DeleteOutcome.Missing, deleteOutcome2)
    }
}
