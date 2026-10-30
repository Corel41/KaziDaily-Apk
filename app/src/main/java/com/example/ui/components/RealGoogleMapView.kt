package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.Job
import com.example.model.JobStatus
import com.example.model.NairobiLocations
import com.example.ui.theme.LocalGlassTokens

@Composable
fun RealGoogleMapView(
    jobs: List<Job>,
    selectedJob: Job?,
    onSelectJob: (Job?) -> Unit,
    onApplyForJob: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isSatelliteView by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surfaceGlass)
            .border(1.dp, tokens.borderGlass, RoundedCornerShape(20.dp))
            .testTag("real_google_map_view")
    ) {
        // Embedded Live Interactive Map Engine
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            updateMapMarkers(this@apply, jobs, selectedJob, isSatelliteView, tokens.isDark)
                        }
                    }

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onJobClicked(jobId: String) {
                            val found = jobs.find { it.id == jobId }
                            if (found != null) {
                                this@apply.post { onSelectJob(found) }
                            }
                        }
                    }, "AndroidBridge")

                    val html = buildMapHtml(jobs, selectedJob, tokens.isDark, isSatelliteView)
                    loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { wv ->
                updateMapMarkers(wv, jobs, selectedJob, isSatelliteView, tokens.isDark)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Map Utility Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Satellite / Street Toggle Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surfaceElevated.copy(alpha = 0.92f))
                    .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                    .clickable {
                        isSatelliteView = !isSatelliteView
                        webViewRef?.let {
                            updateMapMarkers(it, jobs, selectedJob, isSatelliteView, tokens.isDark)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("map_layer_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSatelliteView) "🗺️ Streets" else "🛰️ Satellite",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
            }

            // Recenter My Location (Westlands)
            IconButton(
                onClick = {
                    webViewRef?.evaluateJavascript(
                        "if (window.centerOnUser) window.centerOnUser();",
                        null
                    )
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tokens.surfaceElevated.copy(alpha = 0.92f))
                    .border(0.8.dp, tokens.borderGlass, CircleShape)
                    .testTag("map_recenter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = tokens.mintAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Live Nairobi Region Indicator Pill (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tokens.surfaceElevated.copy(alpha = 0.92f))
                .border(0.8.dp, tokens.mintAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tokens.mintAccent)
                )
                Text(
                    text = "Live Google Map · Nairobi (${jobs.size} pins)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
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
                RealJobMapRouteCard(
                    job = job,
                    onClose = { onSelectJob(null) },
                    onApply = { onApplyForJob(job) },
                    onOpenInGoogleMaps = { launchGoogleMapsApp(context, job) },
                    onStartNavigation = { launchGoogleMapsNavigation(context, job) }
                )
            }
        }
    }
}

@Composable
private fun RealJobMapRouteCard(
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
            .testTag("map_route_card"),
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
                        text = "📍 ${job.locationName} · ${route.distanceKm} km away",
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
            CommuteEstimateBadge(label = "Boda Boda", time = "${route.bodaBodaMinutes}m", emoji = "🛵", modifier = Modifier.weight(1f))
            CommuteEstimateBadge(label = "Driving", time = "${route.drivingMinutes}m", emoji = "🚗", modifier = Modifier.weight(1f))
            CommuteEstimateBadge(label = "Walking", time = "${route.walkingMinutes}m", emoji = "🚶", modifier = Modifier.weight(1f))
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
                    .testTag("open_in_google_maps_button")
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
                    .testTag("start_google_navigation_button")
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Action: Apply / View Details
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
                .testTag("map_apply_button")
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
private fun CommuteEstimateBadge(label: String, time: String, emoji: String, modifier: Modifier = Modifier) {
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
    try {
        // geo:latitude,longitude?q=latitude,longitude(Label)
        val geoUri = Uri.parse("geo:${job.latitude},${job.longitude}?q=${job.latitude},${job.longitude}(${Uri.encode(job.title)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback to web Google Maps
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${job.latitude},${job.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    } catch (e: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${job.latitude},${job.longitude}")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

private fun launchGoogleMapsNavigation(context: Context, job: Job) {
    try {
        val navUri = Uri.parse("google.navigation:q=${job.latitude},${job.longitude}&mode=d")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (navIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(navIntent)
        } else {
            val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}&travelmode=driving")
            context.startActivity(Intent(Intent.ACTION_VIEW, dirUri))
        }
    } catch (e: Exception) {
        val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}&travelmode=driving")
        context.startActivity(Intent(Intent.ACTION_VIEW, dirUri))
    }
}

private fun updateMapMarkers(
    webView: WebView,
    jobs: List<Job>,
    selectedJob: Job?,
    isSatellite: Boolean,
    isDark: Boolean
) {
    val jobsJson = jobs.joinToString(prefix = "[", postfix = "]") { j ->
        """{"id":"${j.id}","title":"${j.title.replace("\"", "\\\"")}","lat":${j.latitude},"lng":${j.longitude},"pay":${j.payKsh},"icon":"${j.category.iconName}","location":"${j.locationName}"}"""
    }
    val selId = selectedJob?.id ?: ""
    val selLat = selectedJob?.latitude ?: 0.0
    val selLng = selectedJob?.longitude ?: 0.0

    val js = "if (window.renderJobs) window.renderJobs($jobsJson, '$selId', $selLat, $selLng, $isSatellite, $isDark);"
    webView.evaluateJavascript(js, null)
}

private fun buildMapHtml(
    jobs: List<Job>,
    selectedJob: Job?,
    isDark: Boolean,
    isSatellite: Boolean
): String {
    val bgColor = if (isDark) "#0a0e14" else "#f0f4f8"
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background: $bgColor;
                }
                .custom-job-pin {
                    background: #00E676;
                    color: #002214;
                    font-weight: 800;
                    font-size: 11px;
                    border-radius: 18px;
                    padding: 4px 8px;
                    border: 2px solid white;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.35);
                    display: inline-flex;
                    align-items: center;
                    white-space: nowrap;
                    cursor: pointer;
                    transform: translate(-50%, -50%);
                }
                .custom-job-pin.selected {
                    background: #9D4EDD;
                    color: white;
                    border-color: #00E676;
                    transform: translate(-50%, -50%) scale(1.15);
                    box-shadow: 0 6px 18px rgba(157,78,221,0.6);
                }
                .user-pulse-pin {
                    width: 18px;
                    height: 18px;
                    background: #00B4D8;
                    border: 3px solid white;
                    border-radius: 50%;
                    box-shadow: 0 0 10px #00B4D8;
                    animation: pulse 1.8s infinite;
                }
                @keyframes pulse {
                    0% { box-shadow: 0 0 0 0 rgba(0,180,216, 0.7); }
                    70% { box-shadow: 0 0 0 16px rgba(0,180,216, 0); }
                    100% { box-shadow: 0 0 0 0 rgba(0,180,216, 0); }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([-1.286389, 36.817223], 13);

                var tileLayer;
                var currentTileType = '$isSatellite';

                function setLayer(isSat, isDarkTheme) {
                    if (tileLayer) map.removeLayer(tileLayer);
                    if (isSat) {
                        tileLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                            maxZoom: 18
                        });
                    } else if (isDarkTheme) {
                        tileLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                            maxZoom: 19
                        });
                    } else {
                        tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19
                        });
                    }
                    tileLayer.addTo(map);
                }
                setLayer($isSatellite, $isDark);

                // Add User Location Marker (Westlands)
                var userMarker = L.marker([-1.2673, 36.8055], {
                    icon: L.divIcon({
                        className: '',
                        html: '<div class="user-pulse-pin" title="My Location (Westlands)"></div>',
                        iconSize: [20, 20]
                    })
                }).addTo(map);

                var markerLayerGroup = L.layerGroup().addTo(map);
                var routeLine = null;

                window.renderJobs = function(jobsList, selectedId, selLat, selLng, isSat, isDark) {
                    setLayer(isSat, isDark);
                    markerLayerGroup.clearLayers();

                    jobsList.forEach(function(job) {
                        var isSel = (job.id === selectedId);
                        var pinClass = isSel ? 'custom-job-pin selected' : 'custom-job-pin';
                        var html = '<div class="' + pinClass + '" onclick="window.AndroidBridge && window.AndroidBridge.onJobClicked(\'' + job.id + '\')">'
                                 + job.icon + ' KSh ' + job.pay + '</div>';

                        var icon = L.divIcon({
                            className: '',
                            html: html,
                            iconSize: [80, 28]
                        });

                        L.marker([job.lat, job.lng], { icon: icon }).addTo(markerLayerGroup);
                    });

                    if (routeLine) {
                        map.removeLayer(routeLine);
                        routeLine = null;
                    }

                    if (selLat && selLng) {
                        routeLine = L.polyline([
                            [-1.2673, 36.8055],
                            [selLat, selLng]
                        ], {
                            color: '#00E676',
                            weight: 4,
                            dashArray: '6, 8',
                            opacity: 0.9
                        }).addTo(map);

                        map.flyTo([selLat, selLng], 14, { duration: 1.0 });
                    }
                };

                window.centerOnUser = function() {
                    map.flyTo([-1.2673, 36.8055], 14, { duration: 1.0 });
                };
            </script>
        </body>
        </html>
    """.trimIndent()
}
