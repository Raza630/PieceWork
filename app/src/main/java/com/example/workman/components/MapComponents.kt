package com.example.workman.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.google.android.gms.location.LocationServices
import com.mappls.sdk.maps.MapView
import com.mappls.sdk.maps.MapplsMap
import com.mappls.sdk.maps.OnMapReadyCallback
import com.mappls.sdk.maps.annotations.MarkerOptions
import com.mappls.sdk.maps.annotations.PolylineOptions
import com.mappls.sdk.maps.camera.CameraUpdateFactory
import com.mappls.sdk.maps.geometry.LatLng
import com.mappls.sdk.maps.geometry.LatLngBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Creates a [MapView] whose lifecycle is bound to the current Compose lifecycle owner.
 * The Mappls (Mapbox-based) MapView requires the standard onCreate/onStart/.../onDestroy
 * calls to be forwarded, which we do here via a [LifecycleEventObserver].
 */
@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { id = View.generateViewId() }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    return mapView
}

/**
 * A composable that shows a Mappls map where the user can pick a location by moving
 * the map under a fixed center pin.
 */
@Composable
fun MapLocationPicker(
    modifier: Modifier = Modifier,
    initialLatitude: Double = 20.5937,  // Default: India center
    initialLongitude: Double = 78.9629,
    initialZoom: Double = 5.0,
    recenterTo: Pair<Double, Double>? = null,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onMyLocationClick: (() -> Unit)? = null
) {
    val mapView = rememberMapViewWithLifecycle()
    var mapplsMap by remember { mutableStateOf<MapplsMap?>(null) }

    // Initialize the map once it's ready.
    LaunchedEffect(mapView) {
        mapView.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(map: MapplsMap) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(initialLatitude, initialLongitude),
                        initialZoom
                    )
                )

                // Report the map center whenever the user stops moving the map.
                map.addOnCameraIdleListener {
                    val target = map.cameraPosition.target
                    if (target != null) {
                        onLocationSelected(target.latitude, target.longitude)
                    }
                }
                mapplsMap = map
            }

            override fun onMapError(errorCode: Int, message: String?) = Unit
        })
    }

    // Programmatically recenter + zoom whenever an external target changes
    // (e.g. boss's GPS location resolves, or "My Location" is tapped).
    LaunchedEffect(recenterTo, mapplsMap) {
        val map = mapplsMap ?: return@LaunchedEffect
        recenterTo?.let { (lat, lng) ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16.0)
            )
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        // Fixed center pin — the map moves under it while the pin stays put.
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Selected location",
            tint = PrimaryBlue,
            modifier = Modifier
                .align(Alignment.Center)
                // Offset up by half the icon height so the tip points at the center.
                .offset(y = (-18).dp)
                .size(40.dp)
        )

        // My Location FAB
        if (onMyLocationClick != null) {
            FloatingActionButton(
                onClick = onMyLocationClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp),
                containerColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = "My Location",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * A composable that shows a Mappls map with a pin at a fixed location (read-only).
 *
 * When [showDirections] is true it will:
 *  1. Fetch the viewer's current location (if permission granted),
 *  2. Draw the driving route (blue line) to the job using the free OSRM service,
 *  3. Show a "Navigate" button that opens Google Maps turn-by-turn directions.
 */
@Composable
fun MapLocationView(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    locationName: String = "",
    zoom: Double = 15.0,
    showDirections: Boolean = false
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var mapplsMap by remember { mutableStateOf<MapplsMap?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var originPoint by remember { mutableStateOf<LatLng?>(null) }

    // Initialize the map + drop the job pin once it's ready.
    LaunchedEffect(mapView) {
        mapView.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(map: MapplsMap) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom)
                )
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(latitude, longitude))
                        .title(locationName.ifBlank { "Job location" })
                )
                // Read-only: disable rotation/tilt gestures for a cleaner card.
                map.uiSettings?.setRotateGesturesEnabled(false)
                map.uiSettings?.setTiltGesturesEnabled(false)
                mapplsMap = map
            }

            override fun onMapError(errorCode: Int, message: String?) = Unit
        })
    }

    // Fetch the viewer's location + driving route to the job (free OSRM API).
    LaunchedEffect(showDirections, latitude, longitude) {
        if (!showDirections) return@LaunchedEffect
        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return@LaunchedEffect

        val origin = getLastKnownLocation(context) ?: return@LaunchedEffect
        originPoint = LatLng(origin.first, origin.second)
        routePoints = fetchDrivingRoute(origin.first, origin.second, latitude, longitude)
    }

    // Draw the route line + origin marker and fit the map to show the whole trip.
    LaunchedEffect(routePoints, mapplsMap) {
        val map = mapplsMap ?: return@LaunchedEffect
        if (routePoints.isEmpty()) return@LaunchedEffect

        map.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(PrimaryBlue.toArgb())
                .width(4f)
        )

        originPoint?.let { op ->
            map.addMarker(MarkerOptions().position(op).title("You"))
        }

        // Fit the map to include the whole route with a little padding.
        try {
            val boundsBuilder = LatLngBounds.Builder()
            routePoints.forEach { boundsBuilder.include(it) }
            boundsBuilder.include(LatLng(latitude, longitude))
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80)
            )
        } catch (_: Exception) {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom)
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                factory = { mapView }
            )

            // Location label below the map
            if (locationName.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // "Navigate" button → opens Google Maps turn-by-turn directions (free, no API key).
            if (showDirections) {
                Button(
                    onClick = {
                        launchTurnByTurnNavigation(
                            context,
                            latitude,
                            longitude,
                            locationName
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Navigate to location",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Routing / Navigation helpers ────────────────────────────────────────────────

/**
 * Returns the device's last known location as (lat, lng), or null if unavailable.
 * Caller must ensure location permission is granted.
 */
@SuppressLint("MissingPermission")
private suspend fun getLastKnownLocation(context: Context): Pair<Double, Double>? {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val loc = client.lastLocation.await()
        if (loc != null) loc.latitude to loc.longitude else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Fetches a driving route polyline from the free OSRM demo server (no API key).
 * Returns an ordered list of Mappls [LatLng], or an empty list on failure.
 */
private suspend fun fetchDrivingRoute(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
): List<LatLng> = withContext(Dispatchers.IO) {
    try {
        // OSRM expects lng,lat order.
        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PieceWork-Android/1.0")
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext emptyList()
            val routes = JSONObject(body).optJSONArray("routes") ?: return@withContext emptyList()
            if (routes.length() == 0) return@withContext emptyList()
            val coords = routes.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")
            val points = ArrayList<LatLng>(coords.length())
            for (i in 0 until coords.length()) {
                val c = coords.getJSONArray(i)
                // GeoJSON is [lng, lat]
                points.add(LatLng(c.getDouble(1), c.getDouble(0)))
            }
            points
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Launches turn-by-turn navigation in Google Maps (free, uses the installed app).
 * Falls back to the Google Maps website in a browser if the app isn't installed.
 */
private fun launchTurnByTurnNavigation(
    context: Context,
    lat: Double,
    lng: Double,
    label: String
) {
    try {
        val navIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$lat,$lng")
        ).apply { setPackage("com.google.android.apps.maps") }
        context.startActivity(navIntent)
    } catch (e: Exception) {
        // Fallback 1: generic geo intent (any installed maps app)
        try {
            val geoUri =
                Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label.ifBlank { "Job location" })})")
            context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        } catch (e2: Exception) {
            // Fallback 2: open Google Maps directions in the browser
            val webUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }
}


