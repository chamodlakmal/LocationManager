# LocationManager - Modern Android Location Services

A comprehensive, production-ready Android location management library built with modern Android development practices including Clean Architecture, Jetpack Compose, and Hilt dependency injection.

## 🏗️ Architecture

This project demonstrates modern Android development best practices:

- **Clean Architecture** with clear separation of concerns
- **MVVM Pattern** with ViewModels and UI States
- **Dependency Injection** using Hilt
- **Reactive Programming** with Kotlin Coroutines and Flows
- **Jetpack Compose** for modern UI development
- **Google Play Services Location API** for accurate location services

## 📱 Features

- ✅ Real-time location tracking with efficient battery usage
- ✅ Automatic permission handling
- ✅ Location accuracy and provider management  
- ✅ Background location updates
- ✅ Error handling and retry mechanisms
- ✅ Clean Architecture implementation
- ✅ Modern UI with Jetpack Compose
- ✅ Comprehensive unit and integration tests

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 (Android 7.0) or higher
- Kotlin 2.0.21 or later

### Installation

1. Clone the repository:
```bash
git clone https://github.com/chamodlakmal/LocationManager.git
cd LocationManager
```

2. Open the project in Android Studio

3. Build and run the project:
```bash
./gradlew build
```

## 📦 Dependencies

### Core Dependencies
- **Android Gradle Plugin**: 8.13.0
- **Kotlin**: 2.0.21
- **Compose BOM**: 2025.09.00
- **Play Services Location**: 21.3.0
- **Hilt**: 2.51.1

### Key Libraries
```kotlin
// Location Services
implementation("com.google.android.gms:play-services-location:21.3.0")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.51.1")
kapt("com.google.dagger:hilt-android-compiler:2.51.1")

// Compose UI
implementation(platform("androidx.compose:compose-bom:2025.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// Architecture Components
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
implementation("androidx.activity:activity-compose:1.10.1")
```

## 🏛️ Project Structure

```
app/
├── src/main/java/lk/chamiviews/locationmanager/
│   ├── LocationManagerApplication.kt     # Application class with Hilt
│   ├── MainActivity.kt                   # Main activity with Compose
│   ├── data/                            # Data layer (repositories, data sources)
│   ├── domain/                          # Domain layer (use cases, models)
│   │   ├── exception/                   # Custom exceptions
│   │   ├── model/                       # Domain models
│   │   ├── repository/                  # Repository interfaces
│   │   └── usecase/                     # Business logic use cases
│   ├── presentation/                    # Presentation layer (ViewModels, UI)
│   │   └── screen/                      # Compose screens
│   ├── di/                             # Dependency injection modules
│   └── ui/                             # UI theme and components
```

## 🔧 Usage

### Basic Location Tracking

```kotlin
@AndroidEntryPoint
class LocationActivity : ComponentActivity() {
    private val locationViewModel: LocationViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationManagerTheme {
                LocationScreen(viewModel = locationViewModel)
            }
        }
    }
}
```

### Observing Location Updates

```kotlin
@Composable
fun LocationScreen(viewModel: LocationViewModel) {
    val locationState by viewModel.locationState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startLocationUpdates()
    }
    
    when (locationState) {
        is LocationState.Loading -> CircularProgressIndicator()
        is LocationState.Success -> {
            Text("Lat: ${locationState.location.latitude}")
            Text("Lng: ${locationState.location.longitude}")
        }
        is LocationState.Error -> {
            Text("Error: ${locationState.message}")
        }
    }
}
```

## 🛠️ Configuration

### Permissions

Add these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

### Location Settings

Configure location request parameters:

```kotlin
val locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    10000L // 10 seconds interval
).apply {
    setMinUpdateDistanceMeters(10f)
    setMaxUpdateDelayMillis(30000L)
}.build()
```

## 🧪 Testing

Run the test suite:

```bash
# Unit tests
./gradlew test

# Instrumentation tests  
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

## 📊 Performance Considerations

- **Battery Optimization**: Uses Fused Location Provider for intelligent power management
- **Memory Efficiency**: Proper lifecycle management and resource cleanup
- **Network Usage**: Efficient location caching and update intervals
- **Background Processing**: Optimized for Android's background execution limits

## 🔒 Privacy & Security

- Request permissions only when needed
- Respect user privacy settings
- Handle location data securely
- Comply with location privacy best practices

## 📖 Documentation

For detailed implementation insights, check out the accompanying [Medium article](medium_article.md) that explains:

- Evolution from legacy LocationManager to modern approaches
- Clean Architecture implementation details
- Advanced location tracking patterns
- Testing strategies for location services
- Production deployment considerations

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋‍♂️ Author

**Chamodi Lakmal**
- GitHub: [@chamodlakmal](https://github.com/chamodlakmal)
- Medium: [Building Modern Android Location Manager](https://medium.com/@chamodlakmal97/building-a-modern-android-location-manager-from-legacy-approaches-to-clean-architecture-excellence-3e3e4590533e)

## 🎯 Roadmap

- [ ] Add geofencing capabilities
- [ ] Implement location history tracking
- [ ] Add offline location caching
- [ ] Create location analytics dashboard
- [ ] Add support for custom location providers

## 💡 Acknowledgments

- Google Play Services Location API team
- Android Architecture Components team  
- Jetpack Compose community
- Clean Architecture principles by Uncle Bob

---

⭐ **Star this repository if it helped you!**
