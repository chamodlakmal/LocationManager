package lk.chamiviews.locationmanager.domain.usecase

import android.location.Location
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import lk.chamiviews.locationmanager.domain.exception.LocationException
import lk.chamiviews.locationmanager.domain.repository.LocationManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [GetProximateLocationUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class GetProximateLocationUseCaseTest {

    @MockK
    private lateinit var mockLocationManager: LocationManager

    @MockK
    private lateinit var mockLocation: Location

    private lateinit var getProximateLocationUseCase: GetProximateLocationUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        getProximateLocationUseCase = GetProximateLocationUseCase(mockLocationManager)

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
    fun `invoke returns success when LocationManager returns success`() = runTest {
        // Given
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getProximateLocation() } returns expectedResult

        // When
        val result = getProximateLocationUseCase.invoke()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        coVerify(exactly = 1) { mockLocationManager.getProximateLocation() }
    }

    @Test
    fun `invoke returns failure when LocationManager returns failure`() = runTest {
        // Given
        val exception = Exception("Unable to retrieve proximate location")
        val expectedResult = Result.failure<Location>(exception)
        coEvery { mockLocationManager.getProximateLocation() } returns expectedResult

        // When
        val result = getProximateLocationUseCase.invoke()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { mockLocationManager.getProximateLocation() }
    }

    @Test
    fun `invoke propagates PermissionDeniedException when LocationManager throws it`() = runTest {
        // Given
        val exception = LocationException.PermissionDeniedException("Permission Denied")
        coEvery { mockLocationManager.getProximateLocation() } throws exception

        // When & Then
        try {
            getProximateLocationUseCase.invoke()
            fail("Expected LocationException.PermissionDeniedException to be thrown")
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }

        coVerify(exactly = 1) { mockLocationManager.getProximateLocation() }
    }

    @Test
    fun `invoke propagates generic exceptions when LocationManager throws them`() = runTest {
        // Given
        val exception = RuntimeException("Location service unavailable")
        coEvery { mockLocationManager.getProximateLocation() } throws exception

        // When & Then
        try {
            getProximateLocationUseCase.invoke()
            fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Location service unavailable", e.message)
        }

        coVerify(exactly = 1) { mockLocationManager.getProximateLocation() }
    }

    @Test
    fun `invoke calls LocationManager exactly once`() = runTest {
        // Given
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getProximateLocation() } returns expectedResult

        // When
        getProximateLocationUseCase.invoke()

        // Then
        coVerify(exactly = 1) { mockLocationManager.getProximateLocation() }
        confirmVerified(mockLocationManager)
    }

    @Test
    fun `multiple invokes call LocationManager multiple times`() = runTest {
        // Given
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getProximateLocation() } returns expectedResult

        // When
        repeat(5) {
            getProximateLocationUseCase.invoke()
        }

        // Then
        coVerify(exactly = 5) { mockLocationManager.getProximateLocation() }
    }

    @Test
    fun `invoke preserves location data integrity`() = runTest {
        // Given
        val specificLatitude = 40.7128
        val specificLongitude = -74.0060
        val specificAccuracy = 5.2f
        val specificTime = 1609459200000L

        every { mockLocation.latitude } returns specificLatitude
        every { mockLocation.longitude } returns specificLongitude
        every { mockLocation.accuracy } returns specificAccuracy
        every { mockLocation.time } returns specificTime

        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getProximateLocation() } returns expectedResult

        // When
        val result = getProximateLocationUseCase.invoke()

        // Then
        assertTrue(result.isSuccess)
        val location = result.getOrNull()
        assertNotNull(location)
        assertEquals(specificLatitude, location!!.latitude, 0.0001)
        assertEquals(specificLongitude, location.longitude, 0.0001)
        assertEquals(specificAccuracy, location.accuracy)
        assertEquals(specificTime, location.time)
    }

    @Test
    fun `invoke handles null location in success result gracefully`() = runTest {
        // Given - This shouldn't happen in normal operation but let's test defensive handling
        val expectedResult = Result.success(null as Location?)
        coEvery { mockLocationManager.getProximateLocation() } returns expectedResult as Result<Location>

        // When
        val result = getProximateLocationUseCase.invoke()

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        coVerify(exactly = 1) { mockLocationManager.getProximateLocation() }
    }

    @Test
    fun `invoke demonstrates smart location behavior concept`() = runTest {
        // Given - This test verifies the use case correctly delegates the smart behavior to LocationManager
        // The LocationManager should handle the logic of trying last location first, then current location
        val smartLocationResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getProximateLocation() } returns smartLocationResult

        // When
        val result = getProximateLocationUseCase.invoke()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())

        // Verify that the use case delegates to the LocationManager's smart location method
        coVerify(exactly = 1) { mockLocationManager.getProximateLocation() }

        // Verify it doesn't directly call other location methods (delegation pattern)
        coVerify(exactly = 0) { mockLocationManager.getLastLocation() }
        coVerify(exactly = 0) { mockLocationManager.getCurrentLocation(any()) }
    }
}
