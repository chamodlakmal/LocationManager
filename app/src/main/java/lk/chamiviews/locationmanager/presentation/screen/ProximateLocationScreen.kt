package lk.chamiviews.locationmanager.presentation.screen

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.chamiviews.locationmanager.presentation.components.ErrorCard
import lk.chamiviews.locationmanager.presentation.components.LocationInfoCard
import lk.chamiviews.locationmanager.presentation.model.LocationUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximateLocationScreen(
    uiState: LocationUiState,
    onGetProximateLocation: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProximateLocationScreenContent(
        uiState = uiState,
        onGetProximateLocation = onGetProximateLocation,
        onClearError = onClearError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximateLocationScreenContent(
    uiState: LocationUiState,
    onGetProximateLocation: () -> Unit,
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
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = "Proximate Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Smart Location Retrieval",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Intelligently retrieves the best available location by first checking for " +
                            "cached location, then requesting a fresh high-accuracy fix if needed. " +
                            "This provides optimal balance between speed and accuracy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // How it works explanation
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
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "How It Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "1.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "First attempts to retrieve the last known location (fast)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "2.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "If no cached location is available, requests fresh high-accuracy location",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "3.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Returns the most accurate and up-to-date location available",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Action Button
        Button(
            onClick = onGetProximateLocation,
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
                    imageVector = Icons.Outlined.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (uiState.isLoading) "Finding Best Location..." else "Get Best Location")
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
                title = "Best Available Location",
                timestamp = if (uiState.lastUpdateTime > 0) {
                    "Retrieved at: ${dateFormatter.format(Date(uiState.lastUpdateTime))}"
                } else null
            )
        }

        // Advantages Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Advantages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• Optimal balance between speed and accuracy")
                    Text("• Reduces battery consumption when possible")
                    Text("• Falls back to fresh location when needed")
                    Text("• Best choice for most location-aware apps")
                }
            }
        }

        // Use Cases Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Recommended Use Cases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• Weather apps showing local conditions")
                    Text("• Store locators and nearby search")
                    Text("• Social media check-ins")
                    Text("• General location-based services")
                    Text("• Apps that need 'good enough' location quickly")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProximateLocationScreenPreview() {
    MaterialTheme {
        ProximateLocationScreenContent(
            uiState = LocationUiState(
                isPermissionGranted = true,
                isLoading = false,
                location = Location("fused").apply {
                    latitude = 6.9271
                    longitude = 79.9612
                    accuracy = 3.5f
                },
                lastUpdateTime = System.currentTimeMillis()
            ),
            onGetProximateLocation = { },
            onClearError = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProximateLocationScreenLoadingPreview() {
    MaterialTheme {
        ProximateLocationScreenContent(
            uiState = LocationUiState(
                isPermissionGranted = true,
                isLoading = true
            ),
            onGetProximateLocation = { },
            onClearError = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProximateLocationScreenNoPermissionPreview() {
    MaterialTheme {
        ProximateLocationScreenContent(
            uiState = LocationUiState(
                isPermissionGranted = false,
                isLoading = false
            ),
            onGetProximateLocation = { },
            onClearError = { }
        )
    }
}
