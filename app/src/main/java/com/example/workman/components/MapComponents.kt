package com.example.workman.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

/**
 * A stable, identifiable User-Agent. OpenStreetMap tile servers BLOCK requests
 * whose User-Agent is a generic/sample package name (e.g. "com.example.*"),
 * which is what caused the "access blocked" tiles. A unique app name fixes it.
 */
private const val OSM_USER_AGENT = "WorkMan-Android/1.0"

/**
 * A composable that shows an OpenStreetMap view where the user can pick a location
 * by tapping or dragging the map. Shows a center pin.
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
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Configure osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        // Unique UA — prevents OpenStreetMap "access blocked" tiles.
        Configuration.getInstance().userAgentValue = OSM_USER_AGENT
    }

    // Programmatically recenter + zoom whenever an external target changes
    // (e.g. boss's GPS location resolves, or "My Location" is tapped).
    LaunchedEffect(recenterTo) {
        recenterTo?.let { (lat, lng) ->
            mapViewRef?.let { mv ->
                mv.controller.setZoom(16.0)
                mv.controller.animateTo(GeoPoint(lat, lng))
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(initialZoom)
                    controller.setCenter(GeoPoint(initialLatitude, initialLongitude))

                    // Center pin overlay (follows the map as the user drags)
                    overlays.add(CenterPinOverlay(ctx, PrimaryBlue.toArgb()))

                    // Move listener — attached ONCE, reports only on real center changes
                    overlays.add(MapMoveListener { lat, lng -> onLocationSelected(lat, lng) })

                    addOnFirstLayoutListener { _, _, _, _, _ ->
                        val center = mapCenter
                        onLocationSelected(center.latitude, center.longitude)
                    }

                    mapViewRef = this
                }
            }
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
 * A composable that shows a map with a pin at a fixed location (read-only).
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
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var originPoint by remember { mutableStateOf<GeoPoint?>(null) }

    // Configure osmdroid — unique UA prevents OpenStreetMap "access blocked" tiles.
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = OSM_USER_AGENT
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
        originPoint = GeoPoint(origin.first, origin.second)
        routePoints = fetchDrivingRoute(origin.first, origin.second, latitude, longitude)
    }

    // Draw the route line + origin marker and fit the map to show the whole trip.
    LaunchedEffect(routePoints, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (routePoints.isEmpty()) return@LaunchedEffect

        // Remove any previously-added route line before re-adding.
        mv.overlays.removeAll { it is Polyline }
        val line = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = PrimaryBlue.toArgb()
            outlinePaint.strokeWidth = 12f
            outlinePaint.isAntiAlias = true
        }
        mv.overlays.add(0, line)

        originPoint?.let { op ->
            mv.overlays.add(
                FixedPinOverlay(
                    context,
                    android.graphics.Color.parseColor("#2ECC71"),
                    op
                )
            )
        }

        // Fit the map to include the whole route with a little padding.
        val bounds = BoundingBox.fromGeoPoints(routePoints + GeoPoint(latitude, longitude))
        try {
            mv.zoomToBoundingBox(bounds.increaseByScale(1.4f), true, 80)
        } catch (_: Exception) {
            mv.controller.setCenter(GeoPoint(latitude, longitude))
        }
        mv.invalidate()
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
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(zoom)
                        controller.setCenter(GeoPoint(latitude, longitude))

                        // Disable interaction for view-only
                        setBuiltInZoomControls(false)

                        // Add pin overlay at the fixed (job) location
                        val pinOverlay = FixedPinOverlay(
                            ctx,
                            PrimaryBlue.toArgb(),
                            GeoPoint(latitude, longitude)
                        )
                        overlays.add(pinOverlay)

                        mapViewRef = this
                    }
                }
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
 * Returns an ordered list of GeoPoints, or an empty list on failure.
 */
private suspend fun fetchDrivingRoute(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
): List<GeoPoint> = withContext(Dispatchers.IO) {
    try {
        // OSRM expects lng,lat order.
        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", OSM_USER_AGENT)
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext emptyList()
            val routes = JSONObject(body).optJSONArray("routes") ?: return@withContext emptyList()
            if (routes.length() == 0) return@withContext emptyList()
            val coords = routes.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")
            val points = ArrayList<GeoPoint>(coords.length())
            for (i in 0 until coords.length()) {
                val c = coords.getJSONArray(i)
                // GeoJSON is [lng, lat]
                points.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
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

// ── Custom Overlays ────────────────────────────────────────────────────────────

/**
 * Draws a pin at the center of the map (follows the map as user drags).
 */
private class CenterPinOverlay(
    private val context: Context,
    private val pinColor: Int
) : Overlay() {

    private val paint = Paint().apply {
        color = pinColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint().apply {
        color = android.graphics.Color.argb(60, 0, 0, 0)
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val centerX = mapView.width / 2f
        val centerY = mapView.height / 2f

        // Shadow ellipse
        canvas.drawOval(
            centerX - 8f, centerY + 20f,
            centerX + 8f, centerY + 26f,
            shadowPaint
        )

        // Pin body (teardrop shape using circle + triangle)
        val pinRadius = 16f
        val pinTip = centerY + 20f
        val pinCenter = centerY - pinRadius

        // Triangle (point)
        val path = android.graphics.Path().apply {
            moveTo(centerX - pinRadius * 0.6f, pinCenter + pinRadius * 0.5f)
            lineTo(centerX, pinTip)
            lineTo(centerX + pinRadius * 0.6f, pinCenter + pinRadius * 0.5f)
            close()
        }
        canvas.drawPath(path, paint)

        // Circle (head)
        canvas.drawCircle(centerX, pinCenter, pinRadius, paint)

        // Inner white dot
        val whitePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, pinCenter, pinRadius * 0.4f, whitePaint)
    }
}

/**
 * Draws a pin at a fixed GeoPoint location (for view-only maps).
 */
private class FixedPinOverlay(
    private val context: Context,
    private val pinColor: Int,
    private val geoPoint: GeoPoint
) : Overlay() {

    private val paint = Paint().apply {
        color = pinColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val point = Point()
        mapView.projection.toPixels(geoPoint, point)
        val cx = point.x.toFloat()
        val cy = point.y.toFloat()

        val pinRadius = 16f
        val pinTip = cy + 20f
        val pinCenter = cy - pinRadius

        // Shadow
        val shadowPaint = Paint().apply {
            color = android.graphics.Color.argb(60, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawOval(cx - 8f, pinTip, cx + 8f, pinTip + 6f, shadowPaint)

        // Triangle
        val path = android.graphics.Path().apply {
            moveTo(cx - pinRadius * 0.6f, pinCenter + pinRadius * 0.5f)
            lineTo(cx, pinTip)
            lineTo(cx + pinRadius * 0.6f, pinCenter + pinRadius * 0.5f)
            close()
        }
        canvas.drawPath(path, paint)

        // Circle
        canvas.drawCircle(cx, pinCenter, pinRadius, paint)

        // White dot
        val whitePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(cx, pinCenter, pinRadius * 0.4f, whitePaint)
    }
}

/**
 * Overlay that reports map center changes (acts as a scroll listener).
 * Only fires when the center actually moves, to avoid per-frame churn.
 */
private class MapMoveListener(
    private val onMove: (lat: Double, lng: Double) -> Unit
) : Overlay() {

    private var lastLat = Double.NaN
    private var lastLng = Double.NaN

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val center = mapView.mapCenter
        val lat = center.latitude
        val lng = center.longitude
        if (lat != lastLat || lng != lastLng) {
            lastLat = lat
            lastLng = lng
            onMove(lat, lng)
        }
    }
}

