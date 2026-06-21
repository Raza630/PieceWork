package com.example.workman.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
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
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

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
        Configuration.getInstance().userAgentValue = context.packageName
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
 * A composable that shows a static map with a pin at a fixed location (read-only).
 */
@Composable
fun MapLocationView(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    locationName: String = "",
    zoom: Double = 15.0
) {
    val context = LocalContext.current

    // Configure osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
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

                        // Add pin overlay at the fixed location
                        val pinOverlay = FixedPinOverlay(
                            ctx,
                            PrimaryBlue.toArgb(),
                            GeoPoint(latitude, longitude)
                        )
                        overlays.add(pinOverlay)
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

