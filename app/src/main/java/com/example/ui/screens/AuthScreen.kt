package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.GoogleAuthManager
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassTokens

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Automatic silent sign-in attempt on initial composition
    LaunchedEffect(Unit) {
        GoogleAuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* user will click button */ },
            scope = coroutineScope
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (tokens.isDark) {
                        listOf(Color(0xFF030712), Color(0xFF0A1322), Color(0xFF020617))
                    } else {
                        listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                    }
                )
            )
            .padding(horizontal = 24.dp)
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo & Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(tokens.mintContainer)
                    .border(1.5.dp, tokens.mintAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🇰🇪", fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "KaziDaily",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tokens.textPrimary,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Nairobi Daily Casual & Trade Gigs",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.mintAccent
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Value Proposition Features Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AuthFeatureRow(
                        icon = "🔒",
                        title = "Guaranteed Escrow Protection",
                        subtitle = "Funds locked securely until employer confirms completion."
                    )
                    AuthFeatureRow(
                        icon = "🗺️",
                        title = "Live Nairobi Map Routing",
                        subtitle = "Boda Boda, Driving & Walking commute times across estates."
                    )
                    AuthFeatureRow(
                        icon = "💬",
                        title = "Real-Time Cloud Messaging",
                        subtitle = "Direct job messaging synced across all devices."
                    )
                    AuthFeatureRow(
                        icon = "⚡",
                        title = "Fast M-Pesa Withdrawals",
                        subtitle = "Instant cash-out to your Safaricom mobile wallet."
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message Banner
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.amberAccent.copy(alpha = 0.15f))
                            .border(1.dp, tokens.amberAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = error,
                            fontSize = 12.sp,
                            color = tokens.amberAccent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Google Sign-In Button (48dp min interactive height)
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    GoogleAuthManager.onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { err ->
                            isLoading = false
                            errorMessage = err
                        },
                        scope = coroutineScope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.surfaceElevated,
                    contentColor = tokens.textPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderGlass),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = tokens.mintAccent,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Google 'G' Icon indicator
                        Text("🌐", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign in with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secure Google Authentication · Cloud Sync Powered by Firestore",
                fontSize = 11.sp,
                color = tokens.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthFeatureRow(icon: String, title: String, subtitle: String) {
    val tokens = LocalGlassTokens.current
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = tokens.textMuted,
                lineHeight = 15.sp
            )
        }
    }
}
