package lk.chamiviews.locationmanager.domain.model


data class LocationRequestParams(
    val priority: LocationPriority = LocationPriority.BALANCED_POWER_ACCURACY,
    val intervalMillis: Long = 10000L, // 10 seconds
    val minUpdateIntervalMillis: Long = 5000L, // 5 seconds
    val maxUpdateDelayMillis: Long = 30000L, // 30 seconds
    val waitForAccurateLocation: Boolean = false
)
