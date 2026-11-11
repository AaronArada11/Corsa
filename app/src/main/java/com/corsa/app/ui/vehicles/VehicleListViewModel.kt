package com.corsa.app.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corsa.app.data.CorsaRepository
import com.corsa.app.model.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class VehicleListViewModel(
    private val repository: CorsaRepository
) : ViewModel() {

    val uiState: StateFlow<VehicleListUiState> = repository.observeAllVehicles()
        .map { vehicles ->
            if (vehicles.isEmpty()) {
                VehicleListUiState.Empty
            } else {
                VehicleListUiState.Content(vehicles.map { it.toItemUiModel() })
            }
        }
        .catch { throwable ->
            emit(VehicleListUiState.Error(throwable.localizedMessage ?: "Failed to load vehicles"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VehicleListUiState.Loading
        )

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
