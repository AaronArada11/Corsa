package com.corsa.app.ui.vehicles

import com.corsa.app.model.VehicleDraft

sealed interface VehicleEditorUiState {
    data object Loading : VehicleEditorUiState
    data object Missing : VehicleEditorUiState
    data class LoadError(val message: String) : VehicleEditorUiState
    data class Form(
        val draft: VehicleDraft,
        val errors: Map<String, String> = emptyMap(),
        val saving: Boolean = false,
        val storageError: String? = null
    ) : VehicleEditorUiState
    data class Saved(val id: Long) : VehicleEditorUiState
}
