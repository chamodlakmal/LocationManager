package lk.chamiviews.locationmanager.presentation.viewmodel

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.model.LocationRequestParams
import lk.chamiviews.locationmanager.domain.usecase.GetLocationUpdatesUseCase
import lk.chamiviews.locationmanager.presentation.model.LocationUpdatesUiState
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlinx.coroutines.Dispatchers

/**
 * Unit tests for [LocationUpdatesViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class LocationUpdatesViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var mockGetLocationUpdatesUseCase: GetLocationUpdatesUseCase

    @MockK
    private lateinit var mockLocation1: Location

    @MockK
    private lateinit var mockLocation2: Location

    private lateinit var locationUpdatesViewModel: LocationUpdatesViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        locationUpdatesViewModel = LocationUpdatesViewModel(mockGetLocationUpdatesUseCase)

        // Setup default location mocks
        every { mockLocation1.latitude } returns 6.9271
        every { mockLocation1.longitude } returns 79.9612
        every { mockLocation1.accuracy } returns 10.0f
        every { mockLocation1.time } returns System.currentTimeMillis()
        every { mockLocation1.provider } returns "gps"

        every { mockLocation2.latitude } returns 6.9300
        every { mockLocation2.longitude } returns 79.9650
        every { mockLocation2.accuracy } returns 8.0f
        every { mockLocation2.time } returns System.currentTimeMillis() + 5000
        every { mockLocation2.provider } returns "gps"
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `initial state has correct default values`() = testScope.runTest {
        // When
        locationUpdatesViewModel.uiState.test {
            val initialState = awaitItem()

            // Then
            assertEquals(LocationUpdatesUiState(), initialState)
            assertFalse(initialState.isTracking)
            assertTrue(initialState.locations.isEmpty())
            assertNull(initialState.currentLocation)
            assertNull(initialState.error)
            assertFalse(initialState.isPermissionGranted)
            assertEquals(0, initialState.totalUpdates)
        }
    }

    @Test
    fun `updatePermissionStatus updates permission state correctly`() = testScope.runTest {
        // When
        locationUpdatesViewModel.updatePermissionStatus(true)

        // Then
        locationUpdatesViewModel.uiState.test {
            val updatedState = awaitItem()
            assertTrue(updatedState.isPermissionGranted)
        }
    }

    @Test
    fun `updatePermissionStatus to false stops tracking when currently tracking`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val mockFlow = flowOf(Result.success(mockLocation1))
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        // Start tracking
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        locationUpdatesViewModel.updatePermissionStatus(false)

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isPermissionGranted)
            assertFalse(state.isTracking)
        }
    }

    @Test
    fun `startLocationUpdates shows error when permission not granted`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(false)

        // When
        locationUpdatesViewModel.startLocationUpdates()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Location permission not granted", state.error)
            assertFalse(state.isTracking)
        }

        coVerify(exactly = 0) { mockGetLocationUpdatesUseCase.invoke(any()) }
    }

    @Test
    fun `startLocationUpdates sets tracking state and processes location updates`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val mockFlow = flowOf(
            Result.success(mockLocation1),
            Result.success(mockLocation2)
        )
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        // When
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        locationUpdatesViewModel.uiState.test {
            val finalState = awaitItem()

            assertTrue(finalState.isTracking)
            assertEquals(mockLocation2, finalState.currentLocation)
            assertEquals(2, finalState.locations.size)
            assertEquals(2, finalState.totalUpdates)
            assertNull(finalState.error)

            // Verify locations are in reverse chronological order (newest first)
            assertEquals(mockLocation2, finalState.locations[0].location)
            assertEquals(mockLocation1, finalState.locations[1].location)
        }

        coVerify(exactly = 1) { mockGetLocationUpdatesUseCase.invoke(any()) }
    }

    @Test
    fun `startLocationUpdates with custom parameters passes correct params to use case`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val mockFlow = flowOf(Result.success(mockLocation1))
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        val priority = LocationPriority.HIGH_ACCURACY
        val intervalMs = 10000L
        val fastestIntervalMs = 5000L

        // When
        locationUpdatesViewModel.startLocationUpdates(priority, intervalMs, fastestIntervalMs)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val expectedParams = LocationRequestParams(
            priority = priority,
            locationInterval = intervalMs,
            fastestLocationInterval = fastestIntervalMs,
            maxUpdateDelayMillis = intervalMs * 3,
            waitForAccurateLocation = false
        )

        coVerify(exactly = 1) { mockGetLocationUpdatesUseCase.invoke(expectedParams) }
    }

    @Test
    fun `startLocationUpdates stops previous updates before starting new ones`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val mockFlow1 = flowOf(Result.success(mockLocation1))
        val mockFlow2 = flowOf(Result.success(mockLocation2))

        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow1 andThen mockFlow2

        // Start first tracking
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Start second tracking
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isTracking)
            // Should have cleared previous locations and reset counter
            assertEquals(1, state.totalUpdates)
            assertEquals(1, state.locations.size)
        }

        coVerify(exactly = 2) { mockGetLocationUpdatesUseCase.invoke(any()) }
    }

    @Test
    fun `startLocationUpdates handles flow exception correctly`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val exception = RuntimeException("GPS unavailable")
        val mockFlow = flow<Result<Location>> {
            throw exception
        }
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        // When
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isTracking)
            assertEquals("GPS unavailable", state.error)
        }
    }

    @Test
    fun `startLocationUpdates handles flow with failure result correctly`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val exception = RuntimeException("Location error")
        val mockFlow = flowOf(Result.failure<Location>(exception))
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        // When
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isTracking) // Still tracking but has error
            assertEquals("Location error", state.error)
            assertEquals(0, state.totalUpdates)
        }
    }

    @Test
    fun `startLocationUpdates handles catch block for flow errors`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val exception = RuntimeException("Flow error")
        val mockFlow = flow<Result<Location>> {
            emit(Result.success(mockLocation1))
            throw exception
        }
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        // When
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isTracking)
            assertEquals("Flow error", state.error)
        }
    }

    @Test
    fun `startLocationUpdates limits locations to 20 items`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val locations = (1..25).map { index ->
            mockk<Location> {
                every { latitude } returns 6.9271 + index * 0.001
                every { longitude } returns 79.9612 + index * 0.001
                every { accuracy } returns 10.0f
                every { time } returns System.currentTimeMillis() + index * 1000
                every { provider } returns "gps"
            }
        }

        val results = locations.map { Result.success(it) }
        val mockFlow = flowOf(*results.toTypedArray())
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        // When
        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertEquals(20, state.locations.size)
            assertEquals(25, state.totalUpdates)
            // Verify newest locations are kept (last 20)
            assertEquals(locations[24], state.locations[0].location) // Newest first
            assertEquals(locations[5], state.locations[19].location) // 20th newest
        }
    }

    @Test
    fun `stopLocationUpdates stops tracking`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val mockFlow = flowOf(Result.success(mockLocation1))
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        locationUpdatesViewModel.stopLocationUpdates()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isTracking)
        }
    }

    @Test
    fun `clearLocations clears location data`() = testScope.runTest {
        // Given
        locationUpdatesViewModel.updatePermissionStatus(true)
        val mockFlow = flowOf(Result.success(mockLocation1))
        coEvery { mockGetLocationUpdatesUseCase.invoke(any()) } returns mockFlow

        locationUpdatesViewModel.startLocationUpdates()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        locationUpdatesViewModel.clearLocations()

        // Then
        locationUpdatesViewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.locations.isEmpty())
            assertEquals(0, state.totalUpdates)
            assertNull(state.currentLocation)
        }
    }

    @Test
    fun `clearError clears error state`() = testScope.runTest {
        // Given - Set an error state first
        locationUpdatesViewModel.updatePermissionStatus(false)
        locationUpdatesViewModel.startLocationUpdates()

        // Verify error is set
        locationUpdatesViewModel.uiState.test {
            val stateWithError = awaitItem()
            assertNotNull(stateWithError.error)
            cancelAndIgnoreRemainingEvents()
        }

        // When
        locationUpdatesViewModel.clearError()

        // Then
        locationUpdatesViewModel.uiState.test {
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

}
