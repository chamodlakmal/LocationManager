package lk.chamiviews.locationmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.model.LocationRequestParams
import lk.chamiviews.locationmanager.domain.usecase.GetLocationUpdatesUseCase
import lk.chamiviews.locationmanager.presentation.model.LocationInfo
import lk.chamiviews.locationmanager.presentation.model.LocationUpdatesUiState
import javax.inject.Inject

/**
 * ViewModel for managing continuous location updates
 */
@HiltViewModel
class LocationUpdatesViewModel @Inject constructor(
    private val getLocationUpdatesUseCase: GetLocationUpdatesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUpdatesUiState())
    val uiState: StateFlow<LocationUpdatesUiState> = _uiState.asStateFlow()

    private var locationUpdatesJob: Job? = null

    fun updatePermissionStatus(isGranted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = isGranted) }
        if (!isGranted && _uiState.value.isTracking) {
            stopLocationUpdates()
        }
    }

    fun startLocationUpdates(
        priority: LocationPriority = LocationPriority.BALANCED_POWER_ACCURACY,
        intervalMs: Long = 5000L,
        fastestIntervalMs: Long = 2000L
    ) {
        if (!_uiState.value.isPermissionGranted) {
            _uiState.update { it.copy(error = "Location permission not granted") }
            return
        }

        if (_uiState.value.isTracking) {
            stopLocationUpdates()
        }

        val params = LocationRequestParams(
            priority = priority,
            locationInterval = intervalMs,
            fastestLocationInterval = fastestIntervalMs,
            maxUpdateDelayMillis = intervalMs * 3,
            waitForAccurateLocation = false
        )

        locationUpdatesJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTracking = true,
                    error = null,
                    locations = emptyList(),
                    totalUpdates = 0
                )
            }

            try {
                getLocationUpdatesUseCase(params)
                    .catch { exception ->
                        _uiState.update {
                            it.copy(
                                isTracking = false,
                                error = exception.message ?: "Failed to get location updates"
                            )
                        }
                    }
                    .collect { result ->
                        if (result.isSuccess) {
                            val location = result.getOrNull()
                            if (location != null) {
                                val locationInfo = LocationInfo(location)
                                val currentLocations = _uiState.value.locations
                                val updatedLocations = (listOf(locationInfo) + currentLocations).take(20) // Keep last 20 updates

                                _uiState.update {
                                    it.copy(
                                        currentLocation = location,
                                        locations = updatedLocations,
                                        totalUpdates = it.totalUpdates + 1,
                                        error = null
                                    )
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    error = result.exceptionOrNull()?.message ?: "Failed to get location update"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTracking = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun stopLocationUpdates() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        _uiState.update { it.copy(isTracking = false) }
    }

    fun clearLocations() {
        _uiState.update {
            it.copy(
                locations = emptyList(),
                totalUpdates = 0,
                currentLocation = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
