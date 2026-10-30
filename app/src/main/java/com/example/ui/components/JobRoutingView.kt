package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Job
import com.example.model.NairobiLocations
import com.example.ui.theme.LocalGlassTokens
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

enum class TransitMode(val label: String, val icon: String) {
    BODA_BODA("Boda Boda", "🛵"),
    DRIVING("Driving", "🚗"),
    WALKING("Walking", "🚶")
}

/**
 * Interactive Google Map component specifically tailored for Job Routing and Commute Directions.
 * Displays route polyline from worker's current location to job site, commute modes,
 * step-by-step turn directions, and direct turn-by-turn navigation triggers.
 */
@Composable
fun JobRoutingDialog(
    job: Job,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(TransitMode.BODA_BODA) }
    var isSatellite by remember { mutableStateOf(false) }

    val origin = remember {
        LatLng(NairobiLocations.CURRENT_USER_LOC.latitude, NairobiLocations.CURRENT_USER_LOC.longitude)
    }
    val destination = remember(job) {
        LatLng(job.latitude, job.longitude)
    }

    val route = remember(job) {
        NairobiLocations.calculateRoute(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude,
            job.locationName
        )
    }

    // Midpoint for intermediate waypoint bend to mimic realistic Nairobi road curve
    val midpoint = remember(origin, destination) {
        LatLng(
            (origin.latitude + destination.latitude) / 2 + 0.003,
            (origin.longitude + destination.longitude) / 2 - 0.002
        )
    }

    val polylinePoints = remember(origin, destination) {
        listOf(origin, midpoint, destination)
    }

    val initialCenter = remember(origin, destination) {
        LatLng(
            (origin.latitude + destination.latitude) / 2,
            (origin.longitude + destination.longitude) / 2
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 13f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(tokens.surfaceGlass)
                .border(1.dp, tokens.borderGlass, RoundedCornerShape(24.dp))
                .testTag("job_routing_dialog")
        ) {
            // Interactive Route Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = if (isSatellite) MapType.HYBRID else MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // Origin (Worker's location in Westlands)
                Marker(
                    state = rememberMarkerState(position = origin),
                    title = "Your Location",
                    snippet = "Westlands, Nairobi"
                )

                // Destination (Job site)
                Marker(
                    state = rememberMarkerState(position = destination),
                    title = "${job.category.iconName} ${job.title}",
                    snippet = "📍 ${job.locationName} · KSh ${job.payKsh}"
                )

                // Route Polyline
                Polyline(
                    points = polylinePoints,
                    color = Color(0xFF00E676),
                    width = 12f
                )
            }

            // Top Bar: Navigation Header & Style Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                        .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🗺️", fontSize = 14.sp)
                        Text(
                            text = "Route to ${job.locationName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = tokens.textPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Fit Route Bounds Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val bounds = LatLngBounds.builder()
                                    .include(origin)
                                    .include(destination)
                                    .build()
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngBounds(bounds, 120),
                                    durationMs = 600
                                )
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                            .border(0.8.dp, tokens.borderGlass, CircleShape)
                            .testTag("fit_route_bounds_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOutMap,
                            contentDescription = "Fit Route",
                            tint = tokens.mintAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Satellite toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.surfaceElevated.copy(alpha = 0.94f))
                            .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                            .clickable { isSatellite = !isSatellite }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSatellite) "🛰️ Sat" else "🗺️ Map",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tokens.surfaceElevated.copy(alpha = 0.94f))
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
            }

            // Bottom Route Metrics Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .testTag("job_routing_sheet_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                // Route Summary Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${route.distanceKm} km to Destination",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "From Westlands → ${job.locationName}",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.mintContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "KSh ${job.payKsh}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = tokens.mintAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transit Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransitModeTab(
                        mode = TransitMode.BODA_BODA,
                        time = "${route.bodaBodaMinutes} mins",
                        isSelected = selectedMode == TransitMode.BODA_BODA,
                        onClick = { selectedMode = TransitMode.BODA_BODA },
                        modifier = Modifier.weight(1f)
                    )
                    TransitModeTab(
                        mode = TransitMode.DRIVING,
                        time = "${route.drivingMinutes} mins",
                        isSelected = selectedMode == TransitMode.DRIVING,
                        onClick = { selectedMode = TransitMode.DRIVING },
                        modifier = Modifier.weight(1f)
                    )
                    TransitModeTab(
                        mode = TransitMode.WALKING,
                        time = "${route.walkingMinutes} mins",
                        isSelected = selectedMode == TransitMode.WALKING,
                        onClick = { selectedMode = TransitMode.WALKING },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Step-by-step turns list preview (scrollable max 2 items or compact)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceElevated)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    route.turnDirections.take(3).forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.mintAccent
                            )
                            Text(
                                text = step,
                                fontSize = 11.sp,
                                color = tokens.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Action Triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Open in Google Maps
                    Button(
                        onClick = { launchExternalMapsApp(context, job) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.surfaceElevated,
                            contentColor = tokens.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderGlass),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("launch_external_maps_button")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = tokens.mintAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Start GPS Navigation
                    Button(
                        onClick = { launchLiveNavigation(context, job, selectedMode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.mintAccent,
                            contentColor = if (tokens.isDark) Color(0xFF002214) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("launch_live_navigation_button")
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitModeTab(
    mode: TransitMode,
    time: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) tokens.mintContainer else tokens.surfaceElevated)
            .border(
                if (isSelected) 1.5.dp else 0.6.dp,
                if (isSelected) tokens.mintAccent else tokens.borderGlass,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${mode.icon} $time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) tokens.mintAccent else tokens.textPrimary)
            Text(mode.label, fontSize = 10.sp, color = tokens.textMuted)
        }
    }
}

private fun launchExternalMapsApp(context: Context, job: Job) {
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

private fun launchLiveNavigation(context: Context, job: Job, mode: TransitMode) {
    val modeParam = when (mode) {
        TransitMode.BODA_BODA, TransitMode.DRIVING -> "d"
        TransitMode.WALKING -> "w"
    }
    try {
        val navUri = Uri.parse("google.navigation:q=${job.latitude},${job.longitude}&mode=$modeParam")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (navIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(navIntent)
        } else {
            val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}&travelmode=${if (mode == TransitMode.WALKING) "walking" else "driving"}")
            context.startActivity(Intent(Intent.ACTION_VIEW, dirUri))
        }
    } catch (e: Exception) {
        val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}")
        context.startActivity(Intent(Intent.ACTION_VIEW, dirUri))
    }
}
