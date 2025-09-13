package lk.chamiviews.locationmanager.domain.usecase

import android.location.Location
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Use case for retrieving the most accurate and up-to-date location available.
 */
@ViewModelScoped
class GetProximateLocationUseCase @Inject constructor(
    private val locationManager: LocationManager
) {
    suspend operator fun invoke(): Result<Location> {
        return locationManager.getProximateLocation()
    }
}
