package lk.chamiviews.locationmanager

import android.system.Os
import android.system.OsConstants
import android.util.Log

class PageSizeCompatibility {
    companion object {
        private const val TAG = "PageSizeCompat"

        fun getSystemPageSize(): Long {
            return try {
                Os.sysconf(OsConstants._SC_PAGESIZE)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get page size, assuming 4KB", e)
                4096L // Default fallback
            }
        }

        fun isUsing16KBPages(): Boolean {
            return getSystemPageSize() == 16384L
        }
    }
}