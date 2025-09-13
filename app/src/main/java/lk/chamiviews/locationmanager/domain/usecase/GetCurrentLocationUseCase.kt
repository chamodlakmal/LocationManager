package lk.chamiviews.locationmanager.domain.usecase

import android.location.Location
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Use case for retrieving the current location.
 */
@ViewModelScoped
class GetCurrentLocationUseCase @Inject constructor(
    private val locationManager: LocationManager
) {
    suspend operator fun invoke(priority: LocationPriority = LocationPriority.BALANCED_POWER_ACCURACY): Result<Location> {
        return locationManager.getCurrentLocation(priority)
    }
}
