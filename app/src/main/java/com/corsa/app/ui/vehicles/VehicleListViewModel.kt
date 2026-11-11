package com.corsa.app.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corsa.app.data.CorsaRepository
import com.corsa.app.model.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleListViewModel(
    private val repository: CorsaRepository
) : ViewModel() {

    private val retryGeneration = MutableStateFlow(0)

    val uiState: StateFlow<VehicleListUiState> = retryGeneration.flatMapLatest {
        flow { repository.observeAllVehicles().collect { emit(it) } }
            .map { vehicles ->
                if (vehicles.isEmpty()) VehicleListUiState.Empty
                else VehicleListUiState.Content(vehicles.map { it.toItemUiModel() })
            }
            .onStart { emit(VehicleListUiState.Loading) }
            .catch { emit(VehicleListUiState.Error("Unable to load vehicles")) }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VehicleListUiState.Loading
        )

    fun retry() {
        retryGeneration.value += 1
    }

    companion object {
        fun Vehicle.toItemUiModel(): VehicleListItemUiModel = VehicleListItemUiModel(
            id = id,
            brand = brand,
            model = model,
            year = year,
            plateNumber = plateNumber,
            currentMileageKm = currentMileageKm,
            mileageDisplay = "%,d km".format(currentMileageKm)
        )

        fun provideFactory(repository: CorsaRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VehicleListViewModel(repository) as T
                }
            }
    }
}
