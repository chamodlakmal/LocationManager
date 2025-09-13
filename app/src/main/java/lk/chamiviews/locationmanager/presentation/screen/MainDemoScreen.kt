package lk.chamiviews.locationmanager.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.chamiviews.locationmanager.presentation.viewmodel.CurrentLocationViewModel
import lk.chamiviews.locationmanager.presentation.viewmodel.LastLocationViewModel
import lk.chamiviews.locationmanager.presentation.viewmodel.LocationUpdatesViewModel
import lk.chamiviews.locationmanager.presentation.viewmodel.ProximateLocationViewModel

enum class DemoScreen(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
) {
    LAST_LOCATION("Last Location", Icons.Outlined.History, "Get cached location quickly"),
    CURRENT_LOCATION(
        "Current Location",
        Icons.Outlined.GpsFixed,
        "Get fresh location with priority settings"
    ),
    LOCATION_UPDATES("Location Updates", Icons.Outlined.TrackChanges, "Continuous location tracking"),
    PROXIMATE_LOCATION(
        "Smart Location",
        Icons.Outlined.Explore,
        "Best available location intelligently"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDemoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<DemoScreen?>(null) }

    // Hilt ViewModels
    val lastLocationViewModel: LastLocationViewModel = hiltViewModel()
    val currentLocationViewModel: CurrentLocationViewModel = hiltViewModel()
    val locationUpdatesViewModel: LocationUpdatesViewModel = hiltViewModel()
    val proximateLocationViewModel: ProximateLocationViewModel = hiltViewModel()

    var isLocationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        isLocationPermissionGranted = granted

        // Update all ViewModels with permission status
        lastLocationViewModel.updatePermissionStatus(granted)
        currentLocationViewModel.updatePermissionStatus(granted)
        locationUpdatesViewModel.updatePermissionStatus(granted)
        proximateLocationViewModel.updatePermissionStatus(granted)

        if (!granted) {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    // Update permission status on composition
    LaunchedEffect(isLocationPermissionGranted) {
        lastLocationViewModel.updatePermissionStatus(isLocationPermissionGranted)
        currentLocationViewModel.updatePermissionStatus(isLocationPermissionGranted)
        locationUpdatesViewModel.updatePermissionStatus(isLocationPermissionGranted)
        proximateLocationViewModel.updatePermissionStatus(isLocationPermissionGranted)
    }

    Scaffold(
        topBar = {
            if (currentScreen != null) {
                TopAppBar(
                    title = { Text(currentScreen!!.title) },
                    navigationIcon = {
                        IconButton(onClick = { currentScreen = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (currentScreen == null) {
            // Main menu with proper padding
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = paddingValues.calculateBottomPadding(),
                        top = paddingValues.calculateTopPadding()
                    )
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Location Manager Demo",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Comprehensive location services demonstration with MVVM architecture and Hilt DI",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Permission Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLocationPermissionGranted)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocationPermissionGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isLocationPermissionGranted)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Location Permission",
                                fontWeight = FontWeight.Bold,
                                color = if (isLocationPermissionGranted)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                if (isLocationPermissionGranted) "✓ Granted - All features available" else "✗ Not granted - Grant permission to use location features",
                                color = if (isLocationPermissionGranted)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (!isLocationPermissionGranted) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }

                // Demo Screens
                Text(
                    "Location Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                DemoScreen.entries.forEach { screen ->
                    DemoScreenCard(
                        screen = screen,
                        isEnabled = isLocationPermissionGranted,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        } else {
            // Screen content with proper padding
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                when (currentScreen) {
                    DemoScreen.LAST_LOCATION -> {
                        val uiState by lastLocationViewModel.uiState.collectAsStateWithLifecycle()
                        LastLocationScreen(
                            uiState = uiState,
                            onGetLastLocation = lastLocationViewModel::getLastLocation,
                            onClearError = lastLocationViewModel::clearError
                        )
                    }

                    DemoScreen.CURRENT_LOCATION -> {
                        val uiState by currentLocationViewModel.uiState.collectAsStateWithLifecycle()
                        val selectedPriority by currentLocationViewModel.selectedPriority.collectAsStateWithLifecycle()
                        CurrentLocationScreen(
                            uiState = uiState,
                            selectedPriority = selectedPriority,
                            onUpdatePriority = currentLocationViewModel::updatePriority,
                            onGetCurrentLocation = currentLocationViewModel::getCurrentLocation,
                            onClearError = currentLocationViewModel::clearError
                        )
                    }

                    DemoScreen.LOCATION_UPDATES -> {
                        val uiState by locationUpdatesViewModel.uiState.collectAsStateWithLifecycle()
                        LocationUpdatesScreen(
                            uiState = uiState,
                            onStartTracking = { priority, interval ->
                                locationUpdatesViewModel.startLocationUpdates(
                                    priority = priority,
                                    intervalMs = interval,
                                    fastestIntervalMs = interval / 2
                                )
                            },
                            onStopTracking = locationUpdatesViewModel::stopLocationUpdates,
                            onClearLocations = locationUpdatesViewModel::clearLocations,
                            onClearError = locationUpdatesViewModel::clearError
                        )
                    }

                    DemoScreen.PROXIMATE_LOCATION -> {
                        val uiState by proximateLocationViewModel.uiState.collectAsStateWithLifecycle()
                        ProximateLocationScreen(
                            uiState = uiState,
                            onGetProximateLocation = proximateLocationViewModel::getProximateLocation,
                            onClearError = proximateLocationViewModel::clearError
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreenCard(
    screen: DemoScreen,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = if (isEnabled) onClick else {
            {}
        },
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    screen.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    screen.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            if (isEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Requires permission",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainDemoScreenPreview() {
    MaterialTheme {
        MainDemoScreenContent(
            hasLocationPermission = true,
            onRequestPermission = { },
            onNavigateToScreen = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainDemoScreenNoPermissionPreview() {
    MaterialTheme {
        MainDemoScreenContent(
            hasLocationPermission = false,
            onRequestPermission = { },
            onNavigateToScreen = { }
        )
    }
}

@Composable
fun MainDemoScreenContent(
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToScreen: (DemoScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Manager",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Location Manager Demo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Explore different location retrieval methods and understand their use cases, " +
                            "performance characteristics, and implementation patterns.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Permission Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = if (hasLocationPermission) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (hasLocationPermission) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (hasLocationPermission) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (hasLocationPermission) "Permission Granted" else "Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasLocationPermission) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        if (hasLocationPermission) {
                            "All location features are available"
                        } else {
                            "Location permission is needed to use these features"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasLocationPermission) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                if (!hasLocationPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onErrorContainer,
                            contentColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("Grant")
                    }
                }
            }
        }

        // Demo Options
        Text(
            "Choose a Location Method",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        DemoScreen.entries.forEach { screen ->
            DemoOptionCard(
                screen = screen,
                enabled = hasLocationPermission,
                onClick = { onNavigateToScreen(screen) }
            )
        }

        // Footer Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "About This Demo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This demonstration showcases various Android location APIs and their appropriate use cases. " +
                            "Each method has different characteristics in terms of speed, accuracy, and power consumption.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoOptionCard(
    screen: DemoScreen,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    screen.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Text(
                    screen.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = "Navigate",
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
    }
}
