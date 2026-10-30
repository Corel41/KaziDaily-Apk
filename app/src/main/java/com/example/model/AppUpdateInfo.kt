package com.example.model

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseDate: String,
    val releaseNotes: List<String>,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long = 18_400_000L,
    val isMandatory: Boolean = false
) {
    fun isUpdateAvailable(currentVersionCode: Int): Boolean {
        return latestVersionCode > currentVersionCode
    }

    val formattedSize: String
        get() = "${Math.round((apkSizeBytes / (1024.0 * 1024.0)) * 10.0) / 10.0} MB"
}

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data object Checking : UpdateDownloadState
    data class Available(val updateInfo: AppUpdateInfo) : UpdateDownloadState
    data class UpToDate(val currentVersionName: String) : UpdateDownloadState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState
    data class ReadyToInstall(val apkFilePath: String, val updateInfo: AppUpdateInfo) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}
