package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.Job

/**
 * Utility functions for launching external Google Maps apps and GPS turn-by-turn navigation.
 * These intents invoke the native Android Google Maps client and require zero API keys.
 */
object MapsNavigationUtils {
    fun launchGoogleMapsApp(context: Context, job: Job) {
        try {
            val geoUri = Uri.parse("geo:${job.latitude},${job.longitude}?q=${job.latitude},${job.longitude}(${Uri.encode(job.title)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${job.latitude},${job.longitude}")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${job.latitude},${job.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    fun launchGoogleMapsNavigation(context: Context, job: Job) {
        try {
            val navUri = Uri.parse("google.navigation:q=${job.latitude},${job.longitude}&mode=d")
            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
            } else {
                val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}&travelmode=driving")
                context.startActivity(Intent(Intent.ACTION_VIEW, dirUri))
            }
        } catch (e: Exception) {
            val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}&travelmode=driving")
            context.startActivity(Intent(Intent.ACTION_VIEW, dirUri))
        }
    }
}
