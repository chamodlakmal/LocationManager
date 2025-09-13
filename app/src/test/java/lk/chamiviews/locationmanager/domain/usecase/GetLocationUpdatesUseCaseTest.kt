package lk.chamiviews.locationmanager.domain.usecase

import android.location.Location
import app.cash.turbine.test
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import lk.chamiviews.locationmanager.domain.exception.LocationException
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.model.LocationRequestParams
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [GetLocationUpdatesUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class GetLocationUpdatesUseCaseTest {

    @MockK
    private lateinit var mockLocationManager: LocationManager

    @MockK
    private lateinit var mockLocation1: Location

    @MockK
    private lateinit var mockLocation2: Location

    private lateinit var getLocationUpdatesUseCase: GetLocationUpdatesUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        getLocationUpdatesUseCase = GetLocationUpdatesUseCase(mockLocationManager)

        // Setup default location mocks
        every { mockLocation1.latitude } returns 6.9271
        every { mockLocation1.longitude } returns 79.9612
        every { mockLocation1.accuracy } returns 10.0f
        every { mockLocation1.time } returns System.currentTimeMillis()

        every { mockLocation2.latitude } returns 6.9275
        every { mockLocation2.longitude } returns 79.9615
        every { mockLocation2.accuracy } returns 8.0f
        every { mockLocation2.time } returns System.currentTimeMillis() + 5000
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `invoke with default parameters returns flow when LocationManager returns flow`() = runTest {
        // Given
        val expectedFlow = flowOf(Result.success(mockLocation1))
        coEvery { mockLocationManager.getCurrentLocationUpdates(LocationRequestParams()) } returns expectedFlow

        // When
        val resultFlow = getLocationUpdatesUseCase.invoke()

        // Then
        resultFlow.test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(mockLocation1, result.getOrNull())
            awaitComplete()
        }

        coVerify(exactly = 1) { mockLocationManager.getCurrentLocationUpdates(LocationRequestParams()) }
    }

    @Test
    fun `invoke with custom parameters passes parameters correctly to LocationManager`() = runTest {
        // Given
        val customParams = LocationRequestParams(
            priority = LocationPriority.HIGH_ACCURACY,
            locationInterval = 2000L,
            fastestLocationInterval = 1000L,
            maxUpdateDelayMillis = 5000L,
            waitForAccurateLocation = true
        )
        val expectedFlow = flowOf(Result.success(mockLocation1))
        coEvery { mockLocationManager.getCurrentLocationUpdates(customParams) } returns expectedFlow

        // When
        val resultFlow = getLocationUpdatesUseCase.invoke(customParams)

        // Then
        resultFlow.test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(mockLocation1, result.getOrNull())
            awaitComplete()
        }

        coVerify(exactly = 1) { mockLocationManager.getCurrentLocationUpdates(customParams) }
    }

    @Test
    fun `invoke returns flow with multiple location updates`() = runTest {
        // Given
        val expectedFlow = flowOf(
            Result.success(mockLocation1),
            Result.success(mockLocation2)
        )
        coEvery { mockLocationManager.getCurrentLocationUpdates(any()) } returns expectedFlow

        // When
        val resultFlow = getLocationUpdatesUseCase.invoke()

        // Then
        resultFlow.test {
            val result1 = awaitItem()
            assertTrue(result1.isSuccess)
            assertEquals(mockLocation1, result1.getOrNull())

            val result2 = awaitItem()
            assertTrue(result2.isSuccess)
            assertEquals(mockLocation2, result2.getOrNull())

            awaitComplete()
        }
    }

    @Test
    fun `invoke returns flow with failure when LocationManager returns failure`() = runTest {
        // Given
        val exception = Exception("Unable to retrieve location updates")
        val expectedFlow = flowOf(Result.failure<Location>(exception))
        coEvery { mockLocationManager.getCurrentLocationUpdates(any()) } returns expectedFlow

        // When
        val resultFlow = getLocationUpdatesUseCase.invoke()

        // Then
        resultFlow.test {
            val result = awaitItem()
            assertTrue(result.isFailure)
            assertEquals(exception.message, result.exceptionOrNull()?.message)
            awaitComplete()
        }
    }

    @Test
    fun `invoke propagates PermissionDeniedException when LocationManager throws it`() = runTest {
        // Given
        val exception = LocationException.PermissionDeniedException("Permission Denied")
        val expectedFlow = flow<Result<Location>> {
            throw exception
        }
        coEvery { mockLocationManager.getCurrentLocationUpdates(any()) } returns expectedFlow

        // When & Then
        try {
            getLocationUpdatesUseCase.invoke().test {
                awaitError()
            }
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }

        coVerify(exactly = 1) { mockLocationManager.getCurrentLocationUpdates(any()) }
    }

    @Test
    fun `invoke with different priorities calls LocationManager with correct parameters`() = runTest {
        // Given
        val priorities = listOf(
            LocationPriority.HIGH_ACCURACY,
            LocationPriority.BALANCED_POWER_ACCURACY,
            LocationPriority.LOW_POWER,
            LocationPriority.PASSIVE
        )

        priorities.forEach { priority ->
            val params = LocationRequestParams(priority = priority)
            val expectedFlow = flowOf(Result.success(mockLocation1))
            coEvery { mockLocationManager.getCurrentLocationUpdates(params) } returns expectedFlow

            // When
            val resultFlow = getLocationUpdatesUseCase.invoke(params)

            // Then
            resultFlow.test {
                val result = awaitItem()
                assertTrue("Failed for priority: $priority", result.isSuccess)
                awaitComplete()
            }

            coVerify(exactly = 1) { mockLocationManager.getCurrentLocationUpdates(params) }
            clearMocks(mockLocationManager, answers = false)
        }
    }

    @Test
    fun `invoke with mixed success and failure results handles both correctly`() = runTest {
        // Given
        val exception = Exception("Temporary GPS error")
        val expectedFlow = flowOf(
            Result.success(mockLocation1),
            Result.failure<Location>(exception),
            Result.success(mockLocation2)
        )
        coEvery { mockLocationManager.getCurrentLocationUpdates(any()) } returns expectedFlow

        // When
        val resultFlow = getLocationUpdatesUseCase.invoke()

        // Then
        resultFlow.test {
            // First success
            val result1 = awaitItem()
            assertTrue(result1.isSuccess)
            assertEquals(mockLocation1, result1.getOrNull())

            // Failure
            val result2 = awaitItem()
            assertTrue(result2.isFailure)
            assertEquals("Temporary GPS error", result2.exceptionOrNull()?.message)

            // Second success
            val result3 = awaitItem()
            assertTrue(result3.isSuccess)
            assertEquals(mockLocation2, result3.getOrNull())

            awaitComplete()
        }
    }

    @Test
    fun `invoke calls LocationManager exactly once per invocation`() = runTest {
        // Given
        val expectedFlow = flowOf(Result.success(mockLocation1))
        coEvery { mockLocationManager.getCurrentLocationUpdates(any()) } returns expectedFlow

        // When
        getLocationUpdatesUseCase.invoke()

        // Then
        coVerify(exactly = 1) { mockLocationManager.getCurrentLocationUpdates(any()) }
        confirmVerified(mockLocationManager)
    }

    @Test
    fun `invoke preserves LocationRequestParams integrity`() = runTest {
        // Given
        val specificParams = LocationRequestParams(
            priority = LocationPriority.HIGH_ACCURACY,
            locationInterval = 3000L,
            fastestLocationInterval = 1500L,
            maxUpdateDelayMillis = 8000L,
            waitForAccurateLocation = true
        )
        val expectedFlow = flowOf(Result.success(mockLocation1))

        val paramsSlot = slot<LocationRequestParams>()
        coEvery { mockLocationManager.getCurrentLocationUpdates(capture(paramsSlot)) } returns expectedFlow

        // When
        val resultFlow = getLocationUpdatesUseCase.invoke(specificParams)

        // Then
        resultFlow.test {
            awaitItem()
            awaitComplete()
        }

        val capturedParams = paramsSlot.captured
        assertEquals(LocationPriority.HIGH_ACCURACY, capturedParams.priority)
        assertEquals(3000L, capturedParams.locationInterval)
        assertEquals(1500L, capturedParams.fastestLocationInterval)
        assertEquals(8000L, capturedParams.maxUpdateDelayMillis)
        assertTrue(capturedParams.waitForAccurateLocation)
    }

    @Test
    fun `invoke handles empty flow gracefully`() = runTest {
        // Given
        val expectedFlow = flowOf<Result<Location>>()
        coEvery { mockLocationManager.getCurrentLocationUpdates(any()) } returns expectedFlow

        // When
        val resultFlow = getLocationUpdatesUseCase.invoke()

        // Then
        resultFlow.test {
            awaitComplete()
        }

        coVerify(exactly = 1) { mockLocationManager.getCurrentLocationUpdates(any()) }
    }
}
