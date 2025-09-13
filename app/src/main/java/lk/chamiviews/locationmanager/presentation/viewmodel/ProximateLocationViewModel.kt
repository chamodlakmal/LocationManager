package lk.chamiviews.locationmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.chamiviews.locationmanager.domain.usecase.GetProximateLocationUseCase
import lk.chamiviews.locationmanager.presentation.model.LocationUiState
import javax.inject.Inject

/**
 * ViewModel for managing proximate location functionality
 */
@HiltViewModel
class ProximateLocationViewModel @Inject constructor(
    private val getProximateLocationUseCase: GetProximateLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    fun updatePermissionStatus(isGranted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = isGranted) }
    }

    fun getProximateLocation() {
        if (!_uiState.value.isPermissionGranted) {
            _uiState.update { it.copy(error = "Location permission not granted") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = getProximateLocationUseCase()
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
                            error = result.exceptionOrNull()?.message ?: "Failed to get proximate location"
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
