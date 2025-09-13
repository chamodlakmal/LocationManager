package lk.chamiviews.locationmanager.domain.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.model.LocationRequestParams

/**
 * Interface for managing location services and retrieving device location information.
 */
interface LocationManager {

    /**
     * Retrieves the last known location of the device.
     *
     * @return [Result] A result containing the [Location] on success or an exception on failure.
     */
    suspend fun getLastLocation(): Result<Location>

    /**
     * Retrieves the current device location with the specified priority.
     *
     * @param priority The desired priority level of the location request.
     * @return [Result] A result containing the [Location] on success or an exception on failure.
     */
    suspend fun getCurrentLocation(priority: LocationPriority): Result<Location>

    /**
     * Requests continuous location updates based on the provided parameters.
     *
     * @param locationRequestParams Parameters for configuring the location request.
     * @return [Flow] A Flow that emits [Result] objects containing either a [Location] or an exception.
     */
    suspend fun getCurrentLocationUpdates(locationRequestParams: LocationRequestParams): Flow<Result<Location>>

    /**
     * Asynchronously gets the most accurate and up-to-date location of the device.
     *
     * @return [Result] A result containing the [Location] on success or an exception on failure.
     */
    suspend fun getProximateLocation(): Result<Location>
}
