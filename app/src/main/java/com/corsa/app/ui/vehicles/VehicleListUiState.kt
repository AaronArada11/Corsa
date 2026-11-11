package com.corsa.app.ui.vehicles

data class VehicleListItemUiModel(
    val id: Long,
    val brand: String,
    val model: String,
    val year: Int,
    val plateNumber: String?,
    val currentMileageKm: Long,
    val mileageDisplay: String
)

sealed interface VehicleListUiState {
    data object Loading : VehicleListUiState
    data object Empty : VehicleListUiState
    data class Content(val vehicles: List<VehicleListItemUiModel>) : VehicleListUiState
    data class Error(val message: String) : VehicleListUiState
}
