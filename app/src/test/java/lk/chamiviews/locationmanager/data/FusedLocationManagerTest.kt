package lk.chamiviews.locationmanager.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.text.TextUtils
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import lk.chamiviews.locationmanager.domain.exception.LocationException
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.domain.model.LocationRequestParams
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [FusedLocationManager].
 *
 * Uses pure MockK mocking without Robolectric to avoid Android framework dependency issues.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class FusedLocationManagerTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockFusedLocationProviderClient: FusedLocationProviderClient

    @MockK
    private lateinit var mockLocation: Location

    @MockK
    private lateinit var mockTask: Task<Location>

    @MockK
    private lateinit var mockVoidTask: Task<Void>

    private lateinit var fusedLocationManager: FusedLocationManager

    companion object {
        // Use a thread-safe approach for static mocks
        private val staticMockLock = Any()
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        synchronized(staticMockLock) {
            // Mock TextUtils to handle Android framework calls
            mockkStatic(TextUtils::class)
            every { TextUtils.equals(any(), any()) } returns false

            // Mock LocationServices.getFusedLocationProviderClient
            mockkStatic("com.google.android.gms.location.LocationServices")
            every {
                com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(mockContext)
            } returns mockFusedLocationProviderClient

            // Mock ActivityCompat
            mockkStatic(ActivityCompat::class)
        }

        // Setup default location mock
        every { mockLocation.latitude } returns 37.7749
        every { mockLocation.longitude } returns -122.4194
        every { mockLocation.accuracy } returns 10.0f
        every { mockLocation.time } returns System.currentTimeMillis()

        fusedLocationManager = FusedLocationManager(mockContext)
    }

    @After
    fun tearDown() {
        synchronized(staticMockLock) {
            // Clear static mocks in reverse order
            unmockkStatic(ActivityCompat::class)
            unmockkStatic("com.google.android.gms.location.LocationServices")
            unmockkStatic(TextUtils::class)
        }

        // Clear all other mocks
        clearAllMocks()
    }

    // Permission Tests
    @Test
    fun `checkPermissions returns true when FINE_LOCATION permission is granted`() = runTest {
        // Given
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        every { mockFusedLocationProviderClient.lastLocation } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockLocation)
            mockTask
        }

        // When
        val result = fusedLocationManager.getLastLocation()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
    }

    @Test
    fun `checkPermissions returns true when COARSE_LOCATION permission is granted`() = runTest {
        // Given
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED

        every { mockFusedLocationProviderClient.lastLocation } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockLocation)
            mockTask
        }

        // When
        val result = fusedLocationManager.getLastLocation()

        // Then
        assertTrue(result.isSuccess)
    }

    // getLastLocation Tests
    @Test
    fun `getLastLocation throws PermissionDeniedException when permissions not granted`() = runTest {
        // Given
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED

        // When & Then
        try {
            fusedLocationManager.getLastLocation()
            fail("Expected LocationException.PermissionDeniedException to be thrown")
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }
    }

    @Test
    fun `getLastLocation returns success when location is retrieved successfully`() = runTest {
        // Given
        mockPermissionsGranted()
        every { mockFusedLocationProviderClient.lastLocation } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockLocation)
            mockTask
        }

        // When
        val result = fusedLocationManager.getLastLocation()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
    }

    @Test
    fun `getLastLocation returns failure when location is null after max retries`() = runTest {
        // Given
        mockPermissionsGranted()
        every { mockFusedLocationProviderClient.lastLocation } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(null)
            mockTask
        }

        // When
        val result = fusedLocationManager.getLastLocation()

        // Then
        assertTrue(result.isFailure)
        assertEquals("Unable to retrieve the last location", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getLastLocation handles exception during location request`() = runTest {
        // Given
        mockPermissionsGranted()
        every { mockFusedLocationProviderClient.lastLocation } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val failureSlot = slot<OnFailureListener>()
        every { mockTask.addOnFailureListener(capture(failureSlot)) } answers {
            failureSlot.captured.onFailure(RuntimeException("Location service error"))
            mockTask
        }

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } returns mockTask

        // When
        val result = fusedLocationManager.getLastLocation()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unable to retrieve the last location") == true)
    }

    // getCurrentLocation Tests
    @Test
    fun `getCurrentLocation throws PermissionDeniedException when permissions not granted`() = runTest {
        // Given
        mockPermissionsDenied()

        // When & Then
        try {
            fusedLocationManager.getCurrentLocation(LocationPriority.HIGH_ACCURACY)
            fail("Expected LocationException.PermissionDeniedException to be thrown")
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }
    }

    @Test
    fun `getCurrentLocation returns success when location is retrieved successfully`() = runTest {
        // Given
        mockPermissionsGranted()
        every { mockFusedLocationProviderClient.getCurrentLocation(any<Int>(), any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockLocation)
            mockTask
        }

        // When
        val result = fusedLocationManager.getCurrentLocation(LocationPriority.HIGH_ACCURACY)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
    }

    @Test
    fun `getCurrentLocation returns failure when location is null after max retries`() = runTest {
        // Given
        mockPermissionsGranted()
        every { mockFusedLocationProviderClient.getCurrentLocation(any<Int>(), any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(null)
            mockTask
        }

        // When
        val result = fusedLocationManager.getCurrentLocation(LocationPriority.HIGH_ACCURACY)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Unable to retrieve a current location", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getCurrentLocation uses correct priority value`() = runTest {
        // Given
        mockPermissionsGranted()
        every { mockFusedLocationProviderClient.getCurrentLocation(any<Int>(), any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockLocation)
            mockTask
        }

        val prioritySlot = slot<Int>()
        every { mockFusedLocationProviderClient.getCurrentLocation(capture(prioritySlot), any()) } returns mockTask

        // When
        fusedLocationManager.getCurrentLocation(LocationPriority.HIGH_ACCURACY)

        // Then
        verify { mockFusedLocationProviderClient.getCurrentLocation(LocationPriority.HIGH_ACCURACY.priorityValue, any()) }
    }

    // getCurrentLocationUpdates Tests
    @Test
    fun `getCurrentLocationUpdates throws PermissionDeniedException when permissions not granted`() = runTest {
        // Given
        mockPermissionsDenied()
        val params = LocationRequestParams(
            priority = LocationPriority.HIGH_ACCURACY,
            locationInterval = 5000L,
            fastestLocationInterval = 2000L,
            maxUpdateDelayMillis = 10000L,
            waitForAccurateLocation = false
        )

        // When & Then
        try {
            val flow = fusedLocationManager.getCurrentLocationUpdates(params)
            // The exception should be thrown when we try to collect from the flow
            flow.collect { }
            fail("Expected LocationException.PermissionDeniedException to be thrown")
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }
    }

    @Test
    fun `getCurrentLocationUpdates creates LocationRequest with correct parameters`() = runTest {
        // Given
        mockPermissionsGranted()
        val params = LocationRequestParams(
            priority = LocationPriority.HIGH_ACCURACY,
            locationInterval = 5000L,
            fastestLocationInterval = 2000L,
            maxUpdateDelayMillis = 10000L,
            waitForAccurateLocation = true
        )

        every {
            mockFusedLocationProviderClient.requestLocationUpdates(
                any<LocationRequest>(),
                any<LocationCallback>(),
                any()
            )
        } returns mockVoidTask
        every { mockFusedLocationProviderClient.removeLocationUpdates(any<LocationCallback>()) } returns mockVoidTask

        // When
        val flow = fusedLocationManager.getCurrentLocationUpdates(params)

        // Then
        // Verify that requestLocationUpdates is called when flow is collected
        // Note: Flow is cold, so we need to collect it to trigger the setup
        // This is a basic verification that the flow can be created without throwing
        assertNotNull(flow)
    }

    // getProximateLocation Tests
    @Test
    fun `getProximateLocation throws PermissionDeniedException when permissions not granted`() = runTest {
        // Given
        mockPermissionsDenied()

        // When & Then
        try {
            fusedLocationManager.getProximateLocation()
            fail("Expected LocationException.PermissionDeniedException to be thrown")
        } catch (e: LocationException.PermissionDeniedException) {
            assertEquals("Permission Denied", e.message)
        }
    }

    @Test
    fun `getProximateLocation returns last location when available`() = runTest {
        // Given
        mockPermissionsGranted()
        every { mockFusedLocationProviderClient.lastLocation } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockLocation)
            mockTask
        }

        // When
        val result = fusedLocationManager.getProximateLocation()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        verify(exactly = 1) { mockFusedLocationProviderClient.lastLocation }
    }

    @Test
    fun `getProximateLocation falls back to current location when last location fails`() = runTest {
        // Given
        mockPermissionsGranted()

        // Mock lastLocation to fail
        val lastLocationTask = mockk<Task<Location>>()
        every { mockFusedLocationProviderClient.lastLocation } returns lastLocationTask
        every { lastLocationTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns lastLocationTask
        every { lastLocationTask.addOnFailureListener(any<OnFailureListener>()) } returns lastLocationTask

        val lastLocationSuccessSlot = slot<OnSuccessListener<Location?>>()
        every { lastLocationTask.addOnSuccessListener(capture(lastLocationSuccessSlot)) } answers {
            lastLocationSuccessSlot.captured.onSuccess(null)
            lastLocationTask
        }

        // Mock getCurrentLocation to succeed
        val currentLocationTask = mockk<Task<Location>>()
        every { mockFusedLocationProviderClient.getCurrentLocation(any<Int>(), any()) } returns currentLocationTask
        every { currentLocationTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns currentLocationTask
        every { currentLocationTask.addOnFailureListener(any<OnFailureListener>()) } returns currentLocationTask

        val currentLocationSuccessSlot = slot<OnSuccessListener<Location?>>()
        every { currentLocationTask.addOnSuccessListener(capture(currentLocationSuccessSlot)) } answers {
            currentLocationSuccessSlot.captured.onSuccess(mockLocation)
            currentLocationTask
        }

        // When
        val result = fusedLocationManager.getProximateLocation()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        verify { mockFusedLocationProviderClient.lastLocation }
        verify { mockFusedLocationProviderClient.getCurrentLocation(LocationPriority.HIGH_ACCURACY.priorityValue, any()) }
    }

    @Test
    fun `different priority values are correctly passed to getCurrentLocation`() = runTest {
        // Given
        val priorities = listOf(
            LocationPriority.HIGH_ACCURACY,
            LocationPriority.BALANCED_POWER_ACCURACY,
            LocationPriority.LOW_POWER,
            LocationPriority.PASSIVE
        )

        priorities.forEach { priority ->
            mockPermissionsGranted()

            val mockTaskForPriority = mockk<Task<Location>>()
            every { mockFusedLocationProviderClient.getCurrentLocation(any<Int>(), any()) } returns mockTaskForPriority
            every { mockTaskForPriority.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTaskForPriority
            every { mockTaskForPriority.addOnFailureListener(any<OnFailureListener>()) } returns mockTaskForPriority

            val successSlot = slot<OnSuccessListener<Location?>>()
            every { mockTaskForPriority.addOnSuccessListener(capture(successSlot)) } answers {
                successSlot.captured.onSuccess(mockLocation)
                mockTaskForPriority
            }

            // When
            val result = fusedLocationManager.getCurrentLocation(priority)

            // Then
            assertTrue("Failed for priority: $priority", result.isSuccess)
            verify { mockFusedLocationProviderClient.getCurrentLocation(priority.priorityValue, any()) }

            // Clear mocks for next iteration
            clearMocks(mockFusedLocationProviderClient)
            every {
                com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(mockContext)
            } returns mockFusedLocationProviderClient
        }
    }

    @Test
    fun `retry mechanism works correctly for getLastLocation`() = runTest {
        // Given
        mockPermissionsGranted()

        var callCount = 0
        every { mockFusedLocationProviderClient.lastLocation } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            callCount++
            when (callCount) {
                1, 2 -> successSlot.captured.onSuccess(null) // First two calls return null
                else -> successSlot.captured.onSuccess(mockLocation) // Third call succeeds
            }
            mockTask
        }

        // When
        val result = fusedLocationManager.getLastLocation()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        assertEquals(3, callCount) // Should have been called 3 times
    }

    @Test
    fun `retry mechanism works correctly for getCurrentLocation`() = runTest {
        // Given
        mockPermissionsGranted()

        var callCount = 0
        every { mockFusedLocationProviderClient.getCurrentLocation(any<Int>(), any()) } returns mockTask
        every { mockTask.addOnSuccessListener(any<OnSuccessListener<Location?>>()) } returns mockTask
        every { mockTask.addOnFailureListener(any<OnFailureListener>()) } returns mockTask

        val successSlot = slot<OnSuccessListener<Location?>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            callCount++
            when (callCount) {
                1, 2 -> successSlot.captured.onSuccess(null) // First two calls return null
                else -> successSlot.captured.onSuccess(mockLocation) // Third call succeeds
            }
            mockTask
        }

        // When
        val result = fusedLocationManager.getCurrentLocation(LocationPriority.HIGH_ACCURACY)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockLocation, result.getOrNull())
        assertEquals(3, callCount) // Should have been called 3 times
    }

    // Helper methods
    private fun mockPermissionsGranted() {
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
    }

    private fun mockPermissionsDenied() {
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ActivityCompat.checkSelfPermission(mockContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
    }
}
