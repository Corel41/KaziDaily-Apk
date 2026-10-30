package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LocationCoordinate
import com.example.model.NairobiLocations
import com.example.ui.theme.LocalGlassTokens
import com.example.util.MapsConfigHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * Interactive Google Map component for location selection.
 * Enables employers/workers to pick an exact job pin across Nairobi,
 * drag the pin, pan the camera, or select from popular Nairobi estates.
 */
@Composable
fun LocationPickerMap(
    initialLocation: LocationCoordinate,
    onLocationConfirmed: (LocationCoordinate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val coroutineScope = rememberCoroutineScope()

    val isRealKeyPresent = remember { MapsConfigHelper.isMapsApiKeyConfigured }
    var useBuiltInPicker by remember { mutableStateOf(!isRealKeyPresent) }

    var pickedLatLng by remember {
        mutableStateOf(LatLng(initialLocation.latitude, initialLocation.longitude))
    }
    var customAddressName by remember { mutableStateOf(initialLocation.name) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickedLatLng, 15f)
    }

    val markerState = rememberMarkerState(position = pickedLatLng)

    // Update marker when camera moves if tracking center in Google Maps mode
    LaunchedEffect(cameraPositionState.position) {
        if (!useBuiltInPicker) {
            val target = cameraPositionState.position.target
            pickedLatLng = target
            markerState.position = target
        }
    }

    // Nearest known Nairobi estate calculation
    val nearestEstate = remember(pickedLatLng) {
        findNearestEstate(pickedLatLng.latitude, pickedLatLng.longitude)
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
                .testTag("location_picker_dialog")
        ) {
            if (useBuiltInPicker) {
                // Built-in interactive map picker (100% Free · No API key needed)
                InteractiveMapCanvas(
                    jobs = emptyList(),
                    selectedJob = null,
                    onSelectJob = {},
                    onMapTapCoordinate = { lat, lng, _ ->
                        pickedLatLng = LatLng(lat, lng)
                        markerState.position = pickedLatLng
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Interactive Google Map for Location Selection
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = mapType),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = true,
                        myLocationButtonEnabled = false
                    ),
                    onMapClick = { latLng ->
                        pickedLatLng = latLng
                        markerState.position = latLng
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLng(latLng),
                                durationMs = 400
                            )
                        }
                    }
                ) {
                    // Interactive Selected Location Pin
                    Marker(
                        state = markerState,
                        draggable = true,
                        title = "Selected Job Site",
                        snippet = "${nearestEstate.name} (${String.format("%.4f", pickedLatLng.latitude)}, ${String.format("%.4f", pickedLatLng.longitude)})"
                    )
                }
            }

            // Top Header: Title, Mode Toggle, Satellite Toggle, & Dismiss
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle (Built-in No-Key Map vs Google Maps SDK)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceElevated.copy(alpha = 0.95f))
                        .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (useBuiltInPicker) tokens.mintContainer else Color.Transparent)
                            .clickable { useBuiltInPicker = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("picker_toggle_builtin"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗺️ Built-in",
                            fontSize = 11.sp,
                            fontWeight = if (useBuiltInPicker) FontWeight.Bold else FontWeight.Medium,
                            color = if (useBuiltInPicker) tokens.mintAccent else tokens.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!useBuiltInPicker) tokens.purpleAccent.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { useBuiltInPicker = false }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("picker_toggle_google"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛰️ Google Maps",
                            fontSize = 11.sp,
                            fontWeight = if (!useBuiltInPicker) FontWeight.Bold else FontWeight.Medium,
                            color = if (!useBuiltInPicker) tokens.purpleAccent else tokens.textSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!useBuiltInPicker) {
                        // Map Type Toggle (Satellite / Streets)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(tokens.surfaceElevated.copy(alpha = 0.95f))
                                .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                                .clickable {
                                    mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                                }
                                .padding(horizontal = 9.dp, vertical = 7.dp),
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

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(tokens.surfaceElevated.copy(alpha = 0.95f))
                            .border(0.8.dp, tokens.borderGlass, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = tokens.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Bottom Floating Card: Estate Quick-Jump & Confirmation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                // Preset Estates Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NairobiLocations.PRESET_LOCATIONS.forEach { loc ->
                        val isNear = loc.name == nearestEstate.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isNear) tokens.mintContainer else tokens.surfaceElevated.copy(alpha = 0.94f))
                                .border(
                                    0.8.dp,
                                    if (isNear) tokens.mintAccent else tokens.borderGlass,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    val latLng = LatLng(loc.latitude, loc.longitude)
                                    pickedLatLng = latLng
                                    markerState.position = latLng
                                    customAddressName = loc.name
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                                            durationMs = 500
                                        )
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📍 ${loc.name}",
                                fontSize = 11.sp,
                                fontWeight = if (isNear) FontWeight.Bold else FontWeight.Normal,
                                color = if (isNear) tokens.mintAccent else tokens.textPrimary
                            )
                        }
                    }
                }

                // Selection Details Glass Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_picker_confirm_card"),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nearestEstate.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tokens.textPrimary
                            )
                            Text(
                                text = "Lat: ${String.format("%.4f", pickedLatLng.latitude)}, Lng: ${String.format("%.4f", pickedLatLng.longitude)}",
                                fontSize = 11.sp,
                                color = tokens.textMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.mintContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "GPS Pin Ready",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.mintAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customAddressName,
                        onValueChange = { customAddressName = it },
                        label = { Text("Specific Landmark / Building Name", fontSize = 11.sp, color = tokens.textMuted) },
                        placeholder = { Text("e.g. Near Sarit Centre, Gate 2", fontSize = 12.sp, color = tokens.textMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tokens.surfaceElevated,
                            unfocusedContainerColor = tokens.surfaceElevated,
                            focusedBorderColor = tokens.mintAccent,
                            unfocusedBorderColor = tokens.borderGlass,
                            focusedTextColor = tokens.textPrimary,
                            unfocusedTextColor = tokens.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_picker_landmark_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val finalName = if (customAddressName.isNotBlank()) customAddressName.trim() else nearestEstate.name
                            val chosen = LocationCoordinate(
                                name = finalName,
                                description = "${nearestEstate.name}, Nairobi (${String.format("%.4f", pickedLatLng.latitude)}, ${String.format("%.4f", pickedLatLng.longitude)})",
                                latitude = pickedLatLng.latitude,
                                longitude = pickedLatLng.longitude
                            )
                            onLocationConfirmed(chosen)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.mintAccent,
                            contentColor = if (tokens.isDark) Color(0xFF002214) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("confirm_pin_location_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Confirm Location (Pin Selected)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun findNearestEstate(lat: Double, lng: Double): LocationCoordinate {
    var nearest = NairobiLocations.PRESET_LOCATIONS.first()
    var minDistance = Double.MAX_VALUE

    for (loc in NairobiLocations.PRESET_LOCATIONS) {
        val dLat = loc.latitude - lat
        val dLng = loc.longitude - lng
        val distSq = dLat * dLat + dLng * dLng
        if (distSq < minDistance) {
            minDistance = distSq
            nearest = loc
        }
    }
    return nearest
}
