package com.corsa.app.ui.vehicles

import com.corsa.app.model.Vehicle

sealed interface VehicleDetailsUiState {
    data object Loading : VehicleDetailsUiState
    data object Missing : VehicleDetailsUiState
    data class Error(val message: String) : VehicleDetailsUiState
    data class Content(val vehicle: Vehicle, val deleting: Boolean = false, val deleteError: String? = null) : VehicleDetailsUiState
    data object Deleted : VehicleDetailsUiState
}
