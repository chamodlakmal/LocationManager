package lk.chamiviews.locationmanager.domain.usecase

import android.location.Location
import kotlinx.coroutines.flow.Flow
import lk.chamiviews.locationmanager.domain.model.LocationRequestParams
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Use case for retrieving continuous location updates.
 */
@ViewModelScoped
class GetLocationUpdatesUseCase @Inject constructor(
    private val locationManager: LocationManager
) {
    suspend operator fun invoke(params: LocationRequestParams = LocationRequestParams()): Flow<Result<Location>> {
        return locationManager.getCurrentLocationUpdates(params)
    }
}
