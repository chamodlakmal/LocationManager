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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lk.chamiviews.locationmanager.presentation.components.ErrorCard
import lk.chamiviews.locationmanager.presentation.components.LocationInfoCard
import lk.chamiviews.locationmanager.presentation.model.LocationUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastLocationScreen(
    uiState: LocationUiState,
    onGetLastLocation: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    LastLocationScreenContent(
        uiState = uiState,
        onGetLastLocation = onGetLastLocation,
        modifier = modifier,
        onClearError = onClearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastLocationScreenContent(
    uiState: LocationUiState,
    onGetLastLocation: () -> Unit,
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
                        contentDescription = "Last Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Last Known Location",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Quickly retrieves the last cached location without requesting a new fix. " +
                            "This is the fastest method but may return outdated location data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Speed Indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Column {
                    Text(
                        "Fastest Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Returns immediately with cached location data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Action Button
        Button(
            onClick = onGetLastLocation,
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
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (uiState.isLoading) "Getting Last Location..." else "Get Last Location")
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
                title = "Last Known Location",
                timestamp = if (uiState.lastUpdateTime > 0) {
                    "Retrieved at: ${dateFormatter.format(Date(uiState.lastUpdateTime))}"
                } else null
            )
        }

        // Important Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Column {
                    Text(
                        "Important Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "This location may be outdated or null if no previous location has been cached. " +
                                "The accuracy and freshness depend on when the location was last obtained.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Advantages
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
                    Text("• Extremely fast response time")
                    Text("• No battery consumption")
                    Text("• No network or GPS usage")
                    Text("• Works offline")
                }
            }
        }

        // Use Cases
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Best Use Cases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• App initialization with rough location")
                    Text("• Default location for forms")
                    Text("• Quick location-based preferences")
                    Text("• Fallback when other methods fail")
                    Text("• Apps that can work with approximate location")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LastLocationScreenPreview() {
    MaterialTheme {
        LastLocationScreenContent(
            uiState = LocationUiState(
                isPermissionGranted = true,
                isLoading = false,
                location = Location("gps").apply {
                    latitude = 6.9271
                    longitude = 79.9612
                    accuracy = 10.0f
                    time = System.currentTimeMillis() - 300000 // 5 minutes ago
                },
                lastUpdateTime = System.currentTimeMillis()
            ),
            onGetLastLocation = { },
            onClearError = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LastLocationScreenNoLocationPreview() {
    MaterialTheme {
        LastLocationScreenContent(
            uiState = LocationUiState(
                isPermissionGranted = true,
                isLoading = false,
                location = null
            ),
            onGetLastLocation = { },
            onClearError = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LastLocationScreenLoadingPreview() {
    MaterialTheme {
        LastLocationScreenContent(
            uiState = LocationUiState(
                isPermissionGranted = true,
                isLoading = true
            ),
            onGetLastLocation = { },
            onClearError = { }
        )
    }
}
