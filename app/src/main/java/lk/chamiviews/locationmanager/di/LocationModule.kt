package lk.chamiviews.locationmanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import lk.chamiviews.locationmanager.data.FusedLocationManager
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import lk.chamiviews.locationmanager.domain.usecase.GetCurrentLocationUseCase
import lk.chamiviews.locationmanager.domain.usecase.GetLastLocationUseCase
import lk.chamiviews.locationmanager.domain.usecase.GetLocationUpdatesUseCase
import lk.chamiviews.locationmanager.domain.usecase.GetProximateLocationUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager {
        return FusedLocationManager(context)
    }

    @Provides
    @Singleton
    fun provideGetLastLocationUseCase(locationManager: LocationManager): GetLastLocationUseCase {
        return GetLastLocationUseCase(locationManager)
    }

    @Provides
    @Singleton
    fun provideGetCurrentLocationUseCase(locationManager: LocationManager): GetCurrentLocationUseCase {
        return GetCurrentLocationUseCase(locationManager)
    }

    @Provides
    @Singleton
    fun provideGetLocationUpdatesUseCase(locationManager: LocationManager): GetLocationUpdatesUseCase {
        return GetLocationUpdatesUseCase(locationManager)
    }

    @Provides
    @Singleton
    fun provideGetProximateLocationUseCase(locationManager: LocationManager): GetProximateLocationUseCase {
        return GetProximateLocationUseCase(locationManager)
    }
}
