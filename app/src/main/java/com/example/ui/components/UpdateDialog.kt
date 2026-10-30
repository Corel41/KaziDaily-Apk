package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    Dialog(onDismissRequest = { viewModel.dismissUpdateDialog() }) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("in_app_update_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            when (updateState) {
                is UpdateDownloadState.Checking -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = tokens.mintAccent, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Checking for KaziDaily updates...",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Connecting to release server",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                is UpdateDownloadState.UpToDate -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = tokens.mintAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You're Up to Date!",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "KaziDaily v${updateState.currentVersionName} is the latest version.",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        GlassButton(
                            text = "Done",
                            onClick = { viewModel.dismissUpdateDialog() },
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is UpdateDownloadState.Available -> {
                    val info = updateState.updateInfo
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = tokens.mintAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "New Version Available",
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version ${info.latestVersionName} · ${info.formattedSize} · ${info.releaseDate}",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "What's New:",
                            color = tokens.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            info.releaseNotes.forEach { note ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("• ", color = tokens.mintAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(note, color = tokens.textSecondary, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                text = "Later",
                                onClick = { viewModel.dismissUpdateDialog() },
                                isPrimary = false,
                                modifier = Modifier.weight(1f)
                            )
                            GlassButton(
                                text = "Download & Install",
                                onClick = { viewModel.startDownloadUpdate(context, info) },
                                isPrimary = true,
                                modifier = Modifier.weight(1.8f),
                                testTag = "start_download_update_button"
                            )
                        }
                    }
                }

                is UpdateDownloadState.Downloading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Downloading Update...",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${updateState.progressPercent}% completed",
                            color = tokens.mintAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = tokens.mintAccent,
                            trackColor = tokens.surfaceElevated
                        )
                    }
                }

                is UpdateDownloadState.ReadyToInstall -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = tokens.mintAccent,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Download Complete!",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "KaziDaily v${updateState.updateInfo.latestVersionName} is ready to install.",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassButton(
                            text = "Install Update Now",
                            onClick = { viewModel.installDownloadedApk(context, updateState.apkFilePath) },
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "install_update_now_button"
                        )
                    }
                }

                is UpdateDownloadState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Update Check Notice",
                            color = tokens.coralAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateState.message,
                            color = tokens.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassButton(
                            text = "Close",
                            onClick = { viewModel.dismissUpdateDialog() },
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {}
            }
        }
    }
}
