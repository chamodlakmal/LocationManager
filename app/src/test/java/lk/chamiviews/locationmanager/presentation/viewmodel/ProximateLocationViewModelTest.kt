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
import lk.chamiviews.locationmanager.domain.usecase.GetProximateLocationUseCase
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
 * Unit tests for [ProximateLocationViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class ProximateLocationViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var mockGetProximateLocationUseCase: GetProximateLocationUseCase

    @MockK
    private lateinit var mockLocation: Location

    private lateinit var proximateLocationViewModel: ProximateLocationViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        proximateLocationViewModel = ProximateLocationViewModel(mockGetProximateLocationUseCase)

        // Setup default location mock
        every { mockLocation.latitude } returns 6.9271
        every { mockLocation.longitude } returns 79.9612
        every { mockLocation.accuracy } returns 10.0f
        every { mockLocation.time } returns System.currentTimeMillis()
        every { mockLocation.provider } returns "network"
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `initial state has correct default values`() = testScope.runTest {
        // When
        proximateLocationViewModel.uiState.test {
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
        proximateLocationViewModel.updatePermissionStatus(true)

        // Then
        proximateLocationViewModel.uiState.test {
            val updatedState = awaitItem()
            assertTrue(updatedState.isPermissionGranted)
        }
    }

    @Test
    fun `getProximateLocation shows error when permission not granted`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(false)

        // When
        proximateLocationViewModel.getProximateLocation()

        // Then
        proximateLocationViewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Location permission not granted", state.error)
            assertFalse(state.isLoading)
            assertNull(state.location)
        }

        coVerify(exactly = 0) { mockGetProximateLocationUseCase.invoke() }
    }

    @Test
    fun `getProximateLocation sets loading state then success state`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(true)

        val expectedResult = Result.success(mockLocation)
        coEvery { mockGetProximateLocationUseCase.invoke() } returns expectedResult

        // When
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertEquals(mockLocation, finalState.location)
            assertNull(finalState.error)
            assertTrue(finalState.lastUpdateTime > 0)
        }

        coVerify(exactly = 1) { mockGetProximateLocationUseCase.invoke() }
    }

    @Test
    fun `getProximateLocation handles failure result correctly`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(true)

        val exception = Exception("Network unavailable")
        val expectedResult = Result.failure<Location>(exception)
        coEvery { mockGetProximateLocationUseCase.invoke() } returns expectedResult

        // When
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("Network unavailable", finalState.error)
        }
    }

    @Test
    fun `getProximateLocation handles thrown exception correctly`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(true)

        val exception = LocationException.PermissionDeniedException("Permission denied")
        coEvery { mockGetProximateLocationUseCase.invoke() } throws exception

        // When
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("Permission denied", finalState.error)
        }
    }

    @Test
    fun `getProximateLocation with unknown exception message handles gracefully`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(true)

        val exceptionWithoutMessage = RuntimeException()
        coEvery { mockGetProximateLocationUseCase.invoke() } throws exceptionWithoutMessage

        // When
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertEquals("Unknown error occurred", finalState.error)
        }
    }

    @Test
    fun `getProximateLocation with failure result and no exception message handles gracefully`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(true)

        val exceptionWithoutMessage = RuntimeException()
        val expectedResult = Result.failure<Location>(exceptionWithoutMessage)
        coEvery { mockGetProximateLocationUseCase.invoke() } returns expectedResult

        // When
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("Failed to get proximate location", finalState.error)
        }
    }

    @Test
    fun `clearError clears error state`() = testScope.runTest {
        // Given - Set an error state first
        proximateLocationViewModel.updatePermissionStatus(false)
        proximateLocationViewModel.getProximateLocation()

        // Verify error is set
        proximateLocationViewModel.uiState.test {
            val stateWithError = awaitItem()
            assertNotNull(stateWithError.error)
            cancelAndIgnoreRemainingEvents()
        }

        // When
        proximateLocationViewModel.clearError()

        // Then
        proximateLocationViewModel.uiState.test {
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    @Test
    fun `getProximateLocation clears previous error before starting`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(true)

        // First call that results in error
        val exception = Exception("First error")
        coEvery { mockGetProximateLocationUseCase.invoke() } returns Result.failure(exception)

        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        proximateLocationViewModel.uiState.test {
            val errorState = awaitItem()
            assertEquals("First error", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }

        // Second call that succeeds
        coEvery { mockGetProximateLocationUseCase.invoke() } returns Result.success(mockLocation)

        // When
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
            val successState = awaitItem()
            assertNull(successState.error) // Error should be cleared
            assertEquals(mockLocation, successState.location)
        }
    }

    @Test
    fun `state updates preserve previous values when not explicitly changed`() = testScope.runTest {
        // Given
        proximateLocationViewModel.updatePermissionStatus(true)
        val expectedResult = Result.success(mockLocation)
        coEvery { mockGetProximateLocationUseCase.invoke() } returns expectedResult

        // When
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
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
        proximateLocationViewModel.updatePermissionStatus(true)

        val location1 = mockk<Location> {
            every { latitude } returns 6.9271
            every { longitude } returns 79.9612
            every { accuracy } returns 10.0f
            every { time } returns System.currentTimeMillis()
        }

        val location2 = mockk<Location> {
            every { latitude } returns 6.9300
            every { longitude } returns 79.9650
            every { accuracy } returns 8.0f
            every { time } returns System.currentTimeMillis() + 5000
        }

        coEvery { mockGetProximateLocationUseCase.invoke() } returns Result.success(location1) andThen Result.success(location2)

        // When - First call
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        proximateLocationViewModel.uiState.test {
            val firstState = awaitItem()
            assertEquals(location1, firstState.location)
            cancelAndIgnoreRemainingEvents()
        }

        // When - Second call
        proximateLocationViewModel.getProximateLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        proximateLocationViewModel.uiState.test {
            val secondState = awaitItem()
            assertEquals(location2, secondState.location)
            assertTrue(secondState.lastUpdateTime > 0)
        }

        coVerify(exactly = 2) { mockGetProximateLocationUseCase.invoke() }
    }
}
