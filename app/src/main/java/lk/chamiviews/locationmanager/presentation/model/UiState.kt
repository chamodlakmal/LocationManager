package lk.chamiviews.locationmanager.presentation.model

import android.location.Location

/**
 * UI state for location-related screens
 */
data class LocationUiState(
    val isLoading: Boolean = false,
    val location: Location? = null,
    val error: String? = null,
    val isPermissionGranted: Boolean = false,
    val lastUpdateTime: Long = 0L
)

/**
 * UI state for continuous location updates
 */
data class LocationUpdatesUiState(
    val isTracking: Boolean = false,
    val locations: List<LocationInfo> = emptyList(),
    val currentLocation: Location? = null,
    val error: String? = null,
    val isPermissionGranted: Boolean = false,
    val totalUpdates: Int = 0
)

/**
 * Location information with timestamp for display
 */
data class LocationInfo(
    val location: Location,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float = location.accuracy,
    val provider: String? = location.provider
)

