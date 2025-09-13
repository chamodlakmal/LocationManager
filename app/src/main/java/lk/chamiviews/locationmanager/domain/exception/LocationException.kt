package lk.chamiviews.locationmanager.domain.exception

sealed class LocationException(message: String) : Exception(message) {

    /**
     * Exception thrown when location permissions are not granted
     */
    class PermissionDeniedException(message: String) : LocationException(message)
}
