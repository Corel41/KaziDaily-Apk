package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppUpdateInfo
import com.example.model.UpdateDownloadState
import com.example.ui.theme.LocalGlassTokens
import com.example.viewmodel.KaziViewModel

@Composable
fun UpdateDialog(
    viewModel: KaziViewModel,
    updateState: UpdateDownloadState
) {
    if (updateState is UpdateDownloadState.Idle) return
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current

    // Immediate full-screen glass dialog overlay
    Dialog(
        onDismissRequest = { viewModel.dismissUpdateDialog() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("in_app_patch_fullscreen_view"),
            color = tokens.background.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tokens.mintContainer)
                                .border(1.dp, tokens.mintAccent.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = tokens.mintAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KaziDaily Patch Manager",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = "GitHub In-Place Release Channel",
                                fontSize = 11.sp,
                                color = tokens.textMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dismissUpdateDialog() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tokens.surfaceElevated)
                            .border(0.8.dp, tokens.borderGlass, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = tokens.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Body content based on state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (updateState) {
                        is UpdateDownloadState.Checking -> {
                            CheckingStateView()
                        }

                        is UpdateDownloadState.UpToDate -> {
                            UpToDateStateView(
                                versionName = updateState.currentVersionName,
                                onDismiss = { viewModel.dismissUpdateDialog() }
                            )
                        }

                        is UpdateDownloadState.Available -> {
                            PatchAvailableStateView(
                                info = updateState.updateInfo,
                                onDismiss = { viewModel.dismissUpdateDialog() },
                                onStartDownload = { viewModel.startDownloadUpdate(context, updateState.updateInfo) }
                            )
                        }

                        is UpdateDownloadState.Downloading -> {
                            DownloadingStateView(
                                state = updateState
                            )
                        }

                        is UpdateDownloadState.ReadyToInstall -> {
                            ReadyToRestartStateView(
                                state = updateState,
                                onRestartAndApply = {
                                    viewModel.applyPatchAndRestart(context, updateState.apkFilePath)
                                },
                                onDismiss = { viewModel.dismissUpdateDialog() }
                            )
                        }

                        is UpdateDownloadState.Error -> {
                            ErrorStateView(
                                message = updateState.message,
                                onRetry = { viewModel.checkPatchUpdates() },
                                onDismiss = { viewModel.dismissUpdateDialog() }
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckingStateView() {
    val tokens = LocalGlassTokens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator(
            color = tokens.mintAccent,
            strokeWidth = 3.5.dp,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Checking GitHub Releases...",
            color = tokens.textPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Fetching release metadata and delta patches for KaziDaily",
            color = tokens.textMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun UpToDateStateView(
    versionName: String,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(tokens.mintContainer)
                    .border(1.dp, tokens.mintAccent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = tokens.mintAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "All Patches Applied!",
                color = tokens.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "KaziDaily v$versionName is running the latest GitHub release build.",
                color = tokens.textMuted,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassPill(
                text = "✓ Zero Downtime In-Place Channel",
                color = tokens.mintContainer,
                textColor = tokens.mintAccent
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlassButton(
                text = "Done",
                onClick = onDismiss,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PatchAvailableStateView(
    info: AppUpdateInfo,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassPill(
                            text = "⚡ GitHub Release Patch",
                            color = tokens.mintContainer,
                            textColor = tokens.mintAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = info.patchTag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.cyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "New Patch Available",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Published on GitHub · ${info.releaseDate}",
                        fontSize = 11.sp,
                        color = tokens.textMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = info.formattedSize,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.mintAccent
                    )
                    Text(
                        text = "Delta package",
                        fontSize = 10.sp,
                        color = tokens.textMuted
                    )
                }
            }
        }

        // In-Place Data Safety Guarantee Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.purpleAccent.copy(alpha = if (tokens.isDark) 0.16f else 0.10f))
                .border(1.dp, tokens.purpleAccent.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = tokens.purpleAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "In-Place Patch Installation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = tokens.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "This patch updates the app directly without full reinstallation. Your M-Pesa balance, active escrow guarantees, chat threads, and login credentials will be 100% preserved.",
                        fontSize = 11.sp,
                        color = tokens.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Patch Notes
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text = "What's in this Patch:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                info.releaseNotes.forEach { note ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "• ",
                            color = tokens.mintAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = note,
                            fontSize = 12.sp,
                            color = tokens.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                text = "Later",
                onClick = onDismiss,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "⚡ Download Patch",
                onClick = onStartDownload,
                isPrimary = true,
                modifier = Modifier.weight(2f),
                testTag = "start_download_patch_button"
            )
        }
    }
}

@Composable
private fun DownloadingStateView(
    state: UpdateDownloadState.Downloading
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Downloading GitHub Patch...",
                color = tokens.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.progressPercent}%",
                color = tokens.mintAccent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { state.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = tokens.mintAccent,
                trackColor = tokens.surfaceElevated
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Staging incremental delta update without touching user data",
                color = tokens.textMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ReadyToRestartStateView(
    state: UpdateDownloadState.ReadyToInstall,
    onRestartAndApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(tokens.mintContainer)
                    .border(1.5.dp, tokens.mintAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = tokens.mintAccent,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Patch Ready! Quick Restart Required",
                color = tokens.textPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "KaziDaily ${state.updateInfo.latestVersionName} is staged in cache. Restart now to complete the in-place installation without losing any data.",
                color = tokens.textMuted,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            GlassPill(
                text = "🔒 Zero Data Loss · Session Preserved",
                color = tokens.mintContainer,
                textColor = tokens.mintAccent
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassButton(
                text = "🔄 Restart & Apply Patch Now",
                onClick = onRestartAndApply,
                isPrimary = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("restart_apply_patch_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassButton(
                text = "Restart Later",
                onClick = onDismiss,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Patch Check Notice",
                color = tokens.coralAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = tokens.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = "Close",
                    onClick = onDismiss,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Retry",
                    onClick = onRetry,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
