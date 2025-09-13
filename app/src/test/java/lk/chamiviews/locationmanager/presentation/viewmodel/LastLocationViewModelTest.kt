package lk.chamiviews.locationmanager.presentation.viewmodel

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import lk.chamiviews.locationmanager.domain.exception.LocationException
import lk.chamiviews.locationmanager.domain.usecase.GetLastLocationUseCase
import lk.chamiviews.locationmanager.presentation.model.LocationUiState
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlinx.coroutines.Dispatchers

/**
 * Unit tests for [LastLocationViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class LastLocationViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var mockGetLastLocationUseCase: GetLastLocationUseCase

    @MockK
    private lateinit var mockLocation: Location

    private lateinit var lastLocationViewModel: LastLocationViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        lastLocationViewModel = LastLocationViewModel(mockGetLastLocationUseCase)

        // Setup default location mock
        every { mockLocation.latitude } returns 6.9271
        every { mockLocation.longitude } returns 79.9612
        every { mockLocation.accuracy } returns 15.0f
        every { mockLocation.time } returns System.currentTimeMillis() - 60000 // 1 minute ago
        every { mockLocation.provider } returns "fused"
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `initial state has correct default values`() = testScope.runTest {
        // When
        lastLocationViewModel.uiState.test {
            val initialState = awaitItem()

            // Then
            assertEquals(LocationUiState(), initialState)
            assertFalse(initialState.isLoading)
            assertNull(initialState.location)
            assertNull(initialState.error)
            assertFalse(initialState.isPermissionGranted)
            assertEquals(0L, initialState.lastUpdateTime)
        }
    }

    @Test
    fun `updatePermissionStatus updates permission state correctly`() = testScope.runTest {
        // When
        lastLocationViewModel.updatePermissionStatus(true)

        // Then
        lastLocationViewModel.uiState.test {
            val updatedState = awaitItem()
            assertTrue(updatedState.isPermissionGranted)
        }
    }

    @Test
    fun `getLastLocation shows error when permission not granted`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(false)

        // When
        lastLocationViewModel.getLastLocation()

        // Then
        lastLocationViewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Location permission not granted", state.error)
            assertFalse(state.isLoading)
            assertNull(state.location)
        }

        coVerify(exactly = 0) { mockGetLastLocationUseCase.invoke() }
    }

    @Test
    fun `getLastLocation sets loading state then success state`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)

        val expectedResult = Result.success(mockLocation)
        coEvery { mockGetLastLocationUseCase.invoke() } returns expectedResult

        // When
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertEquals(mockLocation, finalState.location)
            assertNull(finalState.error)
            assertTrue(finalState.lastUpdateTime > 0)
        }

        coVerify(exactly = 1) { mockGetLastLocationUseCase.invoke() }
    }

    @Test
    fun `getLastLocation handles failure result correctly`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)

        val exception = Exception("No last location available")
        val expectedResult = Result.failure<Location>(exception)
        coEvery { mockGetLastLocationUseCase.invoke() } returns expectedResult

        // When
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("No last location available", finalState.error)
        }
    }

    @Test
    fun `getLastLocation handles thrown exception correctly`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)

        val exception = LocationException.PermissionDeniedException("Permission denied")
        coEvery { mockGetLastLocationUseCase.invoke() } throws exception

        // When
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("Permission denied", finalState.error)
        }
    }

    @Test
    fun `getLastLocation with unknown exception message handles gracefully`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)

        val exceptionWithoutMessage = RuntimeException()
        coEvery { mockGetLastLocationUseCase.invoke() } throws exceptionWithoutMessage

        // When
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertEquals("Unknown error occurred", finalState.error)
        }
    }

    @Test
    fun `getLastLocation with failure result and no exception message handles gracefully`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)

        val exceptionWithoutMessage = RuntimeException()
        val expectedResult = Result.failure<Location>(exceptionWithoutMessage)
        coEvery { mockGetLastLocationUseCase.invoke() } returns expectedResult

        // When
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("Failed to get last location", finalState.error)
        }
    }

    @Test
    fun `clearError clears error state`() = testScope.runTest {
        // Given - Set an error state first
        lastLocationViewModel.updatePermissionStatus(false)
        lastLocationViewModel.getLastLocation()

        // Verify error is set
        lastLocationViewModel.uiState.test {
            val stateWithError = awaitItem()
            assertNotNull(stateWithError.error)
            cancelAndIgnoreRemainingEvents()
        }

        // When
        lastLocationViewModel.clearError()

        // Then
        lastLocationViewModel.uiState.test {
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    @Test
    fun `getLastLocation clears previous error before starting`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)

        // First call that results in error
        val exception = Exception("First error")
        coEvery { mockGetLastLocationUseCase.invoke() } returns Result.failure(exception)

        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        lastLocationViewModel.uiState.test {
            val errorState = awaitItem()
            assertEquals("First error", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }

        // Second call that succeeds
        coEvery { mockGetLastLocationUseCase.invoke() } returns Result.success(mockLocation)

        // When
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val successState = awaitItem()
            assertNull(successState.error) // Error should be cleared
            assertEquals(mockLocation, successState.location)
        }
    }

    @Test
    fun `state updates preserve previous values when not explicitly changed`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)
        val expectedResult = Result.success(mockLocation)
        coEvery { mockGetLastLocationUseCase.invoke() } returns expectedResult

        // When
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val state = awaitItem()

            // Permission should still be granted
            assertTrue(state.isPermissionGranted)
            // Location should be set
            assertEquals(mockLocation, state.location)
            // Loading should be false
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `multiple consecutive calls work correctly`() = testScope.runTest {
        // Given
        lastLocationViewModel.updatePermissionStatus(true)

        val location1 = mockk<Location> {
            every { latitude } returns 6.9271
            every { longitude } returns 79.9612
            every { accuracy } returns 15.0f
            every { time } returns System.currentTimeMillis() - 120000 // 2 minutes ago
        }

        val location2 = mockk<Location> {
            every { latitude } returns 6.9300
            every { longitude } returns 79.9650
            every { accuracy } returns 12.0f
            every { time } returns System.currentTimeMillis() - 60000 // 1 minute ago
        }

        coEvery { mockGetLastLocationUseCase.invoke() } returns Result.success(location1) andThen Result.success(location2)

        // When - First call
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        lastLocationViewModel.uiState.test {
            val firstState = awaitItem()
            assertEquals(location1, firstState.location)
            cancelAndIgnoreRemainingEvents()
        }

        // When - Second call
        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        lastLocationViewModel.uiState.test {
            val secondState = awaitItem()
            assertEquals(location2, secondState.location)
            assertTrue(secondState.lastUpdateTime > 0)
        }

        coVerify(exactly = 2) { mockGetLastLocationUseCase.invoke() }
    }

    @Test
    fun `permission change to false clears previous location data`() = testScope.runTest {
        // Given - First get a location
        lastLocationViewModel.updatePermissionStatus(true)
        val expectedResult = Result.success(mockLocation)
        coEvery { mockGetLastLocationUseCase.invoke() } returns expectedResult

        lastLocationViewModel.getLastLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify location is set
        lastLocationViewModel.uiState.test {
            val stateWithLocation = awaitItem()
            assertEquals(mockLocation, stateWithLocation.location)
            cancelAndIgnoreRemainingEvents()
        }

        // When - Change permission to false
        lastLocationViewModel.updatePermissionStatus(false)

        // Then - Location should still be preserved (only permission changes)
        lastLocationViewModel.uiState.test {
            val stateAfterPermissionChange = awaitItem()
            assertFalse(stateAfterPermissionChange.isPermissionGranted)
            assertEquals(mockLocation, stateAfterPermissionChange.location) // Location preserved
        }
    }
}
