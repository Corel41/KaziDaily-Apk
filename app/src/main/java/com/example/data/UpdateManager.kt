package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UpdateManager {

    /**
     * Checks for the latest available release for KaziDaily.
     */
    suspend fun checkForUpdates(currentVersionCode: Int): AppUpdateInfo? = withContext(Dispatchers.IO) {
        delay(600) // Brief simulation of network fetch
        // Release metadata for KaziDaily
        val latestRelease = AppUpdateInfo(
            latestVersionCode = 2,
            latestVersionName = "1.2.0",
            releaseDate = "Today",
            releaseNotes = listOf(
                "Simulated Escrow hold & release logic with 10% platform fee deduction",
                "New Grok-like tactile mobile theme switcher (Moon & Sun)",
                "Full Material 3 native Android NavigationBar with unread badges",
                "In-App APK Auto-Updater with 1-tap installation",
                "Performance optimizations and memory improvements"
            ),
            apkDownloadUrl = "https://github.com/aistudio-build/kazidaily/releases/download/v1.2.0/kazidaily.apk",
            apkSizeBytes = 19_200_000L,
            isMandatory = false
        )

        if (latestRelease.isUpdateAvailable(currentVersionCode)) {
            latestRelease
        } else {
            null
        }
    }

    /**
     * Simulates downloading the APK with realistic progress callbacks and saves to app cache.
     */
    suspend fun downloadApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val apkDir = File(context.cacheDir, "updates")
        if (!apkDir.exists()) apkDir.mkdirs()

        val apkFile = File(apkDir, "KaziDaily-${updateInfo.latestVersionName}.apk")

        // Simulate progressive chunk download (0% -> 100%)
        for (p in 1..10) {
            delay(150)
            onProgress(p * 10)
        }

        // Write simulated APK signature into cache file if not existing
        if (!apkFile.exists() || apkFile.length() == 0L) {
            FileOutputStream(apkFile).use { out ->
                // Write standard APK zip header bytes (PK..)
                out.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
                out.write("KaziDaily APK Update Package v${updateInfo.latestVersionName}".toByteArray())
            }
        }

        apkFile
    }

    /**
     * Launches Android native PackageInstaller to install the downloaded APK via FileProvider.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
