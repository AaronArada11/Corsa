package com.corsa.app.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import com.corsa.app.data.CorsaRepository
import com.corsa.app.data.local.VehicleDao
import com.corsa.app.data.local.VehicleEntity
import com.corsa.app.model.VehicleDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleScreenViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeDao
    private lateinit var repository: CorsaRepository

    @Before fun setup() { Dispatchers.setMain(dispatcher); dao = FakeDao(); repository = CorsaRepository(dao) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun editor_restoresDraftAndIgnoresRepeatedSave() = runTest {
        val handle = SavedStateHandle(mapOf("vehicleId" to 0L))
        val model = VehicleEditorViewModel(repository, handle)
        model.updateDraft(VehicleDraft(brand = "Toyota", model = "Vios", year = "2024", currentMileageKm = "10", photoPath = "/kept"))
        val restored = VehicleEditorViewModel(repository, handle)
        assertEquals("Toyota", (restored.uiState.value as VehicleEditorUiState.Form).draft.brand)
        assertEquals("/kept", (restored.uiState.value as VehicleEditorUiState.Form).draft.photoPath)
        restored.save(); restored.save(); advanceUntilIdle()
        assertEquals(1, dao.insertCount)
    }

    @Test fun details_deletesOnlyRequestedVehicle() = runTest {
        val first = dao.insert(VehicleEntity(brand = "A", model = "One", year = 2020, currentMileageKm = 1))
        val second = dao.insert(VehicleEntity(brand = "B", model = "Two", year = 2021, currentMileageKm = 2))
        val model = VehicleDetailsViewModel(repository, SavedStateHandle(mapOf("vehicleId" to first)))
        advanceUntilIdle()
        model.deleteConfirmed(); advanceUntilIdle()
        assertEquals(VehicleDetailsUiState.Deleted, model.uiState.value)
        assertNull(dao.getById(first))
        assertEquals(second, dao.getById(second)?.id)
    }

    private class FakeDao : VehicleDao {
        private val rows = mutableMapOf<Long, VehicleEntity>()
        private val state = MutableStateFlow<Map<Long, VehicleEntity>>(emptyMap())
        var insertCount = 0
        override suspend fun insert(vehicle: VehicleEntity): Long { insertCount++; val id = (rows.keys.maxOrNull() ?: 0) + 1; rows[id] = vehicle.copy(id = id); state.value = rows.toMap(); return id }
        override suspend fun update(vehicle: VehicleEntity): Int = if (rows.containsKey(vehicle.id)) { rows[vehicle.id] = vehicle; state.value = rows.toMap(); 1 } else 0
        override suspend fun getById(id: Long) = rows[id]
        override fun observeById(id: Long): Flow<VehicleEntity?> = state.map { it[id] }
        override fun observeAll(): Flow<List<VehicleEntity>> = state.map { it.values.sortedBy(VehicleEntity::id) }
        override suspend fun deleteById(id: Long): Int { val removed = rows.remove(id); state.value = rows.toMap(); return if (removed == null) 0 else 1 }
        override suspend fun advanceMileageIfHigher(id: Long, newMileageKm: Long) = 0
    }
}
