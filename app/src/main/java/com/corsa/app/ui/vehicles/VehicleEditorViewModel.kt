package com.corsa.app.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.corsa.app.data.CorsaRepository
import com.corsa.app.data.CorsaRepository.Companion.toDraft
import com.corsa.app.model.SaveOutcome
import com.corsa.app.model.VehicleDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleEditorViewModel(
    private val repository: CorsaRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow<VehicleEditorUiState>(VehicleEditorUiState.Loading)
    val uiState: StateFlow<VehicleEditorUiState> = mutableState.asStateFlow()
    private val vehicleId = savedState.get<Long>("vehicleId") ?: 0L

    init {
        val savedId = savedState.get<Long>("savedId")
        when {
            savedId != null -> mutableState.value = VehicleEditorUiState.Saved(savedId)
            savedState.get<Boolean>("draftInitialized") == true -> mutableState.value = VehicleEditorUiState.Form(readDraft())
            vehicleId == 0L -> setDraft(VehicleDraft())
            vehicleId < 0L -> mutableState.value = VehicleEditorUiState.Missing
            else -> load()
        }
    }

    fun retryLoad() = load()

    private fun load() {
        if (vehicleId <= 0L) return
        mutableState.value = VehicleEditorUiState.Loading
        viewModelScope.launch {
            try {
                val vehicle = repository.getVehicle(vehicleId)
                if (vehicle == null) mutableState.value = VehicleEditorUiState.Missing else setDraft(vehicle.toDraft())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = VehicleEditorUiState.LoadError("Unable to load this vehicle")
            }
        }
    }

    fun updateDraft(draft: VehicleDraft) {
        val state = mutableState.value as? VehicleEditorUiState.Form ?: return
        if (state.saving) return
        setDraft(draft, emptyMap(), null)
    }

    fun save() {
        val state = mutableState.value as? VehicleEditorUiState.Form ?: return
        if (state.saving) return
        mutableState.value = state.copy(saving = true, storageError = null)
        viewModelScope.launch {
            try {
                when (val outcome = repository.saveVehicle(state.draft)) {
                    is SaveOutcome.Success -> {
                        savedState["savedId"] = outcome.id
                        mutableState.value = VehicleEditorUiState.Saved(outcome.id)
                    }
                    is SaveOutcome.ValidationErrors -> mutableState.value = state.copy(errors = outcome.errors)
                    SaveOutcome.MissingRecord -> mutableState.value = VehicleEditorUiState.Missing
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = state.copy(storageError = "Couldn’t save changes. Try again.")
            }
        }
    }

    private fun setDraft(draft: VehicleDraft, errors: Map<String, String> = emptyMap(), storageError: String? = null) {
        savedState["draftInitialized"] = true
        savedState["id"] = draft.id
        savedState["brand"] = draft.brand
        savedState["model"] = draft.model
        savedState["year"] = draft.year
        savedState["plateNumber"] = draft.plateNumber
        savedState["currentMileageKm"] = draft.currentMileageKm
        savedState["photoPath"] = draft.photoPath
        mutableState.value = VehicleEditorUiState.Form(draft, errors, storageError = storageError)
    }

    private fun readDraft() = VehicleDraft(
        id = savedState.get<Long>("id") ?: vehicleId,
        brand = savedState.get<String>("brand").orEmpty(),
        model = savedState.get<String>("model").orEmpty(),
        year = savedState.get<String>("year").orEmpty(),
        plateNumber = savedState.get<String>("plateNumber").orEmpty(),
        currentMileageKm = savedState.get<String>("currentMileageKm").orEmpty(),
        photoPath = savedState.get<String>("photoPath")
    )

    companion object {
        fun provideFactory(repository: CorsaRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { VehicleEditorViewModel(repository, createSavedStateHandle()) }
        }
    }
}
