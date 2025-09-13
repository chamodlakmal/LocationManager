package lk.chamiviews.locationmanager.domain.usecase

import android.location.Location
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Use case for retrieving the last known location.
 */
@ViewModelScoped
class GetLastLocationUseCase @Inject constructor(
    private val locationManager: LocationManager
) {
    suspend operator fun invoke(): Result<Location> {
        return locationManager.getLastLocation()
    }
}
