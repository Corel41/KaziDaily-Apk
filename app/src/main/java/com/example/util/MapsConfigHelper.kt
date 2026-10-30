package com.example.util

import com.example.BuildConfig

/**
 * Utility to check whether a production Google Maps API key has been injected.
 * If no key or a placeholder key is present, the app gracefully falls back to its
 * built-in interactive vector map of Nairobi, ensuring 100% functionality with zero setup.
 */
object MapsConfigHelper {
    val isMapsApiKeyConfigured: Boolean
        get() {
            return try {
                val key = BuildConfig.MAPS_API_KEY
                key.isNotBlank() &&
                    !key.contains("YOUR_GOOGLE_MAPS_API_KEY") &&
                    !key.equals("null", ignoreCase = true) &&
                    key.length > 15
            } catch (e: Exception) {
                false
            }
        }
}
