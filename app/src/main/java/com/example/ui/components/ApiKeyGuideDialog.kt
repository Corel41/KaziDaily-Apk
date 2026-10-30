package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalGlassTokens

/**
 * Educational dialog explaining how KaziDaily works out of the box with ZERO API keys
 * via the built-in interactive vector map, and offering simple step-by-step guidance
 * on how to acquire a 100% free Google Maps API key with Google's $200 monthly credit.
 */
@Composable
fun ApiKeyGuideDialog(
    isKeyConfigured: Boolean,
    onDismiss: () -> Unit,
    onUseBuiltInMap: () -> Unit
) {
    val tokens = LocalGlassTokens.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(tokens.surfaceElevated)
                .border(1.dp, tokens.borderGlass, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("api_key_guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(tokens.mintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = tokens.mintAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Maps & Key Guide",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = if (isKeyConfigured) "Google Maps SDK Connected" else "Zero-Key Mode Available",
                                fontSize = 11.sp,
                                color = if (isKeyConfigured) tokens.mintAccent else tokens.amberAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = tokens.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Highlight Card: No Key Needed!
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✨", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "No API Key? You're All Set!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "KaziDaily comes with a Built-in Interactive Vector Map of Nairobi that works 100% free with zero configuration. You can view gigs, calculate routes, check Boda Boda/Driving commute times, and start GPS turn-by-turn navigation in Google Maps on your phone without needing any API key.",
                                fontSize = 11.sp,
                                color = tokens.textSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // How to get a free Google Maps API key (Optional)
                Text(
                    text = "Want Live Google Satellite & Street Tiles? (Optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                StepGuideItem(
                    stepNum = "1",
                    title = "Visit Google Cloud Console",
                    desc = "Go to console.cloud.google.com and sign in with your Google account."
                )
                StepGuideItem(
                    stepNum = "2",
                    title = "Enable Maps SDK for Android",
                    desc = "In 'APIs & Services' > 'Library', search for 'Maps SDK for Android' and click Enable."
                )
                StepGuideItem(
                    stepNum = "3",
                    title = "Generate API Key",
                    desc = "Go to 'Credentials' > 'Create Credentials' > 'API key'."
                )
                StepGuideItem(
                    stepNum = "4",
                    title = "Paste in AI Studio Secrets",
                    desc = "Copy your key and paste it as MAPS_API_KEY in the Secrets panel in AI Studio. (Google gives $200 free credit monthly!)."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderGlass),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.textPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onUseBuiltInMap,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.mintAccent,
                            contentColor = if (tokens.isDark) Color(0xFF002214) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("use_builtin_map_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Built-in Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepGuideItem(stepNum: String, title: String, desc: String) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(tokens.surfaceElevated)
                .border(0.8.dp, tokens.borderGlass, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.mintAccent
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = tokens.textMuted,
                lineHeight = 14.sp
            )
        }
    }
}
