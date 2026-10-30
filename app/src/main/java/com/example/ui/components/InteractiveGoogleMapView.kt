package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.Job
import com.example.model.JobStatus
import com.example.model.NairobiLocations
import com.example.ui.theme.LocalGlassTokens
import com.example.util.MapsConfigHelper
import com.example.util.MapsNavigationUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * Interactive Google Map component powered by Google Maps SDK for Android (`maps-compose`).
 * Displays job markers, route polylines, commute metrics, and map style toggles.
 */
@Composable
fun InteractiveGoogleMapView(
    jobs: List<Job>,
    selectedJob: Job?,
    onSelectJob: (Job?) -> Unit,
    onApplyForJob: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Default center on Nairobi (Westlands user location)
    val userLatLng = remember {
        LatLng(NairobiLocations.CURRENT_USER_LOC.latitude, NairobiLocations.CURRENT_USER_LOC.longitude)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLatLng, 13f)
    }

    val isRealKeyPresent = remember { MapsConfigHelper.isMapsApiKeyConfigured }
    var useBuiltInMap by remember { mutableStateOf(!isRealKeyPresent) }
    var showApiKeyGuideDialog by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    // Move camera smoothly when a job is selected in Google Maps mode
    LaunchedEffect(selectedJob) {
        selectedJob?.let { job ->
            val jobLatLng = LatLng(job.latitude, job.longitude)
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(jobLatLng, 14f),
                durationMs = 800
            )
        }
    }

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = true
            )
        )
    }

    val mapProperties by remember(mapType) {
        mutableStateOf(
            MapProperties(
                mapType = mapType,
                isMyLocationEnabled = false
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surfaceGlass)
            .border(1.dp, tokens.borderGlass, RoundedCornerShape(20.dp))
            .testTag("interactive_google_map_view")
    ) {
        if (useBuiltInMap) {
            // Built-in interactive vector map of Nairobi (100% Free · No API key required)
            InteractiveMapCanvas(
                jobs = jobs,
                selectedJob = selectedJob,
                onSelectJob = onSelectJob,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Google Maps SDK Composable
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = {
                    onSelectJob(null)
                }
            ) {
                // Worker / User Location Marker (Westlands)
                Marker(
                    state = rememberMarkerState(position = userLatLng),
                    title = "My Location (Worker)",
                    snippet = "Waiyaki Way, Westlands",
                    onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(userLatLng, 14.5f),
                                durationMs = 600
                            )
                        }
                        false
                    }
                )

                // Render Job Markers across Nairobi
                jobs.forEach { job ->
                    val jobPosition = LatLng(job.latitude, job.longitude)
                    val isSelected = selectedJob?.id == job.id

                    Marker(
                        state = rememberMarkerState(key = job.id, position = jobPosition),
                        title = "${job.category.iconName} ${job.title}",
                        snippet = "KSh ${job.payKsh} · ${job.locationName}",
                        onClick = {
                            onSelectJob(job)
                            false
                        }
                    )
                }

                // Route Polyline when a job is selected
                if (selectedJob != null) {
                    val routePoints = listOf(
                        userLatLng,
                        LatLng(
                            (userLatLng.latitude + selectedJob.latitude) / 2 + 0.003,
                            (userLatLng.longitude + selectedJob.longitude) / 2 - 0.002
                        ),
                        LatLng(selectedJob.latitude, selectedJob.longitude)
                    )

                    Polyline(
                        points = routePoints,
                        color = Color(0xFF00E676),
                        width = 12f
                    )
                }
            }
        }

        // Top Left: Mode Switcher (Built-in No-Key Map vs Google Maps SDK)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tokens.surfaceElevated.copy(alpha = 0.95f))
                .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(14.dp))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Built-in Map Toggle (Zero API key needed)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (useBuiltInMap) tokens.mintContainer else Color.Transparent)
                    .clickable { useBuiltInMap = true }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
                    .testTag("toggle_builtin_map"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🗺️ Built-in Map",
                    fontSize = 11.sp,
                    fontWeight = if (useBuiltInMap) FontWeight.Bold else FontWeight.Medium,
                    color = if (useBuiltInMap) tokens.mintAccent else tokens.textSecondary
                )
            }

            // Google Maps SDK Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!useBuiltInMap) tokens.purpleAccent.copy(alpha = 0.25f) else Color.Transparent)
                    .clickable {
                        useBuiltInMap = false
                        if (!isRealKeyPresent) {
                            showApiKeyGuideDialog = true
                        }
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
                    .testTag("toggle_google_maps_sdk"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛰️ Google Maps",
                    fontSize = 11.sp,
                    fontWeight = if (!useBuiltInMap) FontWeight.Bold else FontWeight.Medium,
                    color = if (!useBuiltInMap) tokens.purpleAccent else tokens.textSecondary
                )
            }
        }

        // Top Right: Controls (Info guide, Satellite toggle, Recenter)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // API Key Guide Button
            IconButton(
                onClick = { showApiKeyGuideDialog = true },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                    .border(0.8.dp, tokens.borderGlass, CircleShape)
                    .testTag("maps_api_key_guide_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Maps Key Guide",
                    tint = tokens.mintAccent,
                    modifier = Modifier.size(17.dp)
                )
            }

            if (!useBuiltInMap) {
                // Satellite / Street Toggle (Google Maps SDK mode)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                        .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                        .clickable {
                            mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("google_map_type_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mapType == MapType.NORMAL) "🛰️" else "🗺️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                }
            }

            // Recenter Button
            IconButton(
                onClick = {
                    if (useBuiltInMap) {
                        onSelectJob(null)
                    } else {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(userLatLng, 14f),
                                durationMs = 600
                            )
                        }
                    }
                },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                    .border(0.8.dp, tokens.borderGlass, CircleShape)
                    .testTag("google_map_recenter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter on user",
                    tint = tokens.mintAccent,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Notice when in Google Maps SDK mode without active key
        if (!useBuiltInMap && !isRealKeyPresent) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surfaceElevated.copy(alpha = 0.96f))
                    .border(0.8.dp, tokens.amberAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { showApiKeyGuideDialog = true }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🔑", fontSize = 12.sp)
                        Text(
                            text = "Live Google tiles require MAPS_API_KEY. Tap for guide or switch back to Built-in Map.",
                            fontSize = 10.sp,
                            color = tokens.textSecondary,
                            lineHeight = 13.sp
                        )
                    }
                    Text(
                        text = "Use Built-in",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.mintAccent,
                        modifier = Modifier.clickable { useBuiltInMap = true }
                    )
                }
            }
        }

        // Bottom Selected Job Route Card
        AnimatedVisibility(
            visible = selectedJob != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp)
        ) {
            selectedJob?.let { job ->
                GoogleMapsRouteCard(
                    job = job,
                    onClose = { onSelectJob(null) },
                    onApply = { onApplyForJob(job) },
                    onOpenInGoogleMaps = { launchGoogleMapsApp(context, job) },
                    onStartNavigation = { launchGoogleMapsNavigation(context, job) }
                )
            }
        }
    }

    if (showApiKeyGuideDialog) {
        ApiKeyGuideDialog(
            isKeyConfigured = isRealKeyPresent,
            onDismiss = { showApiKeyGuideDialog = false },
            onUseBuiltInMap = {
                useBuiltInMap = true
                showApiKeyGuideDialog = false
            }
        )
    }
}

@Composable
private fun GoogleMapsRouteCard(
    job: Job,
    onClose: () -> Unit,
    onApply: () -> Unit,
    onOpenInGoogleMaps: () -> Unit,
    onStartNavigation: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val route = remember(job.id) {
        NairobiLocations.calculateRoute(
            NairobiLocations.CURRENT_USER_LOC.latitude,
            NairobiLocations.CURRENT_USER_LOC.longitude,
            job.latitude,
            job.longitude,
            job.locationName
        )
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_map_route_card"),
        shape = RoundedCornerShape(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = job.category.iconName, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = job.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "📍 ${job.locationName} · ${route.distanceKm} km route",
                        fontSize = 12.sp,
                        color = tokens.textMuted
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = tokens.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Commute Travel Times in Nairobi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapCommuteBadge(label = "Boda Boda", time = "${route.bodaBodaMinutes}m", emoji = "🛵", modifier = Modifier.weight(1f))
            MapCommuteBadge(label = "Driving", time = "${route.drivingMinutes}m", emoji = "🚗", modifier = Modifier.weight(1f))
            MapCommuteBadge(label = "Walking", time = "${route.walkingMinutes}m", emoji = "🚶", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Google Maps Integration Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Open in Google Maps
            Button(
                onClick = onOpenInGoogleMaps,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.surfaceElevated,
                    contentColor = tokens.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderGlass),
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
                    .testTag("open_in_google_maps_sdk_button")
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = tokens.mintAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Start Google Navigation
            Button(
                onClick = onStartNavigation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.mintAccent,
                    contentColor = if (tokens.isDark) Color(0xFF002214) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
                    .testTag("start_google_navigation_sdk_button")
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Action: Apply
        Button(
            onClick = onApply,
            enabled = job.status == JobStatus.OPEN,
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.purpleAccent,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("google_map_apply_button")
        ) {
            Text(
                text = if (job.status == JobStatus.APPLIED) "Applied ✓" else "Apply for Job (KSh ${job.payKsh})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MapCommuteBadge(label: String, time: String, emoji: String, modifier: Modifier = Modifier) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tokens.surfaceElevated)
            .border(0.6.dp, tokens.borderGlass, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$emoji $time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
            Text(label, fontSize = 10.sp, color = tokens.textMuted)
        }
    }
}

private fun launchGoogleMapsApp(context: Context, job: Job) {
    MapsNavigationUtils.launchGoogleMapsApp(context, job)
}

private fun launchGoogleMapsNavigation(context: Context, job: Job) {
    MapsNavigationUtils.launchGoogleMapsNavigation(context, job)
}
