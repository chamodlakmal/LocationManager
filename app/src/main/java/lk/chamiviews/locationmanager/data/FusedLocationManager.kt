package lk.chamiviews.locationmanager.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import lk.chamiviews.locationmanager.domain.exception.LocationException
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.model.LocationRequestParams
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FusedLocationManager(private val context: Context) : LocationManager {

    private var fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retrieves the last known location of the device.
     *
     * This method attempts to get the most recent historical location of the device that is currently available.
     * A retry mechanism has been implemented to address occasional instances where the fusedLocationManager may return a null location.
     * The process will attempt to retrieve the location up to a maximum of four times to ensure reliability.
     *
     * @return [Result] A result containing the [Location] on success or an exception on failure.
     * @throws [LocationException.PermissionDeniedException] if location permissions are not granted.
     */
    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): Result<Location> {
        if (!checkPermissions()) {
            throw LocationException.PermissionDeniedException("Permission Denied")
        }

        val maxRetries = 4
        var currentTry = 0

        while (currentTry <= maxRetries) {
            Log.d("RETRY_LAST_LOCATION", "$currentTry/$maxRetries")

            try {
                val location = suspendCoroutine<Location?> { continuation ->
                    fusedLocationProviderClient.lastLocation
                        .addOnSuccessListener { location ->
                            continuation.resume(location)
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(null)
                        }
                }

                if (location != null) {
                    return Result.success(location)
                } else if (currentTry < maxRetries) {
                    currentTry++
                    delay(1000)
                } else {
                    return Result.failure(Exception("Unable to retrieve the last location"))
                }
            } catch (exception: Exception) {
                if (currentTry < maxRetries) {
                    currentTry++
                    delay(1000)
                } else {
                    return Result.failure(Exception("Failed to get last location: ${exception.message}"))
                }
            }
        }

        return Result.failure(Exception("Unable to retrieve the last location"))
    }

    /**
     * Retrieves the current device location with the specified priority.
     *
     * This method attempts to get the current location of the device. It may return a cached location if a recent enough location fix is available,
     * or it may compute a new location. A retry mechanism has been implemented to address occasional instances where the
     * fusedLocationManager may return a null location. The process will attempt to retrieve the location up to a maximum of four times.
     *
     * @param priority The desired priority level of the location request. This parameter determines the accuracy of the location fix and the power consumption.
     * @return [Result] A result containing the [Location] on success or an exception on failure.
     * @throws [LocationException.PermissionDeniedException] if location permissions are not granted.
     */
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(priority: LocationPriority): Result<Location> {
        if (!checkPermissions()) {
            throw LocationException.PermissionDeniedException("Permission Denied")
        }

        val maxRetries = 4
        var currentTry = 0

        while (currentTry <= maxRetries) {
            Log.d("RETRY_CURRENT_LOCATION", "$currentTry/$maxRetries")

            try {
                val location = suspendCoroutine<Location?> { continuation ->
                    fusedLocationProviderClient.getCurrentLocation(
                        priority.priorityValue,
                        object : CancellationToken() {
                            override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                                CancellationTokenSource().token

                            override fun isCancellationRequested(): Boolean = false
                        }
                    ).addOnSuccessListener { location ->
                        continuation.resume(location)
                    }.addOnFailureListener { exception ->
                        continuation.resume(null)
                    }
                }

                if (location != null) {
                    return Result.success(location)
                } else if (currentTry < maxRetries) {
                    currentTry++
                    delay(1000)
                } else {
                    return Result.failure(Exception("Unable to retrieve a current location"))
                }
            } catch (exception: Exception) {
                if (currentTry < maxRetries) {
                    currentTry++
                    delay(1000)
                } else {
                    return Result.failure(Exception("Failed to get current location: ${exception.message}"))
                }
            }
        }

        return Result.failure(Exception("Unable to retrieve a current location"))
    }

    /**
     * Requests continuous location updates based on the provided [LocationRequestParams].
     *
     * This method initiates a request for continuous updates about the device's location. The updates are configured
     * according to the parameters specified in the [LocationRequestParams]. The updates will continue until the Flow is cancelled.
     *
     * @param locationRequestParams Parameters for configuring the location request.
     * @return [Flow] A Flow that emits [Result] objects containing either a [Location] on success or an exception on failure.
     * @throws [LocationException.PermissionDeniedException] if location permissions are not granted.
     */
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocationUpdates(locationRequestParams: LocationRequestParams): Flow<Result<Location>> =
        callbackFlow {
            if (!checkPermissions()) {
                throw LocationException.PermissionDeniedException("Permission Denied")
            }

            val locationRequest: LocationRequest = LocationRequest.Builder(
                locationRequestParams.priority.priorityValue, locationRequestParams.locationInterval
            ).setWaitForAccurateLocation(locationRequestParams.waitForAccurateLocation)
                .setMinUpdateIntervalMillis(locationRequestParams.fastestLocationInterval)
                .setMaxUpdateDelayMillis(locationRequestParams.maxUpdateDelayMillis).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val locationList = locationResult.locations
                    if (locationList.isNotEmpty()) {
                        val location = locationList.last()
                        trySend(Result.success(location))
                    } else {
                        trySend(Result.failure(Exception("Unable to retrieve a location list")))
                    }
                }
            }

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, android.os.Looper.getMainLooper()
            )

            awaitClose {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }

    /**
     * Asynchronously gets the most accurate and up-to-date location of the device.
     *
     * This function checks for the necessary location permissions before attempting to get the location.
     * If permissions are not granted, it throws a [LocationException.PermissionDeniedException].
     * It first tries to get the last known location of the device. If it's not available or outdated,
     * it requests a new location update with high accuracy.
     *
     * @return [Result] A result containing the [Location] on success or an exception on failure.
     * @throws [LocationException.PermissionDeniedException] if location permissions are not granted.
     */
    override suspend fun getProximateLocation(): Result<Location> {
        if (!checkPermissions()) {
            throw LocationException.PermissionDeniedException("Permission Denied")
        }

        val lastLocationResult = getLastLocation()
        return if (lastLocationResult.isSuccess) {
            lastLocationResult
        } else {
            getCurrentLocation(LocationPriority.PRIORITY_HIGH_ACCURACY)
        }
    }
}
