package lk.chamiviews.locationmanager.domain.usecase

import android.location.Location
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import lk.chamiviews.locationmanager.domain.exception.LocationException
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [GetCurrentLocationUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class GetCurrentLocationUseCaseTest {

    @MockK
    private lateinit var mockLocationManager: LocationManager

    @MockK
    private lateinit var mockLocation: Location

    private lateinit var getCurrentLocationUseCase: GetCurrentLocationUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        getCurrentLocationUseCase = GetCurrentLocationUseCase(mockLocationManager)

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
    fun `invoke with default priority returns success when LocationManager returns success`() = runTest {
        // Given
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getCurrentLocation(LocationPriority.BALANCED_POWER_ACCURACY) } returns expectedResult

        // When
        val result = getCurrentLocationUseCase.invoke()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        coVerify(exactly = 1) { mockLocationManager.getCurrentLocation(LocationPriority.BALANCED_POWER_ACCURACY) }
    }

    @Test
    fun `invoke with specific priority returns success when LocationManager returns success`() = runTest {
        // Given
        val priority = LocationPriority.HIGH_ACCURACY
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getCurrentLocation(priority) } returns expectedResult

        // When
        val result = getCurrentLocationUseCase.invoke(priority)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        coVerify(exactly = 1) { mockLocationManager.getCurrentLocation(priority) }
    }

    @Test
    fun `invoke returns failure when LocationManager returns failure`() = runTest {
        // Given
        val priority = LocationPriority.LOW_POWER
        val exception = Exception("Unable to retrieve current location")
        val expectedResult = Result.failure<Location>(exception)
        coEvery { mockLocationManager.getCurrentLocation(priority) } returns expectedResult

        // When
        val result = getCurrentLocationUseCase.invoke(priority)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { mockLocationManager.getCurrentLocation(priority) }
    }

    @Test
    fun `invoke propagates PermissionDeniedException when LocationManager throws it`() = runTest {
        // Given
        val priority = LocationPriority.PASSIVE
        val exception = LocationException.PermissionDeniedException("Permission Denied")
        coEvery { mockLocationManager.getCurrentLocation(priority) } throws exception

        // When & Then
        try {
            getCurrentLocationUseCase.invoke(priority)
            fail("Expected LocationException.PermissionDeniedException to be thrown")
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }

        coVerify(exactly = 1) { mockLocationManager.getCurrentLocation(priority) }
    }

    @Test
    fun `invoke with all priority types calls LocationManager with correct priority`() = runTest {
        // Given
        val priorities = listOf(
            LocationPriority.HIGH_ACCURACY,
            LocationPriority.BALANCED_POWER_ACCURACY,
            LocationPriority.LOW_POWER,
            LocationPriority.PASSIVE
        )
        val expectedResult = Result.success(mockLocation)

        priorities.forEach { priority ->
            coEvery { mockLocationManager.getCurrentLocation(priority) } returns expectedResult

            // When
            val result = getCurrentLocationUseCase.invoke(priority)

            // Then
            assertTrue("Failed for priority: $priority", result.isSuccess)
            coVerify(exactly = 1) { mockLocationManager.getCurrentLocation(priority) }

            // Clear mocks for next iteration
            clearMocks(mockLocationManager, answers = false)
        }
    }

    @Test
    fun `invoke uses BALANCED_POWER_ACCURACY as default priority`() = runTest {
        // Given
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getCurrentLocation(LocationPriority.BALANCED_POWER_ACCURACY) } returns expectedResult

        // When
        val result = getCurrentLocationUseCase.invoke() // No priority parameter

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockLocationManager.getCurrentLocation(LocationPriority.BALANCED_POWER_ACCURACY) }
        coVerify(exactly = 0) { mockLocationManager.getCurrentLocation(LocationPriority.HIGH_ACCURACY) }
        coVerify(exactly = 0) { mockLocationManager.getCurrentLocation(LocationPriority.LOW_POWER) }
        coVerify(exactly = 0) { mockLocationManager.getCurrentLocation(LocationPriority.PASSIVE) }
    }

    @Test
    fun `invoke propagates generic exceptions when LocationManager throws them`() = runTest {
        // Given
        val priority = LocationPriority.HIGH_ACCURACY
        val exception = RuntimeException("GPS unavailable")
        coEvery { mockLocationManager.getCurrentLocation(priority) } throws exception

        // When & Then
        try {
            getCurrentLocationUseCase.invoke(priority)
            fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("GPS unavailable", e.message)
        }

        coVerify(exactly = 1) { mockLocationManager.getCurrentLocation(priority) }
    }

    @Test
    fun `invoke preserves location data integrity for different priorities`() = runTest {
        // Given
        val highAccuracyLocation = mockk<Location>()
        every { highAccuracyLocation.latitude } returns 40.7128
        every { highAccuracyLocation.longitude } returns -74.0060
        every { highAccuracyLocation.accuracy } returns 3.0f

        val lowPowerLocation = mockk<Location>()
        every { lowPowerLocation.latitude } returns 34.0522
        every { lowPowerLocation.longitude } returns -118.2437
        every { lowPowerLocation.accuracy } returns 50.0f

        coEvery { mockLocationManager.getCurrentLocation(LocationPriority.HIGH_ACCURACY) } returns Result.success(highAccuracyLocation)
        coEvery { mockLocationManager.getCurrentLocation(LocationPriority.LOW_POWER) } returns Result.success(lowPowerLocation)

        // When
        val highAccuracyResult = getCurrentLocationUseCase.invoke(LocationPriority.HIGH_ACCURACY)
        val lowPowerResult = getCurrentLocationUseCase.invoke(LocationPriority.LOW_POWER)

        // Then
        assertTrue(highAccuracyResult.isSuccess)
        assertTrue(lowPowerResult.isSuccess)

        val highAccLocation = highAccuracyResult.getOrNull()!!
        val lowPowLocation = lowPowerResult.getOrNull()!!

        assertEquals(40.7128, highAccLocation.latitude, 0.0001)
        assertEquals(34.0522, lowPowLocation.latitude, 0.0001)
        assertEquals(3.0f, highAccLocation.accuracy)
        assertEquals(50.0f, lowPowLocation.accuracy)
    }
}
