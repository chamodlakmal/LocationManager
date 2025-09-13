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
 * Unit tests for [GetLastLocationUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class GetLastLocationUseCaseTest {

    @MockK
    private lateinit var mockLocationManager: LocationManager

    @MockK
    private lateinit var mockLocation: Location

    private lateinit var getLastLocationUseCase: GetLastLocationUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        getLastLocationUseCase = GetLastLocationUseCase(mockLocationManager)

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
        coEvery { mockLocationManager.getLastLocation() } returns expectedResult

        // When
        val result = getLastLocationUseCase.invoke()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        coVerify(exactly = 1) { mockLocationManager.getLastLocation() }
    }

    @Test
    fun `invoke returns failure when LocationManager returns failure`() = runTest {
        // Given
        val exception = Exception("Unable to retrieve the last location")
        val expectedResult = Result.failure<Location>(exception)
        coEvery { mockLocationManager.getLastLocation() } returns expectedResult

        // When
        val result = getLastLocationUseCase.invoke()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception.message, result.exceptionOrNull()?.message)
        coVerify(exactly = 1) { mockLocationManager.getLastLocation() }
    }

    @Test
    fun `invoke propagates PermissionDeniedException when LocationManager throws it`() = runTest {
        // Given
        val exception = LocationException.PermissionDeniedException("Permission Denied")
        coEvery { mockLocationManager.getLastLocation() } throws exception

        // When & Then
        try {
            getLastLocationUseCase.invoke()
            fail("Expected LocationException.PermissionDeniedException to be thrown")
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }

        coVerify(exactly = 1) { mockLocationManager.getLastLocation() }
    }

    @Test
    fun `invoke propagates generic exceptions when LocationManager throws them`() = runTest {
        // Given
        val exception = RuntimeException("Network error")
        coEvery { mockLocationManager.getLastLocation() } throws exception

        // When & Then
        try {
            getLastLocationUseCase.invoke()
            fail("Expected RuntimeException to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Network error", e.message)
        }

        coVerify(exactly = 1) { mockLocationManager.getLastLocation() }
    }

    @Test
    fun `invoke calls LocationManager exactly once`() = runTest {
        // Given
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getLastLocation() } returns expectedResult

        // When
        getLastLocationUseCase.invoke()

        // Then
        coVerify(exactly = 1) { mockLocationManager.getLastLocation() }
        confirmVerified(mockLocationManager)
    }

    @Test
    fun `multiple invokes call LocationManager multiple times`() = runTest {
        // Given
        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getLastLocation() } returns expectedResult

        // When
        repeat(3) {
            getLastLocationUseCase.invoke()
        }

        // Then
        coVerify(exactly = 3) { mockLocationManager.getLastLocation() }
    }

    @Test
    fun `invoke preserves location data integrity`() = runTest {
        // Given
        val specificLatitude = 37.7749
        val specificLongitude = -122.4194
        val specificAccuracy = 5.0f
        val specificTime = 1609459200000L // Fixed timestamp

        every { mockLocation.latitude } returns specificLatitude
        every { mockLocation.longitude } returns specificLongitude
        every { mockLocation.accuracy } returns specificAccuracy
        every { mockLocation.time } returns specificTime

        val expectedResult = Result.success(mockLocation)
        coEvery { mockLocationManager.getLastLocation() } returns expectedResult

        // When
        val result = getLastLocationUseCase.invoke()

        // Then
        assertTrue(result.isSuccess)
        val location = result.getOrNull()
        assertNotNull(location)
        assertEquals(specificLatitude, location!!.latitude, 0.0001)
        assertEquals(specificLongitude, location.longitude, 0.0001)
        assertEquals(specificAccuracy, location.accuracy)
        assertEquals(specificTime, location.time)
    }
}
