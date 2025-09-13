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
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.usecase.GetCurrentLocationUseCase
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
 * Unit tests for [CurrentLocationViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CurrentLocationViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var mockGetCurrentLocationUseCase: GetCurrentLocationUseCase

    @MockK
    private lateinit var mockLocation: Location

    private lateinit var currentLocationViewModel: CurrentLocationViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        currentLocationViewModel = CurrentLocationViewModel(mockGetCurrentLocationUseCase)

        // Setup default location mock
        every { mockLocation.latitude } returns 6.9271
        every { mockLocation.longitude } returns 79.9612
        every { mockLocation.accuracy } returns 10.0f
        every { mockLocation.time } returns System.currentTimeMillis()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `initial state has correct default values`() = testScope.runTest {
        // When
        currentLocationViewModel.uiState.test {
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
    fun `initial selected priority is BALANCED_POWER_ACCURACY`() = testScope.runTest {
        // When
        currentLocationViewModel.selectedPriority.test {
            val initialPriority = awaitItem()

            // Then
            assertEquals(LocationPriority.BALANCED_POWER_ACCURACY, initialPriority)
        }
    }

    @Test
    fun `updatePermissionStatus updates permission state correctly`() = testScope.runTest {
        // When
        currentLocationViewModel.updatePermissionStatus(true)

        // Then
        currentLocationViewModel.uiState.test {
            val updatedState = awaitItem()
            assertTrue(updatedState.isPermissionGranted)
        }
    }

    @Test
    fun `updatePriority updates selected priority correctly`() = testScope.runTest {
        // Given
        val newPriority = LocationPriority.HIGH_ACCURACY

        // When
        currentLocationViewModel.updatePriority(newPriority)

        // Then
        currentLocationViewModel.selectedPriority.test {
            val updatedPriority = awaitItem()
            assertEquals(newPriority, updatedPriority)
        }
    }

    @Test
    fun `getCurrentLocation shows error when permission not granted`() = testScope.runTest {
        // Given
        currentLocationViewModel.updatePermissionStatus(false)

        // When
        currentLocationViewModel.getCurrentLocation()

        // Then
        currentLocationViewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Location permission not granted", state.error)
            assertFalse(state.isLoading)
            assertNull(state.location)
        }

        coVerify(exactly = 0) { mockGetCurrentLocationUseCase.invoke(any()) }
    }

    @Test
    fun `getCurrentLocation sets loading state then success state`() = testScope.runTest {
        // Given
        currentLocationViewModel.updatePermissionStatus(true)
        currentLocationViewModel.updatePriority(LocationPriority.HIGH_ACCURACY)

        val expectedResult = Result.success(mockLocation)
        coEvery { mockGetCurrentLocationUseCase.invoke(LocationPriority.HIGH_ACCURACY) } returns expectedResult

        // When
        currentLocationViewModel.getCurrentLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        currentLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertEquals(mockLocation, finalState.location)
            assertNull(finalState.error)
            assertTrue(finalState.lastUpdateTime > 0)
        }

        coVerify(exactly = 1) { mockGetCurrentLocationUseCase.invoke(LocationPriority.HIGH_ACCURACY) }
    }

    @Test
    fun `getCurrentLocation handles failure result correctly`() = testScope.runTest {
        // Given
        currentLocationViewModel.updatePermissionStatus(true)

        val exception = Exception("GPS unavailable")
        val expectedResult = Result.failure<Location>(exception)
        coEvery { mockGetCurrentLocationUseCase.invoke(any()) } returns expectedResult

        // When
        currentLocationViewModel.getCurrentLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        currentLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("GPS unavailable", finalState.error)
        }
    }

    @Test
    fun `getCurrentLocation handles thrown exception correctly`() = testScope.runTest {
        // Given
        currentLocationViewModel.updatePermissionStatus(true)

        val exception = LocationException.PermissionDeniedException("Permission denied")
        coEvery { mockGetCurrentLocationUseCase.invoke(any()) } throws exception

        // When
        currentLocationViewModel.getCurrentLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        currentLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertNull(finalState.location)
            assertEquals("Permission denied", finalState.error)
        }
    }

    @Test
    fun `getCurrentLocation uses currently selected priority`() = testScope.runTest {
        // Given
        currentLocationViewModel.updatePermissionStatus(true)

        val priorities = listOf(
            LocationPriority.HIGH_ACCURACY,
            LocationPriority.LOW_POWER,
            LocationPriority.PASSIVE
        )

        priorities.forEach { priority ->
            currentLocationViewModel.updatePriority(priority)
            val expectedResult = Result.success(mockLocation)
            coEvery { mockGetCurrentLocationUseCase.invoke(priority) } returns expectedResult

            // When
            currentLocationViewModel.getCurrentLocation()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mockGetCurrentLocationUseCase.invoke(priority) }

            clearMocks(mockGetCurrentLocationUseCase, answers = false)
        }
    }

    @Test
    fun `clearError clears error state`() = testScope.runTest {
        // Given - Set an error state first
        currentLocationViewModel.updatePermissionStatus(false)
        currentLocationViewModel.getCurrentLocation()

        // Verify error is set
        currentLocationViewModel.uiState.test {
            val stateWithError = awaitItem()
            assertNotNull(stateWithError.error)
            cancelAndIgnoreRemainingEvents()
        }

        // When
        currentLocationViewModel.clearError()

        // Then
        currentLocationViewModel.uiState.test {
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    @Test
    fun `multiple priority changes update selected priority correctly`() = testScope.runTest {
        // Given
        val priorities = listOf(
            LocationPriority.HIGH_ACCURACY,
            LocationPriority.BALANCED_POWER_ACCURACY,
            LocationPriority.LOW_POWER,
            LocationPriority.PASSIVE,
            LocationPriority.HIGH_ACCURACY
        )

        // When & Then
        priorities.forEach { priority ->
            currentLocationViewModel.updatePriority(priority)

            currentLocationViewModel.selectedPriority.test {
                val currentPriority = awaitItem()
                assertEquals(priority, currentPriority)
            }
        }
    }

    @Test
    fun `getCurrentLocation with unknown exception message handles gracefully`() = testScope.runTest {
        // Given
        currentLocationViewModel.updatePermissionStatus(true)

        val exceptionWithoutMessage = RuntimeException()
        coEvery { mockGetCurrentLocationUseCase.invoke(any()) } throws exceptionWithoutMessage

        // When
        currentLocationViewModel.getCurrentLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        currentLocationViewModel.uiState.test {
            val finalState = awaitItem()

            assertFalse(finalState.isLoading)
            assertEquals("Unknown error occurred", finalState.error)
        }
    }

    @Test
    fun `state updates preserve previous values when not explicitly changed`() = testScope.runTest {
        // Given
        currentLocationViewModel.updatePermissionStatus(true)
        val expectedResult = Result.success(mockLocation)
        coEvery { mockGetCurrentLocationUseCase.invoke(any()) } returns expectedResult

        // When
        currentLocationViewModel.getCurrentLocation()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        currentLocationViewModel.uiState.test {
            val state = awaitItem()

            // Permission should still be granted
            assertTrue(state.isPermissionGranted)
            // Location should be set
            assertEquals(mockLocation, state.location)
            // Loading should be false
            assertFalse(state.isLoading)
        }
    }
}
