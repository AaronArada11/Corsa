package com.corsa.app.ui.vehicles

import com.corsa.app.data.CorsaRepository
import com.corsa.app.data.local.VehicleDao
import com.corsa.app.data.local.VehicleEntity
import com.corsa.app.model.VehicleDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeVehicleDao : VehicleDao {
        private val storage = mutableMapOf<Long, VehicleEntity>()
        val state = MutableStateFlow<Map<Long, VehicleEntity>>(emptyMap())
        var shouldThrow = false

        override suspend fun insert(vehicle: VehicleEntity): Long {
            val id = (storage.keys.maxOrNull() ?: 0L) + 1L
            storage[id] = vehicle.copy(id = id)
            state.update { storage.toMap() }
            return id
        }

        override suspend fun update(vehicle: VehicleEntity): Int = 0
        override suspend fun getById(id: Long): VehicleEntity? = storage[id]
        override fun observeById(id: Long): Flow<VehicleEntity?> = state.map { it[id] }

        override fun observeAll(): Flow<List<VehicleEntity>> {
            if (shouldThrow) {
                return flow { throw RuntimeException("Disk I/O error") }
            }
            return state.map { it.values.toList().sortedBy { it.id } }
        }

        override suspend fun deleteById(id: Long): Int = 0
        override suspend fun advanceMileageIfHigher(id: Long, newMileageKm: Long): Int = 0
    }

    private lateinit var fakeDao: FakeVehicleDao
    private lateinit var repository: CorsaRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeVehicleDao()
        repository = CorsaRepository(vehicleDao = fakeDao)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading_thenBecomesEmpty() = runTest {
        val viewModel = VehicleListViewModel(repository)
        assertEquals(VehicleListUiState.Loading, viewModel.uiState.value)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()
        assertEquals(VehicleListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun withVehicles_emitsContentInOrderWithFormattedMileage() = runTest {
        repository.saveVehicle(
            VehicleDraft(brand = "Toyota", model = "Camry", year = "2021", plateNumber = "XYZ-123", currentMileageKm = "45250")
        )
        repository.saveVehicle(
            VehicleDraft(brand = "Honda", model = "CR-V", year = "2023", currentMileageKm = "8100")
        )

        val viewModel = VehicleListViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is VehicleListUiState.Content)
        val content = state as VehicleListUiState.Content
        assertEquals(2, content.vehicles.size)

        assertEquals("Toyota", content.vehicles[0].brand)
        assertEquals("Camry", content.vehicles[0].model)
        assertEquals("XYZ-123", content.vehicles[0].plateNumber)
        assertEquals("45,250 km", content.vehicles[0].mileageDisplay)

        assertEquals("Honda", content.vehicles[1].brand)
        assertEquals("8,100 km", content.vehicles[1].mileageDisplay)
    }

    @Test
    fun whenStorageFails_emitsRetryableErrorState() = runTest {
        fakeDao.shouldThrow = true
        val viewModel = VehicleListViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is VehicleListUiState.Error)
        assertEquals("Unable to load vehicles", (state as VehicleListUiState.Error).message)
    }

    @Test
    fun retry_afterStorageFailure_resubscribesAndReceivesContent() = runTest {
        fakeDao.shouldThrow = true
        val viewModel = VehicleListViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is VehicleListUiState.Error)

        fakeDao.shouldThrow = false
        repository.saveVehicle(
            VehicleDraft(brand = "Mazda", model = "3", year = "2024", currentMileageKm = "12")
        )
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value as VehicleListUiState.Content
        assertEquals("Mazda", state.vehicles.single().brand)
    }
}
