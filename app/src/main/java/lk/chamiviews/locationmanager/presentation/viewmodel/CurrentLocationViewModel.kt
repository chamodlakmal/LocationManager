package lk.chamiviews.locationmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.usecase.GetCurrentLocationUseCase
import lk.chamiviews.locationmanager.presentation.model.LocationUiState
import javax.inject.Inject

/**
 * ViewModel for managing current location functionality
 */
@HiltViewModel
class CurrentLocationViewModel @Inject constructor(
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private val _selectedPriority = MutableStateFlow(LocationPriority.BALANCED_POWER_ACCURACY)
    val selectedPriority: StateFlow<LocationPriority> = _selectedPriority.asStateFlow()

    fun updatePermissionStatus(isGranted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = isGranted) }
    }

    fun updatePriority(priority: LocationPriority) {
        _selectedPriority.update { priority }
    }

    fun getCurrentLocation() {
        if (!_uiState.value.isPermissionGranted) {
            _uiState.update { it.copy(error = "Location permission not granted") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = getCurrentLocationUseCase(_selectedPriority.value)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            location = result.getOrNull(),
                            lastUpdateTime = System.currentTimeMillis(),
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to get current location"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
