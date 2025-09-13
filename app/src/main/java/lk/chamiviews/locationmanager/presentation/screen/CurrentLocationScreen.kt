package lk.chamiviews.locationmanager.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.chamiviews.locationmanager.domain.model.LocationPriority
import lk.chamiviews.locationmanager.presentation.components.LocationInfoCard
import lk.chamiviews.locationmanager.presentation.components.ErrorCard
import lk.chamiviews.locationmanager.presentation.viewmodel.CurrentLocationViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import android.location.Location
import lk.chamiviews.locationmanager.presentation.model.LocationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentLocationScreen(
    uiState: LocationUiState,
    selectedPriority: LocationPriority,
    onUpdatePriority: (LocationPriority) -> Unit,
    onGetCurrentLocation: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Current Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Current Location",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Requests a fresh location fix with configurable accuracy and power consumption settings. " +
                            "This may take longer but provides the most up-to-date location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Priority Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Location Priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                LocationPriority.entries.forEach { priority ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedPriority == priority,
                                onClick = { onUpdatePriority(priority) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPriority == priority,
                            onClick = { onUpdatePriority(priority) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = getPriorityDisplayName(priority),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getPriorityDescription(priority),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Action Button
        Button(
            onClick = onGetCurrentLocation,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isPermissionGranted && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (uiState.isLoading) "Getting Current Location..." else "Get Current Location")
        }

        // Permission Status
        if (!uiState.isPermissionGranted) {
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

        // Error Display
        uiState.error?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = onClearError
            )
        }

        // Location Result
        uiState.location?.let { location ->
            LocationInfoCard(
                location = location,
                title = "Current Location (${getPriorityDisplayName(selectedPriority)})",
                timestamp = if (uiState.lastUpdateTime > 0) {
                    "Retrieved at: ${dateFormatter.format(Date(uiState.lastUpdateTime))}"
                } else null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurrentLocationScreenPreview() {
    MaterialTheme {
        CurrentLocationScreen(
            uiState = LocationUiState(
                isPermissionGranted = true,
                isLoading = false,
                location = Location("mock").apply {
                    latitude = 6.9271
                    longitude = 79.9612
                    accuracy = 5.2f
                },
                lastUpdateTime = System.currentTimeMillis()
            ),
            selectedPriority = LocationPriority.HIGH_ACCURACY,
            onUpdatePriority = { },
            onGetCurrentLocation = { },
            onClearError = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CurrentLocationScreenLoadingPreview() {
    MaterialTheme {
        CurrentLocationScreen(
            uiState = LocationUiState(
                isPermissionGranted = true,
                isLoading = true
            ),
            selectedPriority = LocationPriority.BALANCED_POWER_ACCURACY,
            onUpdatePriority = { },
            onGetCurrentLocation = { },
            onClearError = { }
        )
    }
}

private fun getPriorityDisplayName(priority: LocationPriority): String {
    return when (priority) {
        LocationPriority.HIGH_ACCURACY -> "High Accuracy"
        LocationPriority.BALANCED_POWER_ACCURACY -> "Balanced"
        LocationPriority.LOW_POWER -> "Low Power"
        LocationPriority.PASSIVE -> "Passive"
    }
}

private fun getPriorityDescription(priority: LocationPriority): String {
    return when (priority) {
        LocationPriority.HIGH_ACCURACY -> "Most accurate, highest power usage"
        LocationPriority.BALANCED_POWER_ACCURACY -> "Good balance of accuracy and power"
        LocationPriority.LOW_POWER -> "City-level accuracy, low power"
        LocationPriority.PASSIVE -> "No active requests, uses other apps' locations"
    }
}
