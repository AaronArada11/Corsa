package com.corsa.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.corsa.app.data.CorsaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Garage(val vehicleCount: Int) : DashboardUiState
    data object Error : DashboardUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: CorsaRepository) : ViewModel() {
    private val retryGeneration = MutableStateFlow(0)
    val uiState: StateFlow<DashboardUiState> = retryGeneration.flatMapLatest {
        repository.observeAllVehicles().map { if (it.isEmpty()) DashboardUiState.Empty else DashboardUiState.Garage(it.size) }
            .onStart { emit(DashboardUiState.Loading) }.catch { emit(DashboardUiState.Error) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)
    fun retry() { retryGeneration.value += 1 }
    companion object {
        fun provideFactory(repository: CorsaRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repository) as T
        }
    }
}
