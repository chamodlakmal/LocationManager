package lk.chamiviews.locationmanager.domain.model
import com.google.android.gms.location.Priority


enum class LocationPriority(val value: Int) {
    HIGH_ACCURACY(Priority.PRIORITY_HIGH_ACCURACY),
    BALANCED_POWER_ACCURACY(Priority.PRIORITY_BALANCED_POWER_ACCURACY),
    LOW_POWER(Priority.PRIORITY_LOW_POWER),
    PASSIVE(Priority.PRIORITY_PASSIVE)
}
