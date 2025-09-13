package lk.chamiviews.locationmanager.presentation.screen

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.presentation.components.ErrorCard
import lk.chamiviews.locationmanager.presentation.components.LocationInfoCard
import lk.chamiviews.locationmanager.presentation.model.LocationInfo
import lk.chamiviews.locationmanager.presentation.model.LocationUpdatesUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationUpdatesScreen(
    uiState: LocationUpdatesUiState,
    onStartTracking: (LocationPriority, Long) -> Unit,
    onStopTracking: () -> Unit,
    onClearLocations: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var selectedInterval by remember { mutableStateOf(5000L) }
    var selectedPriority by remember { mutableStateOf(LocationPriority.BALANCED_POWER_ACCURACY) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrackChanges,
                            contentDescription = "Location Updates",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Continuous Location Updates",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Provides continuous location updates at specified intervals. " +
                                "Useful for tracking movement and navigation applications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Settings Card
        if (!uiState.isTracking) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Update Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Priority Selection
                        Text(
                            "Priority:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LocationPriority.entries.forEach { priority ->
                                FilterChip(
                                    onClick = { selectedPriority = priority },
                                    label = { Text(getPriorityShortName(priority)) },
                                    selected = selectedPriority == priority,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Interval Selection
                        Text(
                            "Update Interval:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                1000L to "1s",
                                2000L to "2s",
                                5000L to "5s",
                                10000L to "10s"
                            ).forEach { (interval, label) ->
                                FilterChip(
                                    onClick = { selectedInterval = interval },
                                    label = { Text(label) },
                                    selected = selectedInterval == interval,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Control Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (uiState.isTracking) {
                            onStopTracking()
                        } else {
                            onStartTracking(selectedPriority, selectedInterval)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isPermissionGranted
                ) {
                    Icon(
                        imageVector = if (uiState.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isTracking) "Stop Updates" else "Start Updates")
                }

                if (uiState.locations.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearLocations,
                        enabled = !uiState.isTracking
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear")
                    }
                }
            }
        }

        // Permission Status
        if (!uiState.isPermissionGranted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "Location permission not granted. Please grant permission to use this feature.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Tracking Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status: ${if (uiState.isTracking) "🟢 Tracking Active" else "🔴 Tracking Stopped"}")
                    Text("Total Updates: ${uiState.totalUpdates}")
                    Text("Stored Locations: ${uiState.locations.size}")
                    if (uiState.isTracking) {
                        Text("Interval: ${selectedInterval / 1000}s")
                        Text("Priority: ${getPriorityShortName(selectedPriority)}")
                    }
                }
            }
        }

        // Error Display
        uiState.error?.let { error ->
            item {
                ErrorCard(
                    error = error,
                    onDismiss = onClearError
                )
            }
        }

        // Current Location (if tracking)
        uiState.currentLocation?.let { location ->
            item {
                LocationInfoCard(
                    location = location,
                    title = "Latest Location Update",
                    timestamp = "Update #${uiState.totalUpdates}"
                )
            }
        }

        // Location History Header
        if (uiState.locations.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        "Location History (Last ${minOf(uiState.locations.size, 20)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Location History Items
        if (uiState.locations.isNotEmpty()) {
            items(uiState.locations) { locationInfo ->
                LocationHistoryItem(
                    locationInfo = locationInfo,
                    dateFormatter = dateFormatter
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationHistoryItem(
    locationInfo: LocationInfo,
    dateFormatter: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${
                        String.format(
                            Locale.US,
                            "%.6f",
                            locationInfo.location.latitude
                        )
                    }, ${String.format(Locale.US, "%.6f", locationInfo.location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "±${String.format(Locale.US, "%.1f", locationInfo.accuracy)}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                dateFormatter.format(Date(locationInfo.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationUpdatesScreenPreview() {
    MaterialTheme {
        LocationUpdatesScreen(
            uiState = LocationUpdatesUiState(
                isPermissionGranted = true,
                isTracking = false,
                totalUpdates = 5,
                locations = listOf(
                    LocationInfo(
                        location = Location("mock").apply {
                            latitude = 6.9271
                            longitude = 79.9612
                            accuracy = 5.2f
                        },
                        timestamp = System.currentTimeMillis()
                    ),
                    LocationInfo(
                        location = Location("mock").apply {
                            latitude = 6.9275
                            longitude = 79.9615
                            accuracy = 3.8f
                        },
                        timestamp = System.currentTimeMillis() - 5000
                    )
                )
            ),
            onStartTracking = { _, _ -> },
            onStopTracking = { },
            onClearLocations = { },
            onClearError = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LocationUpdatesScreenTrackingPreview() {
    MaterialTheme {
        LocationUpdatesScreen(
            uiState = LocationUpdatesUiState(
                isPermissionGranted = true,
                isTracking = true,
                totalUpdates = 12,
                currentLocation = Location("mock").apply {
                    latitude = 6.9271
                    longitude = 79.9612
                    accuracy = 4.1f
                },
                locations = listOf()
            ),
            onStartTracking = { _, _ -> },
            onStopTracking = { },
            onClearLocations = { },
            onClearError = { }
        )
    }
}

private fun getPriorityShortName(priority: LocationPriority): String {
    return when (priority) {
        LocationPriority.HIGH_ACCURACY -> "High"
        LocationPriority.BALANCED_POWER_ACCURACY -> "Balanced"
        LocationPriority.LOW_POWER -> "Low Power"
        LocationPriority.PASSIVE -> "Passive"
    }
}
