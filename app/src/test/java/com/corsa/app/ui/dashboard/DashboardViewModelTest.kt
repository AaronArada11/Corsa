package com.corsa.app.ui.dashboard

import com.corsa.app.data.CorsaRepository
import com.corsa.app.data.local.VehicleDao
import com.corsa.app.data.local.VehicleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: DashboardDao
    @Before fun setup() { Dispatchers.setMain(dispatcher); dao = DashboardDao() }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun emptyAndGarageStates_reflectStoredVehicles() = runTest {
        val model = DashboardViewModel(CorsaRepository(dao))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        advanceUntilIdle()
        assertEquals(DashboardUiState.Empty, model.uiState.value)
        dao.insert(VehicleEntity(brand = "A", model = "One", year = 2024, currentMileageKm = 1))
        advanceUntilIdle()
        assertEquals(DashboardUiState.Garage(1), model.uiState.value)
    }

    @Test fun error_canRetry() = runTest {
        dao.fails = true
        val model = DashboardViewModel(CorsaRepository(dao))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        advanceUntilIdle()
        assertEquals(DashboardUiState.Error, model.uiState.value)
        dao.fails = false; model.retry(); advanceUntilIdle()
        assertEquals(DashboardUiState.Empty, model.uiState.value)
    }
}

private class DashboardDao : VehicleDao {
    private val state = MutableStateFlow<List<VehicleEntity>>(emptyList())
    var fails = false
    override suspend fun insert(vehicle: VehicleEntity): Long { val id = state.value.size + 1L; state.value = state.value + vehicle.copy(id = id); return id }
    override suspend fun update(vehicle: VehicleEntity) = 0
    override suspend fun getById(id: Long) = state.value.firstOrNull { it.id == id }
    override fun observeById(id: Long): Flow<VehicleEntity?> = state.map { rows -> rows.firstOrNull { it.id == id } }
    override fun observeAll(): Flow<List<VehicleEntity>> = if (fails) flow { throw IllegalStateException("disk") } else state
    override suspend fun deleteById(id: Long) = 0
    override suspend fun advanceMileageIfHigher(id: Long, newMileageKm: Long) = 0
}
