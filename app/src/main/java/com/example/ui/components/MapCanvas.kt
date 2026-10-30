package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Job
import com.example.model.NairobiLocations
import com.example.model.RouteInfo
import com.example.ui.theme.LocalGlassTokens

@Composable
fun InteractiveMapCanvas(
    jobs: List<Job>,
    selectedJob: Job?,
    onSelectJob: (Job?) -> Unit,
    modifier: Modifier = Modifier,
    onMapTapCoordinate: ((Double, Double, String) -> Unit)? = null
) {
    val tokens = LocalGlassTokens.current

    // Pulsing radar animation for user current location
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    // Bounding coordinates for Nairobi region
    // Min Lat: -1.330 (Karen / Langata), Max Lat: -1.210 (Kasarani)
    // Min Lng: 36.690 (Karen), Max Lng: 36.900 (Eastlands)
    val minLat = -1.335
    val maxLat = -1.215
    val minLng = 36.690
    val maxLng = 36.900

    fun latLngToOffset(lat: Double, lng: Double, width: Float, height: Float): Offset {
        val xRatio = ((lng - minLng) / (maxLng - minLng)).coerceIn(0.05, 0.95)
        val yRatio = (1.0 - ((lat - minLat) / (maxLat - minLat))).coerceIn(0.05, 0.95)
        return Offset((xRatio * width).toFloat(), (yRatio * height).toFloat())
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (tokens.isDark) Color(0xFF070A0F) else Color(0xFFE8EEF5))
            .testTag("interactive_map_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(jobs, selectedJob) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        // Check if tapped near any job pin (hit radius 36dp = ~100px)
                        var clickedJob: Job? = null
                        for (job in jobs) {
                            val pos = latLngToOffset(job.latitude, job.longitude, w, h)
                            val dist = (pos - tapOffset).getDistance()
                            if (dist < 85f) {
                                clickedJob = job
                                break
                            }
                        }

                        if (clickedJob != null) {
                            onSelectJob(clickedJob)
                        } else {
                            if (onMapTapCoordinate != null) {
                                val clickedLng = minLng + (tapOffset.x / w) * (maxLng - minLng)
                                val clickedLat = minLat + (1.0 - (tapOffset.y / h)) * (maxLat - minLat)
                                onMapTapCoordinate(clickedLat, clickedLng, "Selected Map Point")
                            } else {
                                onSelectJob(null)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw base road network (Nairobi arteries)
            drawRoadNetwork(tokens.isDark, w, h)

            // 2. Draw user current location
            val userOffset = latLngToOffset(
                NairobiLocations.CURRENT_USER_LOC.latitude,
                NairobiLocations.CURRENT_USER_LOC.longitude,
                w, h
            )

            // Pulsing radar ring
            drawCircle(
                color = tokens.cyanAccent.copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = userOffset
            )
            // Core blue GPS dot
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = userOffset
            )
            drawCircle(
                color = tokens.cyanAccent,
                radius = 6.5f,
                center = userOffset
            )

            // 3. Draw route line if a job is selected
            if (selectedJob != null) {
                val destOffset = latLngToOffset(selectedJob.latitude, selectedJob.longitude, w, h)

                val routePath = Path().apply {
                    moveTo(userOffset.x, userOffset.y)
                    // Create curved route waypoint simulating real urban roads
                    val midX = (userOffset.x + destOffset.x) / 2f + (destOffset.y - userOffset.y) * 0.12f
                    val midY = (userOffset.y + destOffset.y) / 2f - (destOffset.x - userOffset.x) * 0.12f
                    quadraticTo(midX, midY, destOffset.x, destOffset.y)
                }

                // Route halo glow
                drawPath(
                    path = routePath,
                    color = tokens.mintAccent.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 9f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), 0f)
                    )
                )
                // Active route solid line
                drawPath(
                    path = routePath,
                    color = tokens.mintAccent,
                    style = Stroke(width = 4.5f)
                )
            }

            // 4. Draw Job Pins
            for (job in jobs) {
                val pinPos = latLngToOffset(job.latitude, job.longitude, w, h)
                val isSelected = selectedJob?.id == job.id

                val pinColor = when {
                    isSelected -> tokens.mintAccent
                    job.escrowAmountKsh > 0 -> tokens.purpleAccent
                    else -> if (tokens.isDark) Color(0xFF1E293B) else Color.White
                }
                val borderColor = if (isSelected) Color.White else tokens.mintAccent

                // Outer pin circle
                drawCircle(
                    color = borderColor,
                    radius = if (isSelected) 18f else 13f,
                    center = pinPos
                )
                drawCircle(
                    color = pinColor,
                    radius = if (isSelected) 15f else 10f,
                    center = pinPos
                )
            }
        }

        // Map Watermark / Overlay info
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.surfaceGlass.copy(alpha = 0.90f))
                .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🗺️ ", fontSize = 12.sp)
                Text(
                    text = "Google Maps · Nairobi Live Hub",
                    color = tokens.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Reset to My Location button
        IconButton(
            onClick = { onSelectJob(null) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(tokens.surfaceElevated.copy(alpha = 0.90f))
                .border(0.8.dp, tokens.borderGlass, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location",
                tint = tokens.cyanAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun DrawScope.drawRoadNetwork(isDark: Boolean, w: Float, h: Float) {
    val roadColor = if (isDark) Color(0xFF141C28) else Color(0xFFD6DFEC)
    val majorRoadColor = if (isDark) Color(0xFF1D283A) else Color(0xFFC7D3E5)

    // Waiyaki Way to CBD
    drawLine(
        color = majorRoadColor,
        start = Offset(w * 0.15f, h * 0.35f),
        end = Offset(w * 0.65f, h * 0.50f),
        strokeWidth = 6f
    )
    // Uhuru Highway / Mombasa Rd
    drawLine(
        color = majorRoadColor,
        start = Offset(w * 0.55f, h * 0.20f),
        end = Offset(w * 0.70f, h * 0.85f),
        strokeWidth = 7f
    )
    // Thika Superhighway
    drawLine(
        color = majorRoadColor,
        start = Offset(w * 0.62f, h * 0.45f),
        end = Offset(w * 0.92f, h * 0.15f),
        strokeWidth = 6f
    )
    // Ngong Road to Karen
    drawLine(
        color = roadColor,
        start = Offset(w * 0.58f, h * 0.55f),
        end = Offset(w * 0.12f, h * 0.80f),
        strokeWidth = 5f
    )
    // Ring Road Kilimani
    drawLine(
        color = roadColor,
        start = Offset(w * 0.35f, h * 0.40f),
        end = Offset(w * 0.45f, h * 0.65f),
        strokeWidth = 4f
    )
    // Eastleigh Avenue
    drawLine(
        color = roadColor,
        start = Offset(w * 0.65f, h * 0.48f),
        end = Offset(w * 0.82f, h * 0.42f),
        strokeWidth = 4f
    )
}

@Composable
fun RouteDirectionsCard(
    job: Job,
    onClose: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val route = remember(job) {
        NairobiLocations.calculateRoute(
            fromLat = NairobiLocations.CURRENT_USER_LOC.latitude,
            fromLng = NairobiLocations.CURRENT_USER_LOC.longitude,
            toLat = job.latitude,
            toLng = job.longitude,
            destName = job.locationName
        )
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        borderWidth = 1.dp
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(job.category.iconName, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = job.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = tokens.textPrimary
                    )
                }
                Text(
                    text = "${job.locationName} · ${job.durationHours} hrs · ${route.distanceKm} km away",
                    color = tokens.textMuted,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "KSh ${job.payKsh}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = tokens.mintAccent
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Commute Mode Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CommuteTimePill(
                icon = "🛵",
                title = "Boda-Boda",
                time = "${route.bodaBodaMinutes} min",
                modifier = Modifier.weight(1f)
            )
            CommuteTimePill(
                icon = "🚗",
                title = "Drive",
                time = "${route.drivingMinutes} min",
                modifier = Modifier.weight(1f)
            )
            CommuteTimePill(
                icon = "🚶",
                title = "Walk",
                time = "${route.walkingMinutes} min",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Steps
        Text(
            text = "GPS Turn-by-Turn Directions",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = tokens.textSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            route.turnDirections.take(3).forEachIndexed { idx, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${idx + 1}. ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.mintAccent
                    )
                    Text(
                        text = step,
                        fontSize = 11.sp,
                        color = tokens.textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                text = "Close",
                onClick = onClose,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = if (job.status == com.example.model.JobStatus.APPLIED) "Applied ✓" else "Apply for Job",
                onClick = onApply,
                enabled = job.status == com.example.model.JobStatus.OPEN,
                isPrimary = true,
                modifier = Modifier.weight(1.5f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun CommuteTimePill(
    icon: String,
    title: String,
    time: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.surfaceElevated)
            .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("$icon $title", fontSize = 11.sp, color = tokens.textMuted)
            Text(time, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
        }
    }
}
