package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.model.Job
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
 * Dedicated interactive map view for the Job Details screen powered by the Google Maps SDK
 * (with built-in zero-key fallback for environments without an active API key).
 * Visualizes the job's precise site location, displays an active route polyline from the worker's
 * current location, provides multi-modal commute times (Boda Boda, Driving, Walking), turn-by-turn
 * navigation instructions, and direct triggers to launch native Google Maps GPS navigation.
 */
@Composable
fun JobLocationRouteMapView(
    job: Job,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isRealKeyPresent = remember { MapsConfigHelper.isMapsApiKeyConfigured }
    var useBuiltInMap by remember { mutableStateOf(!isRealKeyPresent) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var isDirectionsExpanded by remember { mutableStateOf(false) }

    val userLatLng = remember {
        LatLng(NairobiLocations.CURRENT_USER_LOC.latitude, NairobiLocations.CURRENT_USER_LOC.longitude)
    }
    val jobLatLng = remember(job.id) {
        LatLng(job.latitude, job.longitude)
    }
    val midpoint = remember(job.id) {
        LatLng(
            (userLatLng.latitude + jobLatLng.latitude) / 2.0,
            (userLatLng.longitude + jobLatLng.longitude) / 2.0
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(midpoint, 13f)
    }

    val route = remember(job.id) {
        NairobiLocations.calculateRoute(
            NairobiLocations.CURRENT_USER_LOC.latitude,
            NairobiLocations.CURRENT_USER_LOC.longitude,
            job.latitude,
            job.longitude,
            job.locationName
        )
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("job_details_map_component")
    ) {
        // Section Title & Route Overview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🗺️", fontSize = 16.sp)
                Text(
                    text = "Job Site & Route Tracker",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.mintContainer)
                    .border(0.6.dp, tokens.mintAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${route.distanceKm} km away",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.mintAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Map Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.surfaceGlass)
                .border(1.dp, tokens.borderGlass, RoundedCornerShape(18.dp))
                .testTag("job_details_map_frame")
        ) {
            if (useBuiltInMap) {
                // Built-in Interactive Vector Map
                InteractiveMapCanvas(
                    jobs = listOf(job),
                    selectedJob = job,
                    onSelectJob = { /* already focused on this job */ },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Google Maps SDK Composable
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings
                ) {
                    // Marker 1: Worker's Location (Origin)
                    Marker(
                        state = rememberMarkerState(position = userLatLng),
                        title = "Your Location",
                        snippet = "Waiyaki Way, Westlands"
                    )

                    // Marker 2: Job Site Location (Destination)
                    Marker(
                        state = rememberMarkerState(position = jobLatLng),
                        title = "${job.category.iconName} ${job.title}",
                        snippet = "📍 ${job.locationName} · KSh ${job.payKsh}"
                    )

                    // Route Polyline tracking from worker to job site
                    val routePoints = listOf(
                        userLatLng,
                        LatLng(
                            (userLatLng.latitude + jobLatLng.latitude) / 2 + 0.003,
                            (userLatLng.longitude + jobLatLng.longitude) / 2 - 0.002
                        ),
                        jobLatLng
                    )

                    Polyline(
                        points = routePoints,
                        color = Color(0xFF00E676),
                        width = 10f
                    )
                }
            }

            // Top Left: Map Mode Toggle (Built-in vs Google Maps SDK)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                    .border(0.6.dp, tokens.borderGlass, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (useBuiltInMap) tokens.mintContainer else Color.Transparent)
                        .clickable { useBuiltInMap = true }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("details_map_toggle_builtin"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗺️ Vector",
                        fontSize = 10.sp,
                        fontWeight = if (useBuiltInMap) FontWeight.Bold else FontWeight.Medium,
                        color = if (useBuiltInMap) tokens.mintAccent else tokens.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!useBuiltInMap) tokens.purpleAccent.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { useBuiltInMap = false }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("details_map_toggle_google"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛰️ Google SDK",
                        fontSize = 10.sp,
                        fontWeight = if (!useBuiltInMap) FontWeight.Bold else FontWeight.Medium,
                        color = if (!useBuiltInMap) tokens.purpleAccent else tokens.textSecondary
                    )
                }
            }

            // Top Right: Satellite toggle & Recenter button
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!useBuiltInMap) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                            .border(0.6.dp, tokens.borderGlass, RoundedCornerShape(10.dp))
                            .clickable {
                                mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                            }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .testTag("details_map_satellite_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mapType == MapType.NORMAL) "🛰️" else "🗺️",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(midpoint, 13f),
                                durationMs = 500
                            )
                        }
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                        .border(0.6.dp, tokens.borderGlass, CircleShape)
                        .testTag("details_map_recenter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on route",
                        tint = tokens.mintAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Bottom Floating Origin-to-Destination Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                    .border(0.6.dp, tokens.borderGlass, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Westlands", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tokens.mintAccent)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = tokens.textMuted, modifier = Modifier.size(12.dp))
                    Text(job.locationName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Commute Travel Times Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CommuteTimeCard(label = "Boda Boda", time = "${route.bodaBodaMinutes}m", emoji = "🛵", modifier = Modifier.weight(1f))
            CommuteTimeCard(label = "Driving", time = "${route.drivingMinutes}m", emoji = "🚗", modifier = Modifier.weight(1f))
            CommuteTimeCard(label = "Walking", time = "${route.walkingMinutes}m", emoji = "🚶", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Open in Google Maps
            Button(
                onClick = { MapsNavigationUtils.launchGoogleMapsApp(context, job) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.surfaceElevated,
                    contentColor = tokens.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderGlass),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("details_open_google_maps_button")
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp), tint = tokens.mintAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Start GPS Navigation
            Button(
                onClick = { MapsNavigationUtils.launchGoogleMapsNavigation(context, job) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.mintAccent,
                    contentColor = if (tokens.isDark) Color(0xFF002214) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(42.dp)
                    .testTag("details_start_navigation_button")
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Expandable Turn-by-Turn Route Guidance
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.surfaceElevated)
                .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                .clickable { isDirectionsExpanded = !isDirectionsExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("details_directions_accordion")
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🧭", fontSize = 13.sp)
                        Text(
                            text = "Turn-by-Turn Route (${route.turnDirections.size} steps)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.textPrimary
                        )
                    }
                    Icon(
                        imageVector = if (isDirectionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle directions",
                        tint = tokens.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isDirectionsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        route.turnDirections.forEachIndexed { idx, direction ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(tokens.mintContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.mintAccent
                                    )
                                }
                                Text(
                                    text = direction,
                                    fontSize = 11.sp,
                                    color = tokens.textSecondary,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommuteTimeCard(label: String, time: String, emoji: String, modifier: Modifier = Modifier) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.surfaceElevated)
            .border(0.6.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$emoji $time", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, color = tokens.textMuted)
        }
    }
}
