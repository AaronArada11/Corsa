package com.corsa.app.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.corsa.app.data.CorsaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleDetailsViewModel(
    private val repository: CorsaRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val vehicleId = savedState.get<Long>("vehicleId") ?: -1L
    private val mutableState = MutableStateFlow<VehicleDetailsUiState>(VehicleDetailsUiState.Loading)
    val uiState: StateFlow<VehicleDetailsUiState> = mutableState.asStateFlow()
    private var observation: Job? = null
    private var deleting = false
    private var documentObservation: Job? = null

    init { retry() }

    fun retry() {
        observation?.cancel()
        if (vehicleId <= 0L) { mutableState.value = VehicleDetailsUiState.Missing; return }
        mutableState.value = VehicleDetailsUiState.Loading
        observation = viewModelScope.launch {
            try {
                repository.observeVehicle(vehicleId).collect { vehicle ->
                    if (!deleting) {
                        val previous = mutableState.value as? VehicleDetailsUiState.Content
                        mutableState.value = if (vehicle == null) VehicleDetailsUiState.Missing else previous?.copy(vehicle = vehicle) ?: VehicleDetailsUiState.Content(vehicle)
                        if (vehicle != null && documentObservation == null) retryDocuments()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = VehicleDetailsUiState.Error("Unable to load this vehicle")
            }
        }
    }

    fun retryDocuments() {
        documentObservation?.cancel()
        documentObservation = viewModelScope.launch {
            try {
                repository.observeVehicleDocuments(vehicleId).collect { documents ->
                    val content = mutableState.value as? VehicleDetailsUiState.Content ?: return@collect
                    mutableState.value = content.copy(documents = documents, documentError = null, documentsLoading = false)
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                val content = mutableState.value as? VehicleDetailsUiState.Content
                if (content != null) mutableState.value = content.copy(documentError = "Unable to load documents. Tap to retry.", documentsLoading = false)
            }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            try { repository.deleteDocument(id) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                val content = mutableState.value as? VehicleDetailsUiState.Content
                if (content != null) mutableState.value = content.copy(documentError = "Couldn’t delete document. Tap to retry.")
            }
        }
    }

    fun deleteConfirmed() {
        val content = mutableState.value as? VehicleDetailsUiState.Content ?: return
        if (deleting) return
        deleting = true
        mutableState.value = content.copy(deleting = true, deleteError = null)
        viewModelScope.launch {
            try {
                repository.deleteVehicle(vehicleId)
                mutableState.value = VehicleDetailsUiState.Deleted
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                deleting = false
                mutableState.value = content.copy(deleteError = "Couldn’t delete this car. Try again.")
            }
        }
    }

    companion object {
        fun provideFactory(repository: CorsaRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { VehicleDetailsViewModel(repository, createSavedStateHandle()) }
        }
    }
}
